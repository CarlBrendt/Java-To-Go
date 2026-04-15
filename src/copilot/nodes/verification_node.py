from __future__ import annotations

import re
import logging
from typing import Dict, Any, List

from src.copilot.graph_state import MigrationGraphState

logger = logging.getLogger(__name__)

# Типы Spring/Java которые НЕ нужно искать в Go-моделях
SKIP_TYPES = {
    # Java primitives & wrappers
    "void", "Void", "String", "Object", "Integer", "Long",
    "Double", "Float", "Boolean", "Byte", "Short", "Character",
    "int", "long", "double", "float", "boolean", "byte",
    # Java collections
    "List", "Set", "Map", "Collection", "Optional",
    "ArrayList", "HashMap", "HashSet",
    # Spring types
    "ResponseEntity", "RedirectView", "ModelAndView",
    "HttpStatus", "MultipartFile", "Resource",
    "HttpServletRequest", "HttpServletResponse",
    "BindingResult", "Pageable", "Page", "Sort",
    "Authentication", "Principal",
    # Generic wrappers
    "CompletableFuture", "Mono", "Flux",
}


async def node_verify_code(
    state: MigrationGraphState,
) -> Dict[str, Any]:
    """
    Проверяет корректность сгенерированного кода против контракта API.
    """
    contract = state.get("api_contract", [])
    code_files = state.get("generated_go_code", {})

    errors: List[str] = []
    manual_fixes: List[str] = []
    is_valid = True

    # ── 1. Проверка наличия ключевых файлов ──
    # Ищем файлы по содержимому, а не по имени
    has_models = False
    has_handlers = False
    has_main = False

    models_content = ""
    handlers_content = ""

    for filename, content in code_files.items():
        lower_name = filename.lower()
        lower_content = content.lower()

        # Ищем файл с моделями (может быть models.go, dto.go, types.go, и т.д.)
        if any(kw in lower_name for kw in ("model", "dto", "type", "entity")):
            has_models = True
            models_content += "\n" + content
        elif "type " in content and "struct " in content:
            # Файл содержит определения структур
            has_models = True
            models_content += "\n" + content

        # Ищем файл с хэндлерами
        if any(kw in lower_name for kw in ("handler", "router", "route", "controller")):
            has_handlers = True
            handlers_content += "\n" + content

        # main.go
        if "main" in lower_name or "func main()" in content:
            has_main = True
            handlers_content += "\n" + content  # main может содержать роуты

    # Если models.go нет, но есть другие файлы со структурами — OK
    if not has_models:
        # Проверяем ВСЕ файлы на наличие struct определений
        all_content = "\n".join(code_files.values())
        if "struct {" in all_content or "struct{" in all_content:
            has_models = True
            models_content = all_content

    if not has_models and not models_content:
        # Не критично — просто предупреждение
        manual_fixes.append(
            "Файлы с Go-моделями (structs) не найдены. "
            "Проверьте, что DTO были сгенерированы корректно."
        )

    if not has_main:
        errors.append("Файл с `func main()` не найден.")
        manual_fixes.append(
            "Создать main.go с точкой входа и настройкой роутера."
        )
        is_valid = False

    # Собираем весь контент для поиска
    all_go_content = "\n".join(code_files.values())

    if not handlers_content:
        handlers_content = all_go_content

    # ── 2. Проверка DTO (типов из контракта) ──
    types_in_contract: set = set()

    for endpoint in contract:
        if endpoint.get("is_exception_handler"):
            continue
        for key in ("response_type", "request_type"):
            raw = endpoint.get(key, "")
            if not raw:
                continue
            # Убираем дженерики: ResponseEntity<List<UserDTO>> -> UserDTO
            # Разбираем вложенные generic-и
            clean = raw
            while '<' in clean:
                clean = re.sub(r'[^<]*<', '', clean, count=1)
            clean = clean.rstrip('>')
            clean = clean.split('.')[-1].strip()
            # Убираем массивы
            clean = clean.replace("[]", "")
            if clean and clean not in SKIP_TYPES:
                types_in_contract.add(clean)

    missing_types: List[str] = []
    search_content = models_content if models_content else all_go_content

    for type_name in types_in_contract:
        if not re.search(
            rf'\b{re.escape(type_name)}\b', search_content
        ):
            missing_types.append(type_name)

    if missing_types:
        # Не делаем is_valid = False — это предупреждение
        manual_fixes.append(
            f"Типы из контракта не найдены в Go-коде: "
            f"{', '.join(missing_types)}. "
            f"Проверьте, что соответствующие структуры созданы."
        )

    # ── 3. Проверка эндпоинтов ──
    missing_endpoints: List[str] = []
    for endpoint in contract:
        if endpoint.get("is_exception_handler"):
            continue

        path = endpoint.get("path", "")
        method = endpoint.get("method", "").upper()

        if not path or not method:
            continue

        # Нормализуем путь для поиска
        search_path = path.replace("{", ":").rstrip("}")

        found = False

        # Ищем в ВСЁМ коде, не только в handlers
        search_in = all_go_content

        # Паттерн 1: router.GET("/path", handler)
        pattern1 = rf'\.{method}\s*\(\s*["\']'
        if re.search(pattern1, search_in, re.IGNORECASE):
            if (
                re.search(re.escape(path), search_in)
                or re.search(re.escape(search_path), search_in)
            ):
                found = True

        # Паттерн 2: мягкий — метод и путь рядом
        if not found:
            if (
                method.lower() in search_in.lower()
                and (path in search_in or search_path in search_in)
            ):
                found = True

        # Паттерн 3: HandleFunc("/path", ...)
        if not found:
            if path in search_in or search_path in search_in:
                found = True

        if not found:
            missing_endpoints.append(f"{method} {path}")
            is_valid = False

    if missing_endpoints:
        errors.append(
            f"Эндпоинты не найдены в Go-коде: "
            f"{', '.join(missing_endpoints[:10])}"
        )
        for ep in missing_endpoints:
            manual_fixes.append(
                f"Реализовать эндпоинт `{ep}` — "
                f"не найден в сгенерированном коде."
            )

    # ── 4. Проверка package declarations ──
    for filename, content in code_files.items():
        if filename.endswith(".go") and content.strip():
            if not content.strip().startswith("package"):
                errors.append(
                    f"Файл `{filename}` не начинается с "
                    f"'package' declaration."
                )
                manual_fixes.append(
                    f"Добавить `package ...` в начало `{filename}`."
                )
                is_valid = False

    # ── 5. Проверка базовых синтаксических проблем ──
    for filename, content in code_files.items():
        if not filename.endswith(".go"):
            continue

        open_braces = content.count('{')
        close_braces = content.count('}')
        if abs(open_braces - close_braces) > 2:  # Допуск на строки в строках
            manual_fixes.append(
                f"Проверить синтаксис в `{filename}` — "
                f"несовпадение скобок "
                f"({{ = {open_braces}, }} = {close_braces})."
            )

    # ── 6. go.mod ──
    if "go.mod" not in code_files:
        manual_fixes.append(
            "Создать go.mod — выполнить "
            "`go mod init <module>` и `go mod tidy`."
        )

    return {
        "is_valid": is_valid,
        "errors": errors,
        "manual_fixes": manual_fixes,
    }


async def node_verify_node(state: MigrationGraphState) -> dict:
    """LangGraph node wrapper for verification."""
    result = await node_verify_code(state)

    logger.info(
        f"Verification: "
        f"{'PASSED' if result['is_valid'] else 'FAILED'} "
        f"({len(result['errors'])} errors, "
        f"{len(result['manual_fixes'])} manual fixes)"
    )

    if result['errors']:
        for err in result['errors']:
            logger.warning(f"  Verification error: {err}")

    if result['manual_fixes']:
        for fix in result['manual_fixes'][:5]:
            logger.info(f"  Manual fix: {fix}")

    return {
        "verification_passed": result["is_valid"],
        "verification_errors": result["errors"],
        "manual_fixes": result["manual_fixes"],
        "status": (
            "verified" if result["is_valid"]
            else "verification_failed"
        ),
        "current_node": "verify",
    }