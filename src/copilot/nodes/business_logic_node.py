from __future__ import annotations

import json
import logging

from langchain_core.messages import HumanMessage

from src.copilot.graph_state import MigrationGraphState
from src.services.mws_llm_factory import build_mws_chat_llm
from src.settings.config import APISettings
from src.copilot.utils import extract_json_files

logger = logging.getLogger(__name__)


async def node_business_logic(state: MigrationGraphState) -> dict:
    """Stage 4: Business Logic Migration.

    Translates Java service and repository layers into Go:
    - Converts Spring service classes to Go structs with methods
    - Replaces Spring DI with constructor injection
    - Converts JPA/Hibernate patterns to GORM/sqlx
    - Marks complex logic with TODO comments for manual review
    """
    structure = state.get("java_structure", {})
    dependency_graph = state.get("dependency_graph", {})
    migration_plan = state.get("migration_plan", "")
    models_code = state.get("generated_models_code", "")
    contract = state.get("api_contract", [])

    if not structure.get("controllers") and not structure.get("services"):
        logger.warning(
            "No controllers or services found — "
            "skipping business logic generation"
        )
        return {
            "generated_service_code": "",
            "generated_go_code": state.get("generated_go_code", {}),
            "status": "business_logic_skipped",
            "current_node": "business_logic",
        }

    # Build service context
    services_detail = []
    for svc in structure.get("services", []):
        if isinstance(svc, dict):
            services_detail.append({
                "class_name": svc.get("class_name", ""),
                "methods": [
                    {
                        "name": m.get("name", ""),
                        "return_type": m.get("return_type", ""),
                        "annotations": m.get("annotations", []),
                    }
                    for m in svc.get("methods", [])
                ],
                "fields": [
                    {
                        "name": f.get("name", ""),
                        "type": f.get("type", ""),
                        "is_autowired": f.get("is_autowired", False),
                    }
                    for f in svc.get("fields", [])
                ],
                "injected_dependencies": svc.get(
                    "injected_dependencies", []
                ),
            })

    # Build controller-to-service mapping
    controller_service_map = {}
    for cls_name, deps in dependency_graph.items():
        service_deps = [
            d.get("type", "") for d in deps if isinstance(d, dict)
        ]
        if service_deps:
            controller_service_map[cls_name] = service_deps

    context = json.dumps({
        "services": services_detail,
        "dependency_graph": controller_service_map,
        "api_contract": [
            e for e in contract if not e.get("is_exception_handler")
        ],
    }, indent=2, ensure_ascii=False)

    prompt_content = (
        "### CRITICAL INSTRUCTION ###\n"
        "YOU MUST RETURN ONLY A VALID JSON OBJECT. "
        "DO NOT WRITE ANY TEXT, EXPLANATIONS, OR MARKDOWN BEFORE THE JSON.\n"
        "The response must START with '{' and END with '}'.\n"
        "### END INSTRUCTION ###\n\n"

        "You are a Senior Go engineer migrating Java Spring Boot "
        "business logic to Go.\n\n"

        f"## Java Services & Dependencies\n\n"
        f"```json\n{context}\n```\n\n"

        f"## Generated Go Models (models.go)\n\n"
        f"```go\n"
        f"{models_code[:3000] if models_code else '// No models generated yet'}"
        f"\n```\n\n"

        f"## Migration Plan Context\n\n"
        f"{migration_plan[:2000] if migration_plan else 'No plan available.'}"
        f"\n\n"

        "## Requirements\n\n"
        "1. For each Java service class, create a Go struct with:\n"
        "   - Dependencies as struct fields (constructor injection pattern)\n"
        "   - A `New<ServiceName>` constructor function\n"
        "   - Methods that mirror the Java service methods\n"
        "2. Apply these patterns:\n"
        "   - Spring `@Service` → Go struct with methods\n"
        "   - Spring `@Autowired` → Constructor parameter injection\n"
        "   - Spring `@Repository` → Go interface + struct implementation\n"
        "   - JPA `findById` → GORM `First` or `Find`\n"
        "   - JPA `save` → GORM `Create` / `Save`\n"
        "   - Java `Optional<T>` → `(*T, error)` return pattern\n"
        "   - Java exceptions → Go `error` return values\n"
        "   - Java streams → Go `for` loops or utility functions\n"
        "   - Java FTP/SFTP client code → Go equivalents "
        "(e.g., `github.com/jlaffaye/ftp`, `github.com/pkg/sftp`)\n"
        "3. For complex logic that cannot be automatically translated, add:\n"
        "   `// TODO manual migration: <description>`\n"
        "4. Use `package services` for service files.\n"
        "5. Create repository interfaces with `package repository`.\n"
        "6. Include proper error handling (return errors, don't panic).\n"
        "7. Add necessary imports.\n\n"

        "### CRITICAL JSON FORMATTING RULE ###\n"
        "Return a VALID JSON string. DO NOT USE BACKTICKS (`) inside 'content'.\n"
        "All Go code in 'content' MUST be properly escaped:\n"
        "- Use '\\n' for new lines.\n"
        "- Use '\\\"' for double quotes.\n"
        "- Do NOT use raw multi-line strings or backticks.\n"
        "### END RULE ###\n\n"

        "Return ONLY JSON:\n"
        "{\n"
        "  \"files\": [\n"
        "    {\"name\": \"service.go\", \"content\": \"package services\\n...\"},\n"
        "    {\"name\": \"repository.go\", \"content\": \"package repository\\n...\"}\n"
        "  ]\n"
        "}\n"
    )

    settings = APISettings()
    llm = build_mws_chat_llm(settings)

    try:
        msg = await llm.ainvoke([HumanMessage(content=prompt_content)])
        content = (
            msg.content if isinstance(msg.content, str)
            else str(msg.content)
        )

        generated_files = extract_json_files(content)

        if not generated_files:
            logger.warning(
                "LLM did not return valid JSON for business logic"
            )
            return {
                "generated_service_code": content,
                "generated_go_code": state.get("generated_go_code", {}),
                "status": "business_logic_parse_error",
                "current_node": "business_logic",
            }

        # Combine all service/repo files into a single code string
        service_code = "\n\n".join(
            f"// === {fname} ===\n{code}"
            for fname, code in generated_files.items()
        )

        # Merge into generated_go_code
        existing_code = dict(state.get("generated_go_code", {}))
        existing_code.update(generated_files)

        logger.info(
            f"Business logic migration complete: "
            f"{len(generated_files)} files generated"
        )

        return {
            "generated_service_code": service_code,
            "generated_go_code": existing_code,
            "status": "business_logic_complete",
            "current_node": "business_logic",
        }

    except Exception as e:
        logger.exception(f"Error in business logic migration: {e}")
        return {
            "generated_service_code": "",
            "generated_go_code": state.get("generated_go_code", {}),
            "status": f"business_logic_error: {e}",
            "current_node": "business_logic",
        }