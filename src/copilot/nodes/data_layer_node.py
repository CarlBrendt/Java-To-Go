from __future__ import annotations

import json
import logging
from typing import Dict, Any

from langchain_core.messages import HumanMessage

from src.services.mws_llm_factory import build_mws_chat_llm
from src.settings.config import APISettings
from src.copilot.graph_state import MigrationGraphState
from src.copilot.utils import extract_json_files

logger = logging.getLogger(__name__)


async def node_data_layer(state: MigrationGraphState) -> dict:
    """Stage 3: Data Layer Transformation."""
    structure = state.get("java_structure", {})
    contract = state.get("api_contract", [])
    migration_plan = state.get("migration_plan", "")

    if not contract:
        logger.warning(
            "No API contract available for data layer transformation"
        )
        return {
            "generated_models_code": "",
            "generated_go_code": state.get("generated_go_code", {}),
            "status": "data_layer_error_no_contract",
            "current_node": "data_layer",
        }

    # Collect all DTOs
    dtos_detail = []
    for dto in structure.get("dtos", []):
        if isinstance(dto, dict):
            dtos_detail.append({
                "class_name": dto.get("class_name", ""),
                "fields": [
                    {
                        "name": f.get("name", "unknown"),
                        "type": f.get("type", "unknown"),
                        "annotations": f.get("annotations", []),
                    }
                    for f in dto.get("fields", [])
                ],
                "annotations": dto.get("annotations", []),
            })
        else:
            dtos_detail.append({
                "class_name": str(dto),
                "fields": [],
                "annotations": [],
            })

    # Collect types from API contract
    contract_types = set()
    for endpoint in contract:
        if endpoint.get("is_exception_handler"):
            continue
        for key in ("response_type", "request_type"):
            raw = endpoint.get(key, "")
            if raw:
                contract_types.add(raw.split('.')[-1])

    # Если слишком много DTO — разбиваем на батчи
    MAX_DTOS_PER_REQUEST = 30
    if len(dtos_detail) > MAX_DTOS_PER_REQUEST:
        logger.info(
            f"Too many DTOs ({len(dtos_detail)}), "
            f"splitting into batches of {MAX_DTOS_PER_REQUEST}"
        )

    all_generated_files: Dict[str, str] = {}
    all_models_code = ""

    # Разбиваем на батчи
    batches = [
        dtos_detail[i:i + MAX_DTOS_PER_REQUEST]
        for i in range(0, len(dtos_detail), MAX_DTOS_PER_REQUEST)
    ]

    settings = APISettings()
    llm = build_mws_chat_llm(settings, state.get("mws_model_name"))

    for batch_idx, batch in enumerate(batches):
        batch_name = (
            f"models.go" if len(batches) == 1
            else f"models_{batch_idx + 1}.go"
        )

        prompt_content = (
            "### INSTRUCTION ###\n"
            "Return ONLY a JSON object. No text before or after. "
            "Start with '{' and end with '}'.\n"
            "### INSTRUCTION ###\n\n"

            "You are a Senior Go engineer. Convert these Java DTOs "
            "to Go structs.\n\n"

            f"## Java DTOs (batch {batch_idx + 1}/{len(batches)})\n\n"
            f"```json\n{json.dumps(batch, indent=2, ensure_ascii=False)}\n```\n\n"

            "## Requirements\n\n"
            f"1. Put ALL structs into ONE file named `{batch_name}`.\n"
            "2. Use `package models`.\n"
            "3. Type mappings:\n"
            "   - String → string\n"
            "   - Long/long → int64\n"
            "   - Integer/int → int\n"
            "   - Double/double → float64\n"
            "   - Float/float → float32\n"
            "   - Boolean/boolean → bool\n"
            "   - List<T> → []T\n"
            "   - Map<K,V> → map[K]V\n"
            "   - Optional<T> → *T\n"
            "   - Date/LocalDateTime/Instant → time.Time\n"
            "   - byte[] → []byte\n"
            "   - BigDecimal → float64\n"
            "4. Add JSON struct tags (camelCase by default).\n"
            "   - If @JsonProperty exists, use that name.\n"
            "5. Add `validate` tags for Bean Validation annotations.\n"
            "6. Add necessary imports (time, etc.).\n"
            "7. IMPORTANT: Put ALL structs in ONE file. "
            "Do NOT create separate files per struct.\n\n"

            "### CRITICAL JSON FORMATTING RULE ###\n"
            "Return a VALID JSON string. "
            "DO NOT USE BACKTICKS (`) inside 'content'.\n"
            "All Go code in 'content' MUST be properly escaped:\n"
            "- Use '\\n' for new lines.\n"
            "- Use '\\\"' for double quotes.\n"
            "### END RULE ###\n\n"

            "Output format:\n"
            "{\n"
            f"  \"files\": [{{\"name\": \"{batch_name}\", "
            f"\"content\": \"package models\\n...\"}}]\n"
            "}\n"
        )

        try:
            msg = await llm.ainvoke([HumanMessage(content=prompt_content)])
            content = (
                msg.content if isinstance(msg.content, str)
                else str(msg.content)
            )

            generated_files = extract_json_files(content)

            if generated_files:
                all_generated_files.update(generated_files)
                # Собираем весь код моделей
                for fname, code in generated_files.items():
                    all_models_code += f"\n// === {fname} ===\n{code}\n"

                logger.info(
                    f"Data layer batch {batch_idx + 1}/{len(batches)}: "
                    f"{len(generated_files)} files, "
                    f"{len(batch)} DTOs processed"
                )
            else:
                logger.warning(
                    f"Data layer batch {batch_idx + 1}: "
                    f"LLM did not return valid JSON"
                )
                # Сохраняем raw ответ как fallback
                all_generated_files[batch_name] = content
                all_models_code += f"\n// === {batch_name} (raw) ===\n{content}\n"

        except Exception as e:
            logger.exception(
                f"Error in data layer batch {batch_idx + 1}: {e}"
            )

    if not all_generated_files:
        logger.warning("No files generated for data layer")
        return {
            "generated_models_code": "",
            "generated_go_code": state.get("generated_go_code", {}),
            "status": "data_layer_parse_error",
            "current_node": "data_layer",
        }

    # Merge into generated_go_code
    existing_code = dict(state.get("generated_go_code", {}))
    existing_code.update(all_generated_files)

    logger.info(
        f"Data layer transformation complete: "
        f"{len(all_generated_files)} files, "
        f"{len(all_models_code)} total chars"
    )

    return {
        "generated_models_code": all_models_code,
        "generated_go_code": existing_code,
        "status": "data_layer_complete",
        "current_node": "data_layer",
    }