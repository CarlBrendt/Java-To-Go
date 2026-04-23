"""
    # Phase 0:  Базовые типы (JsonNode и т.д.)
    # Phase 1:  Унификация package
    # Phase 2:  Построение реестров
    # Phase 3:  Удаление фантомных импортов
    # Phase 4:  Удаление дублей типов
    # Phase 5:  Удаление дублей функций
    # Phase 5.5: Дедупликация методов внутри файлов
    # Phase 6:  Фикс ссылок
    # Пересборка реестров
    # Phase 7:  Генерация заглушек
    # Phase 8:  Пересборка imports
    # Phase 9:  Финальная очистка
    # Phase 10: Safety net для stubs
"""
import re
import logging
from typing import Dict, List, Set, Tuple

from src.copilot.graph_state import MigrationGraphState

logger = logging.getLogger(__name__)

# ─── Типы Java → Go ───
JAVA_TO_GO_TYPE = {
    "String": "string",
    "Integer": "int",
    "int": "int",
    "Long": "int64",
    "long": "int64",
    "Double": "float64",
    "double": "float64",
    "Float": "float32",
    "float": "float32",
    "Boolean": "bool",
    "boolean": "bool",
    "Byte": "byte",
    "byte": "byte",
    "Short": "int16",
    "short": "int16",
    "Character": "rune",
    "char": "rune",
    "Object": "interface{}",
    "BigDecimal": "float64",
    "BigInteger": "int64",
    "Date": "time.Time",
    "LocalDateTime": "time.Time",
    "LocalDate": "time.Time",
    "Instant": "time.Time",
    "ZonedDateTime": "time.Time",
    "UUID": "string",
    "void": "",
    "Void": "",
}

# Импорты, которые точно не существуют в Go
FAKE_IMPORT_PATTERNS = [
    r'"services/',
    r'"repository/',
    r'"models/',
    r'"handlers/',
    r'"controller/',
    r'"internal/',
    r'"dto/',
    r'"entity/',
    r'"ru[./]',
    r'"com[./]',
    r'"org[./]',
    r'"mts/',
    r'"migrated-service/',
    r'"workflow',
]

# Стандартные Go-импорты
STD_IMPORTS = {
    "fmt", "log", "net/http", "os", "context", "time",
    "strings", "strconv", "errors", "io", "bytes",
    "encoding/json", "encoding/xml", "sync", "math",
    "path/filepath", "regexp", "sort", "crypto",
}

# Известные сторонние импорты
KNOWN_THIRD_PARTY = {
    "github.com/gin-gonic/gin",
    "gorm.io/gorm",
    "gorm.io/driver/postgres",
    "gorm.io/driver/mysql",
    "gorm.io/driver/sqlite",
    "github.com/go-playground/validator/v10",
    "github.com/rs/zerolog",
    "github.com/jlaffaye/ftp",
    "github.com/pkg/sftp",
    "github.com/gorilla/websocket",
    "golang.org/x/crypto/ssh",
}


async def node_consolidate(state: MigrationGraphState) -> dict:
    generated_code = dict(state.get("generated_go_code", {}))

    if not generated_code:
        logger.warning("No generated code to consolidate")
        return {
            "generated_go_code": generated_code,
            "status": "consolidate_skipped",
            "current_node": "consolidate",
        }

    fixes_applied: List[str] = []
    # ═══ PHASE 0: Гарантируем базовые типы ═══
    generated_code, f = _phase_ensure_base_types(generated_code)
    fixes_applied.extend(f)

    # ═══ PHASE 1: Унификация package ═══
    generated_code, f = _phase_unify_packages(generated_code)
    fixes_applied.extend(f)

    # ═══ PHASE 2: Построение реестров ═══
    type_registry = _build_type_registry(generated_code)
    func_registry = _build_func_registry(generated_code)
    logger.info(
        f"Initial registry: {len(type_registry)} types, "
        f"{len(func_registry)} functions"
    )

    # ═══ PHASE 3: Удаление фантомных импортов ═══
    generated_code, f = _phase_remove_fake_imports(generated_code)
    fixes_applied.extend(f)

    # ═══ PHASE 4: Удаление дублей типов ═══
    generated_code, f = _phase_remove_duplicate_types(
        generated_code, type_registry
    )
    fixes_applied.extend(f)

    # ═══ PHASE 5: Удаление дублей функций ═══
    generated_code, f = _phase_remove_duplicate_funcs(
        generated_code, func_registry
    )
    fixes_applied.extend(f)

    # ═══ PHASE 5.5: Дедупликация методов внутри файлов ═══
    generated_code, f = _phase_dedup_methods_in_file(generated_code)
    fixes_applied.extend(f)

    # ═══ PHASE 6: Фикс ссылок (package prefixes) ═══
    generated_code, f = _phase_fix_references(
        generated_code, type_registry, func_registry
    )
    fixes_applied.extend(f)

    type_registry = _build_type_registry(generated_code)
    func_registry = _build_func_registry(generated_code)
    logger.info(
        f"Post-dedup registry: {len(type_registry)} types, "
        f"{len(func_registry)} functions"
    )

    # ═══ PHASE 7: Генерация недостающих заглушек ═══
    generated_code, f = _phase_generate_stubs(
        generated_code, type_registry, func_registry
    )
    fixes_applied.extend(f)

    # ═══ PHASE 8: Пересборка import блоков ═══
    generated_code, f = _phase_rebuild_imports(generated_code)
    fixes_applied.extend(f)

    # ═══ PHASE 9: Финальная очистка ═══
    generated_code, f = _phase_final_cleanup(generated_code)
    fixes_applied.extend(f)

    # ══════════════════════════════════════════════
    # PHASE 10: Финальная проверка на дубли
    # (safety net после генерации stubs)
    # ══════════════════════════════════════════════
    final_type_reg = _build_type_registry(generated_code)
    final_func_reg = _build_func_registry(generated_code)

    # Если stubs создали дубли — удаляем из stubs
    generated_code, f = _phase_final_dedup_stubs(
        generated_code, final_type_reg, final_func_reg
    )
    fixes_applied.extend(f)

    final_types = _build_type_registry(generated_code)
    final_funcs = _build_func_registry(generated_code)

    logger.info(
        f"Consolidation complete: {len(fixes_applied)} fixes, "
        f"{len(final_types)} types, {len(final_funcs)} functions, "
        f"{len(generated_code)} files"
    )

    return {
        "generated_go_code": generated_code,
        "consolidation_fixes": fixes_applied,
        "status": "consolidated",
        "current_node": "consolidate",
    }


def _phase_ensure_base_types(
    code: Dict[str, str],
) -> Tuple[Dict[str, str], List[str]]:
    """
    Гарантирует что часто используемые типы-алиасы определены.
    """
    fixes = []
    all_content = _get_all_go_content(code)

    base_types = {
        "JsonNode": "type JsonNode = map[string]interface{}",
        "JsonParseResult": "type JsonParseResult = map[string]interface{}",
        "T": "type T = interface{}",
    }

    missing = []
    for type_name, type_def in base_types.items():
        # Используется в коде?
        if not re.search(rf'\b{type_name}\b', all_content):
            continue
        # Уже определён?
        if re.search(rf'type\s+{type_name}\b', all_content):
            continue
        missing.append(type_def)

    if missing:
        types_file = code.get(
            "types_common.go", "package main\n\n"
        )
        types_file += (
            "\n// Base type aliases\n"
            + "\n".join(missing) + "\n"
        )
        code["types_common.go"] = types_file
        fixes.append(
            f"[consolidate] Added {len(missing)} base type aliases"
        )

    return code, fixes

# ═══════════════════════════════════════════════════
# PHASE 1: Унификация package
# ═══════════════════════════════════════════════════

def _phase_unify_packages(
    code: Dict[str, str],
) -> Tuple[Dict[str, str], List[str]]:
    """Все .go файлы в корне → package main."""
    fixes = []
    for fname, content in code.items():
        if not fname.endswith(".go"):
            continue
        # Только файлы в корне (без /)
        if "/" in fname:
            continue

        lines = content.split("\n")
        for i, line in enumerate(lines):
            stripped = line.strip()
            if stripped.startswith("package "):
                pkg = stripped.split()[1].rstrip(";")
                if pkg != "main":
                    lines[i] = "package main"
                    fixes.append(
                        f"[consolidate] {fname}: "
                        f"package {pkg} → main"
                    )
                break
        else:
            # Нет package declaration — добавляем
            lines.insert(0, "package main\n")
            fixes.append(
                f"[consolidate] {fname}: added package main"
            )

        code[fname] = "\n".join(lines)

    return code, fixes


# ═══════════════════════════════════════════════════
# PHASE 2: Построение реестров
# ═══════════════════════════════════════════════════

def _build_type_registry(
    code: Dict[str, str],
) -> Dict[str, List[str]]:
    """
    Строит реестр: type_name → [files where defined].
    Ищет: type Name struct, type Name interface, type Name = ...
    """
    registry: Dict[str, List[str]] = {}

    for fname, content in code.items():
        if not fname.endswith(".go"):
            continue

        # type Foo struct { ... }
        # type Foo interface { ... }
        # type Foo = bar
        # type Foo int
        for match in re.finditer(
            r'^\s*type\s+(\w+)\s+(?:struct|interface|=|\w)',
            content,
            re.MULTILINE,
        ):
            type_name = match.group(1)
            if type_name not in registry:
                registry[type_name] = []
            registry[type_name].append(fname)

    return registry


def _build_func_registry(
    code: Dict[str, str],
) -> Dict[str, List[str]]:
    """
    Строит реестр: func_name → [files where defined].
    Ищет: func Name(...) и func (r *Receiver) Name(...)
    """
    registry: Dict[str, List[str]] = {}

    for fname, content in code.items():
        if not fname.endswith(".go"):
            continue

        # Standalone functions: func FuncName(
        for match in re.finditer(
            r'^\s*func\s+(\w+)\s*\(',
            content,
            re.MULTILINE,
        ):
            func_name = match.group(1)
            if func_name not in registry:
                registry[func_name] = []
            registry[func_name].append(fname)

        # Methods: func (r *Type) MethodName(
        for match in re.finditer(
            r'^\s*func\s+\([^)]+\)\s+(\w+)\s*\(',
            content,
            re.MULTILINE,
        ):
            method_name = match.group(1)
            key = f"method:{method_name}"
            if key not in registry:
                registry[key] = []
            registry[key].append(fname)

    return registry


# ═══════════════════════════════════════════════════
# PHASE 3: Удаление фантомных импортов
# ═══════════════════════════════════════════════════

def _phase_remove_fake_imports(
    code: Dict[str, str],
) -> Tuple[Dict[str, str], List[str]]:
    """Удаляет импорты несуществующих пакетов."""
    fixes = []

    for fname, content in code.items():
        if not fname.endswith(".go"):
            continue

        new_content, removed = _clean_imports(content)
        if removed:
            code[fname] = new_content
            fixes.append(
                f"[consolidate] {fname}: removed fake imports: "
                f"{', '.join(removed)}"
            )

    return code, fixes


def _clean_imports(content: str) -> Tuple[str, List[str]]:
    """Удаляет фантомные импорты из файла."""
    removed = []

    # Обрабатываем import ( ... ) блоки
    def clean_import_block(match):
        block = match.group(1)
        lines = block.split("\n")
        clean_lines = []

        for line in lines:
            stripped = line.strip()
            if not stripped:
                clean_lines.append(line)
                continue

            # Проверяем, не фантомный ли это импорт
            is_fake = False
            for pattern in FAKE_IMPORT_PATTERNS:
                if re.search(pattern, stripped):
                    is_fake = True
                    # Извлекаем имя для лога
                    imp_match = re.search(r'"([^"]+)"', stripped)
                    if imp_match:
                        removed.append(imp_match.group(1))
                    break

            if not is_fake:
                clean_lines.append(line)

        cleaned = "\n".join(clean_lines)
        # Если блок пустой — удаляем весь import
        if not any(
            l.strip() for l in clean_lines if l.strip()
        ):
            return ""
        return f"import (\n{cleaned}\n)"

    content = re.sub(
        r'import\s*\(\s*\n([\s\S]*?)\n\s*\)',
        clean_import_block,
        content,
    )

    # Обрабатываем одиночные import "..."
    def clean_single_import(match):
        imp_path = match.group(1)
        for pattern in FAKE_IMPORT_PATTERNS:
            if re.search(pattern, f'"{imp_path}"'):
                removed.append(imp_path)
                return ""
        return match.group(0)

    content = re.sub(
        r'import\s+"([^"]+)"\s*\n',
        clean_single_import,
        content,
    )

    return content, removed

# ═══════════════════════════════════════════════════
# PHASE 4: Удаление дублей типов
# ═══════════════════════════════════════════════════

def _phase_remove_duplicate_types(
    code: Dict[str, str],
    type_registry: Dict[str, List[str]],
) -> Tuple[Dict[str, str], List[str]]:
    """
    Если тип определён в нескольких файлах — оставляем
    только в файле с моделями (models_*.go), удаляем из остальных.

    Приоритет файлов (где оставляем):
    1. models_*.go / models.go / types_*.go
    2. service.go / repository.go
    3. handlers.go
    4. остальные
    """
    fixes = []

    FILE_PRIORITY = {
        "models": 0,
        "types": 0,
        "dto": 0,
        "entity": 0,
        "service": 1,
        "repository": 1,
        "repo": 1,
        "handlers": 2,
        "handler": 2,
        "router": 3,
        "main": 4,
    }

    def file_priority(fname: str) -> int:
        base = fname.lower().replace(".go", "")
        # Убираем цифры: models_1 → models
        base_clean = re.sub(r'_\d+$', '', base)
        return FILE_PRIORITY.get(base_clean, 3)

    for type_name, files in type_registry.items():
        if len(files) <= 1:
            continue

        # Сортируем: файл с наименьшим приоритетом — оставляем
        sorted_files = sorted(files, key=file_priority)
        keep_file = sorted_files[0]
        remove_from = sorted_files[1:]

        for fname in remove_from:
            if fname not in code:
                continue
            content = code[fname]
            new_content = _remove_type_definition(
                content, type_name
            )
            if new_content != content:
                code[fname] = new_content
                fixes.append(
                    f"[consolidate] {fname}: removed duplicate "
                    f"type {type_name} (kept in {keep_file})"
                )

    return code, fixes


def _phase_dedup_methods_in_file(
    code: Dict[str, str],
) -> Tuple[Dict[str, str], List[str]]:
    """
    Удаляет дублирующиеся методы ВНУТРИ одного файла.
    LLM иногда генерирует один и тот же метод дважды.
    """
    fixes = []

    for fname, content in code.items():
        if not fname.endswith(".go"):
            continue

        # Находим все методы: func (r *Type) Name(
        # и standalone: func Name(
        methods_found: Dict[str, List[int]] = {}

        for match in re.finditer(
            r'^(\s*func\s+(?:\([^)]+\)\s+)?(\w+)\s*\()',
            content,
            re.MULTILINE,
        ):
            signature_key = match.group(2)  # имя метода
            # Для методов добавляем receiver
            full_line = match.group(1).strip()
            receiver_match = re.match(
                r'func\s+\((\w+)\s+\*?(\w+)\)\s+(\w+)',
                full_line,
            )
            if receiver_match:
                # method: Type.MethodName
                key = f"{receiver_match.group(2)}.{receiver_match.group(3)}"
            else:
                key = signature_key

            if key not in methods_found:
                methods_found[key] = []
            methods_found[key].append(match.start())

        # Удаляем дубли — оставляем первое вхождение
        duplicates_removed = 0
        for key, positions in methods_found.items():
            if len(positions) <= 1:
                continue

            # Удаляем все кроме первого (с конца чтобы не сбить позиции)
            for pos in reversed(positions[1:]):
                # Находим начало функции (включая комментарии)
                func_start = pos
                # Откатываемся на комментарии
                lines_before = content[:pos].split('\n')
                while (
                    lines_before
                    and lines_before[-1].strip().startswith('//')
                ):
                    func_start -= len(lines_before[-1]) + 1
                    lines_before.pop()

                if func_start < 0:
                    func_start = pos

                # Ищем конец функции
                brace_pos = content.find('{', pos)
                if brace_pos == -1:
                    continue

                depth = 0
                i = brace_pos
                while i < len(content):
                    if content[i] == '{':
                        depth += 1
                    elif content[i] == '}':
                        depth -= 1
                        if depth == 0:
                            end = i + 1
                            while (
                                end < len(content)
                                and content[end] in ('\n', '\r')
                            ):
                                end += 1
                            content = (
                                content[:func_start]
                                + content[end:]
                            )
                            duplicates_removed += 1
                            break
                    i += 1

        if duplicates_removed:
            code[fname] = content
            fixes.append(
                f"[consolidate] {fname}: removed "
                f"{duplicates_removed} duplicate methods"
            )

    return code, fixes


def _remove_type_definition(content: str, type_name: str) -> str:
    """
    Удаляет определение типа из файла.
    Обрабатывает:
      type Foo struct { ... }
      type Foo interface { ... }
      type Foo = bar
      type Foo int
    """
    # Паттерн 1: type Name struct/interface { ... }
    # Нужно найти закрывающую скобку с учётом вложенности
    pattern_block = re.compile(
        rf'^(\s*type\s+{re.escape(type_name)}\s+'
        rf'(?:struct|interface)\s*\{{)',
        re.MULTILINE,
    )

    match = pattern_block.search(content)
    if match:
        start = match.start()
        # Ищем закрывающую }
        brace_start = content.index('{', match.start())
        depth = 0
        i = brace_start
        while i < len(content):
            if content[i] == '{':
                depth += 1
            elif content[i] == '}':
                depth -= 1
                if depth == 0:
                    # Удаляем от start до i+1 включительно
                    end = i + 1
                    # Захватываем trailing newlines
                    while (
                        end < len(content)
                        and content[end] in ('\n', '\r')
                    ):
                        end += 1
                    content = content[:start] + content[end:]
                    return content
            i += 1

    # Паттерн 2: type Name = something или type Name int
    pattern_simple = re.compile(
        rf'^\s*type\s+{re.escape(type_name)}\s+=?\s*\S+.*$\n?',
        re.MULTILINE,
    )
    content = pattern_simple.sub('', content)

    return content


# ═══════════════════════════════════════════════════
# PHASE 5: Удаление дублей функций
# ═══════════════════════════════════════════════════

def _phase_remove_duplicate_funcs(
    code: Dict[str, str],
    func_registry: Dict[str, List[str]],
) -> Tuple[Dict[str, str], List[str]]:
    """
    Если функция определена в нескольких файлах —
    оставляем в наиболее подходящем.

    Приоритет:
    - main() → main.go
    - *Handler() → handlers.go
    - Setup*() → router.go
    - New*Service() → service.go
    - остальные → первый файл
    """
    fixes = []

    def best_file_for_func(
        func_name: str, files: List[str]
    ) -> str:
        if func_name == "main":
            for f in files:
                if "main" in f.lower():
                    return f

        if func_name.endswith("Handler"):
            for f in files:
                if "handler" in f.lower():
                    return f

        if func_name.startswith("Setup") or func_name.startswith("Register"):
            for f in files:
                if "router" in f.lower() or "route" in f.lower():
                    return f

        if func_name.startswith("New"):
            for f in files:
                if "service" in f.lower() or "repo" in f.lower():
                    return f

        if "ErrorHandler" in func_name or "Middleware" in func_name:
            for f in files:
                if "handler" in f.lower() or "middleware" in f.lower():
                    return f

        return files[0]

    for func_key, files in func_registry.items():
        if len(files) <= 1:
            continue

        # Пропускаем методы (method:Name) — они могут быть
        # на разных receiver'ах
        if func_key.startswith("method:"):
            continue

        func_name = func_key
        keep_file = best_file_for_func(func_name, files)
        remove_from = [f for f in files if f != keep_file]

        for fname in remove_from:
            if fname not in code:
                continue
            content = code[fname]
            new_content = _remove_func_definition(
                content, func_name
            )
            if new_content != content:
                code[fname] = new_content
                fixes.append(
                    f"[consolidate] {fname}: removed duplicate "
                    f"func {func_name} (kept in {keep_file})"
                )

    return code, fixes


def _remove_func_definition(
    content: str, func_name: str
) -> str:
    """Удаляет определение функции из файла."""
    # Ищем func FuncName(
    pattern = re.compile(
        rf'^(\s*//[^\n]*\n)*'  # опциональные комментарии перед функцией
        rf'\s*func\s+{re.escape(func_name)}\s*\(',
        re.MULTILINE,
    )

    match = pattern.search(content)
    if not match:
        return content

    start = match.start()

    # Ищем начало тела функции {
    brace_pos = content.find('{', match.start())
    if brace_pos == -1:
        return content

    # Ищем закрывающую }
    depth = 0
    i = brace_pos
    while i < len(content):
        if content[i] == '{':
            depth += 1
        elif content[i] == '}':
            depth -= 1
            if depth == 0:
                end = i + 1
                while (
                    end < len(content)
                    and content[end] in ('\n', '\r')
                ):
                    end += 1
                return content[:start] + content[end:]
        i += 1

    return content


# ═══════════════════════════════════════════════════
# PHASE 6: Фикс ссылок (package prefixes)
# ═══════════════════════════════════════════════════

def _phase_fix_references(
    code: Dict[str, str],
    type_registry: Dict[str, List[str]],
    func_registry: Dict[str, List[str]],
) -> Tuple[Dict[str, str], List[str]]:
    """
    Убирает package-префиксы, которые не нужны в package main.

    Примеры:
      models.UserDTO → UserDTO
      services.NewUserService → NewUserService
      repository.UserRepo → UserRepo
    """
    fixes = []

    # Собираем все известные типы и функции
    known_symbols: Set[str] = set()
    for type_name in type_registry:
        known_symbols.add(type_name)
    for func_name in func_registry:
        if not func_name.startswith("method:"):
            known_symbols.add(func_name)

    # Паттерны package-префиксов, которые нужно убрать
    PACKAGE_PREFIXES = [
        "models", "services", "service", "repository",
        "repo", "handlers", "handler", "dto", "entity",
        "internal", "controllers", "controller",
    ]

    for fname, content in code.items():
        if not fname.endswith(".go"):
            continue

        original = content

        for prefix in PACKAGE_PREFIXES:
            # models.TypeName → TypeName
            # Но только если TypeName существует в нашем реестре
            # или выглядит как тип (начинается с заглавной)
            def replace_prefix(match):
                symbol = match.group(1)
                # Если символ есть в реестре — точно заменяем
                if symbol in known_symbols:
                    return symbol
                # Если начинается с заглавной — скорее всего тип
                if symbol and symbol[0].isupper():
                    return symbol
                # Иначе оставляем как есть
                return match.group(0)

            content = re.sub(
                rf'\b{re.escape(prefix)}\.(\w+)',
                replace_prefix,
                content,
            )

        if content != original:
            code[fname] = content
            fixes.append(
                f"[consolidate] {fname}: removed package prefixes"
            )

    return code, fixes


# ═══════════════════════════════════════════════════
# PHASE 7: Генерация недостающих заглушек
# ═══════════════════════════════════════════════════

def _phase_generate_stubs(
    code: Dict[str, str],
    type_registry: Dict[str, List[str]],
    func_registry: Dict[str, List[str]],
) -> Tuple[Dict[str, str], List[str]]:
    """
    Находит типы и функции, которые используются,
    но не определены — и генерирует заглушки.

    ВАЖНО: НЕ генерирует заглушки для уже определённых символов.
    """
    fixes = []
    all_content = _get_all_go_content(code)

    # ── Собираем ВСЕ определённые символы ──
    defined_types = set(type_registry.keys())
    defined_funcs = set()
    for func_key in func_registry:
        if func_key.startswith("method:"):
            defined_funcs.add(func_key[7:])  # убираем "method:"
        else:
            defined_funcs.add(func_key)

    # ── Собираем все использованные типы ──
    used_types: Set[str] = set()

    # В struct fields: FieldName TypeName `json:"..."`
    for match in re.finditer(
        r'\b\w+\s+\*?(\[?\]?[A-Z]\w+)\s+`', all_content
    ):
        clean = match.group(1).lstrip("[]*")
        if clean:
            used_types.add(clean)

    # В []Type, *Type, map[...]Type
    for match in re.finditer(
        r'(?:\[\]|\*|map\[\w+\])([A-Z]\w+)', all_content
    ):
        used_types.add(match.group(1))

    # В присвоениях и возвратах: &TypeName{}, TypeName{}
    for match in re.finditer(
        r'[&]?([A-Z]\w+)\s*\{', all_content
    ):
        used_types.add(match.group(1))

    # В сигнатурах функций: func(...) *TypeName
    for match in re.finditer(
        r'\)\s+\*?([A-Z]\w+)', all_content
    ):
        used_types.add(match.group(1))

    # Go built-in и framework типы — НЕ нужно генерировать
    SKIP_TYPES = {
        # Go builtins
        "Context", "Time", "Duration", "Reader", "Writer",
        "Error", "Mutex", "WaitGroup", "Once",
        # Gin
        "Engine", "HandlerFunc", "RouterGroup", "H",
        # GORM
        "DB", "Model",
        # Стандартные
        "Logger", "Server", "Request", "Response",
        "Header", "Cookie", "URL", "File",
        "Buffer", "Builder",
        # Наши автогенерированные
        "JsonNode",
    }

    missing_types = used_types - defined_types - SKIP_TYPES

    # Фильтруем: только реально используемые как типы
    real_missing: List[str] = []
    for t in sorted(missing_types):
        # Проверяем контекст использования
        type_usage = re.search(
            rf'(?:'
            rf'\b\w+\s+\*?{re.escape(t)}\b'       # field Type
            rf'|\[\]{re.escape(t)}\b'               # []Type
            rf'|\*{re.escape(t)}\b'                 # *Type
            rf'|map\[.*?\]\*?{re.escape(t)}\b'     # map[K]Type
            rf'|{re.escape(t)}\{{'                  # Type{
            rf'|&{re.escape(t)}\{{'                 # &Type{
            rf'|\)\s+\*?{re.escape(t)}\b'          # ) Type (return)
            rf')',
            all_content,
        )
        if type_usage:
            real_missing.append(t)

    # ── Генерируем заглушки для типов ──
    stubs_lines: List[str] = []

    if real_missing:
        stubs_lines.append("package main\n")
        stubs_lines.append(
            "// Auto-generated stubs for missing types."
        )
        stubs_lines.append(
            "// TODO: Replace with real implementations.\n"
        )

        for type_name in real_missing:
            stubs_lines.append(
                f"// TODO: Define {type_name} properly"
            )
            stubs_lines.append(
                f"type {type_name} struct{{}}\n"
            )

        fixes.append(
            f"[consolidate] Generated stubs for "
            f"{len(real_missing)} missing types: "
            f"{', '.join(real_missing[:10])}"
        )

    # ── Проверяем вызовы New*() конструкторов ──
    new_calls = set(re.findall(r'\bNew(\w+)\s*\(', all_content))

    missing_constructors = []
    for name in sorted(new_calls):
        full_name = f"New{name}"

        # ⚠️ КЛЮЧЕВАЯ ПРОВЕРКА: не генерируем,
        # если функция УЖЕ ОПРЕДЕЛЕНА
        if full_name in defined_funcs:
            continue

        # Проверяем, что тип существует
        if (
            name in defined_types
            or name in real_missing
        ):
            missing_constructors.append((full_name, name))

    if missing_constructors:
        if not stubs_lines:
            stubs_lines.append("package main\n")

        stubs_lines.append(
            "\n// Auto-generated constructor stubs\n"
        )

        for func_name, type_name in missing_constructors:
            stubs_lines.append(
                f"// TODO: Implement {func_name} properly"
            )
            stubs_lines.append(
                f"func {func_name}() *{type_name} {{"
            )
            stubs_lines.append(
                f"\treturn &{type_name}{{}}"
            )
            stubs_lines.append("}\n")

        fixes.append(
            f"[consolidate] Generated "
            f"{len(missing_constructors)} constructor stubs: "
            f"{', '.join(f[0] for f in missing_constructors[:10])}"
        )

    # ── Проверяем использование функций-хелперов ──
    # Например: ErrorHandlerMiddleware, CORSMiddleware
    # НЕ генерируем если уже определены!
    helper_patterns = {
        "ErrorHandlerMiddleware": (
            "func ErrorHandlerMiddleware() gin.HandlerFunc {\n"
            "\treturn func(c *gin.Context) {\n"
            "\t\tdefer func() {\n"
            "\t\t\tif err := recover(); err != nil {\n"
            '\t\t\t\tc.JSON(500, gin.H{"error": '
            '"Internal server error"})\n'
            "\t\t\t\tc.Abort()\n"
            "\t\t\t}\n"
            "\t\t}()\n"
            "\t\tc.Next()\n"
            "\t}\n"
            "}"
        ),
        "CORSMiddleware": (
            "func CORSMiddleware() gin.HandlerFunc {\n"
            "\treturn func(c *gin.Context) {\n"
            '\t\tc.Header("Access-Control-Allow-Origin", "*")\n'
            "\t\tc.Next()\n"
            "\t}\n"
            "}"
        ),
    }

    for helper_name, helper_code in helper_patterns.items():
        # Используется в коде?
        if helper_name not in all_content:
            continue
        # Уже определена?
        if helper_name in defined_funcs:
            continue

        if not stubs_lines:
            stubs_lines.append("package main\n")

        stubs_lines.append(f"\n{helper_code}\n")
        fixes.append(
            f"[consolidate] Generated missing helper: "
            f"{helper_name}"
        )

    # ── Сохраняем stubs файл ──
    if stubs_lines:
        code["stubs_generated.go"] = "\n".join(stubs_lines)
    elif "stubs_generated.go" in code:
        # Если стабы не нужны — удаляем файл
        del code["stubs_generated.go"]
        fixes.append(
            "[consolidate] Removed stubs_generated.go "
            "(no stubs needed)"
        )

    return code, fixes

# ═══════════════════════════════════════════════════
# PHASE 8: Пересборка import блоков
# ═══════════════════════════════════════════════════

def _phase_rebuild_imports(
    code: Dict[str, str],
) -> Tuple[Dict[str, str], List[str]]:
    """
    Для каждого файла:
    1. Удаляет ВСЕ существующие import блоки
    2. Сканирует код на использование пакетов
    3. Строит новый чистый import блок
    """
    fixes = []

    for fname, content in code.items():
        if not fname.endswith(".go"):
            continue

        new_content, changed = _rebuild_file_imports(content)
        if changed:
            code[fname] = new_content
            fixes.append(
                f"[consolidate] {fname}: rebuilt imports"
            )

    return code, fixes


def _rebuild_file_imports(content: str) -> Tuple[str, bool]:
    """Полностью пересобирает import блок для файла."""

    # 1. Извлекаем package declaration
    pkg_match = re.search(r'^package\s+\w+', content, re.MULTILINE)
    if not pkg_match:
        return content, False

    # 2. Удаляем все import блоки и одиночные import
    content_no_imports = re.sub(
        r'import\s*\(\s*\n[\s\S]*?\n\s*\)\s*\n?',
        '',
        content,
    )
    content_no_imports = re.sub(
        r'import\s+"[^"]+"\s*\n',
        '',
        content_no_imports,
    )

    # 3. Код без package и import — чистое тело
    pkg_line = pkg_match.group(0)
    body_start = content_no_imports.find(pkg_line) + len(pkg_line)
    body = content_no_imports[body_start:].strip()

    # 4. Определяем нужные импорты по использованию
    needed_imports = _detect_needed_imports(body)

    # 5. Собираем файл заново
    parts = [pkg_line, ""]

    if needed_imports:
        std_imports = []
        third_party = []

        for imp in sorted(needed_imports):
            # Стандартные библиотеки не содержат точку в пути
            # (кроме golang.org/x/...)
            imp_clean = imp.strip('"')
            if (
                "." not in imp_clean
                or imp_clean.startswith("golang.org/x/")
            ):
                std_imports.append(imp)
            else:
                third_party.append(imp)

        import_lines = []
        import_lines.append("import (")

        if std_imports:
            for imp in std_imports:
                import_lines.append(f"\t{imp}")

        if std_imports and third_party:
            import_lines.append("")  # пустая строка между группами

        if third_party:
            for imp in third_party:
                import_lines.append(f"\t{imp}")

        import_lines.append(")")
        parts.append("\n".join(import_lines))

    parts.append("")
    parts.append(body)

    new_content = "\n".join(parts)

    changed = new_content.strip() != content.strip()
    return new_content, changed


def _detect_needed_imports(body: str) -> Set[str]:
    """
    Анализирует тело Go-файла и определяет,
    какие импорты нужны.
    """
    imports: Set[str] = set()

    # ── Стандартная библиотека ──
    USAGE_TO_IMPORT = {
        # fmt
        r'\bfmt\.': '"fmt"',
        # log
        r'\blog\.': '"log"',
        # net/http
        r'\bhttp\.': '"net/http"',
        # os
        r'\bos\.': '"os"',
        # context
        r'\bcontext\.': '"context"',
        # time
        r'\btime\.': '"time"',
        # strings
        r'\bstrings\.': '"strings"',
        # strconv
        r'\bstrconv\.': '"strconv"',
        # errors
        r'\berrors\.': '"errors"',
        # io
        r'\bio\.': '"io"',
        # bytes
        r'\bbytes\.': '"bytes"',
        # encoding/json
        r'\bjson\.': '"encoding/json"',
        # encoding/xml
        r'\bxml\.': '"encoding/xml"',
        # sync
        r'\bsync\.': '"sync"',
        # math
        r'\bmath\.': '"math"',
        # filepath
        r'\bfilepath\.': '"path/filepath"',
        # regexp
        r'\bregexp\.': '"regexp"',
        # sort
        r'\bsort\.': '"sort"',
        # signal
        r'\bsignal\.': '"os/signal"',
        # syscall
        r'\bsyscall\.': '"syscall"',
    }

    for pattern, imp in USAGE_TO_IMPORT.items():
        if re.search(pattern, body):
            imports.add(imp)

    # ── Сторонние библиотеки ──
    THIRD_PARTY_USAGE = {
        r'\bgin\.': '"github.com/gin-gonic/gin"',
        r'\bgorm\.': '"gorm.io/gorm"',
        r'\bpostgres\.': '"gorm.io/driver/postgres"',
        r'\bmysql\.': '"gorm.io/driver/mysql"',
        r'\bvalidator\.': '"github.com/go-playground/validator/v10"',
        r'\bzerolog\.': '"github.com/rs/zerolog"',
        r'\bftp\.': '"github.com/jlaffaye/ftp"',
        r'\bsftp\.': '"github.com/pkg/sftp"',
        r'\bwebsocket\.': '"github.com/gorilla/websocket"',
        r'\bssh\.': '"golang.org/x/crypto/ssh"',
    }

    for pattern, imp in THIRD_PARTY_USAGE.items():
        if re.search(pattern, body):
            imports.add(imp)

    return imports


# ═══════════════════════════════════════════════════
# PHASE 9: Финальная очистка
# ═══════════════════════════════════════════════════

# src/copilot/nodes/consolidation_node.py — добавляем в _phase_final_cleanup

def _phase_final_cleanup(
    code: Dict[str, str],
) -> Tuple[Dict[str, str], List[str]]:
    """Финальная чистка — улучшенная версия."""
    fixes = []

    for fname, content in code.items():
        if not fname.endswith(".go"):
            continue
        
        original = content

        # ── НОВОЕ: Чистим Java массивы Object[] → []interface{} ──
        # Object[] → []interface{}
        content = re.sub(r'\bObject\s*\[\s*\]', '[]interface{}', content)
        # String[] → []string
        content = re.sub(r'\bString\s*\[\s*\]', '[]string', content)
        # int[] → []int
        content = re.sub(r'\bint\s*\[\s*\]', '[]int', content)
        # long[] → []int64
        content = re.sub(r'\blong\s*\[\s*\]', '[]int64', content)
        # byte[] → []byte
        content = re.sub(r'\bbyte\s*\[\s*\]', '[]byte', content)

        # ── НОВОЕ: Чистим Java generics с массивами ──
        # List<Object> → []interface{}
        content = re.sub(r'\bList\s*<\s*Object\s*>', '[]interface{}', content)
        # List<String> → []string
        content = re.sub(r'\bList\s*<\s*String\s*>', '[]string', content)
        # List<Integer> → []int
        content = re.sub(r'\bList\s*<\s*Integer\s*>', '[]int', content)
        # List<Long> → []int64
        content = re.sub(r'\bList\s*<\s*Long\s*>', '[]int64', content)

        # ── НОВОЕ: Чистим поля типа Object (без массива) ──
        # поле Object → interface{}
        content = re.sub(
            r'(\s+)(\w+)\s+Object\s+`',
            r'\1\2 interface{} `',
            content
        )

        # ── НОВОЕ: Чистим MessageArgs Object[] → MessageArgs []interface{} ──
        content = re.sub(
            r'(\w+)\s+Object\s*\[\s*\]',
            r'\1 []interface{}',
            content
        )

        # ── Существующие чистки ──
        # Удаляем Java-аннотации
        content = re.sub(r'@\w+(?:\([^)]*\))?\s*\n', '\n', content)
        content = content.replace('.class', '')

        # Фикс struct tags
        content = re.sub(r'`([^`]+)`\s+`([^`]+)`', r'`\1 \2`', content)

        # Java generics
        content = re.sub(r'\bOptional<(\w+)>', r'*\1', content)
        content = re.sub(r'\bResponseEntity<(\w+)>', r'\1', content)

        # uuid.UUID → string
        content = re.sub(r'\buuid\.UUID\b', 'string', content)

        # *error → error
        content = re.sub(r'\*error\b', 'error', content)

        # Убираем лишние пустые строки
        content = re.sub(r'\n{4,}', '\n\n\n', content)

        if content != original:
            code[fname] = content
            fixes.append(f"{fname}: cleaned Java array syntax")

    return code, fixes


def _phase_final_dedup_stubs(
    code: Dict[str, str],
    type_registry: Dict[str, List[str]],
    func_registry: Dict[str, List[str]],
) -> Tuple[Dict[str, str], List[str]]:
    """
    Safety net: если stubs_generated.go содержит символы,
    которые определены в других файлах — удаляем из stubs.
    """
    fixes = []
    stubs_file = "stubs_generated.go"

    if stubs_file not in code:
        return code, fixes

    content = code[stubs_file]
    original = content

    # Проверяем каждый тип в stubs
    for type_name, files in type_registry.items():
        if stubs_file not in files:
            continue
        # Если тип определён ещё где-то кроме stubs — удаляем из stubs
        other_files = [f for f in files if f != stubs_file]
        if other_files:
            content = _remove_type_definition(content, type_name)
            fixes.append(
                f"[consolidate] stubs: removed duplicate type "
                f"{type_name} (exists in {other_files[0]})"
            )

    # Проверяем каждую функцию в stubs
    for func_key, files in func_registry.items():
        if stubs_file not in files:
            continue
        func_name = (
            func_key[7:] if func_key.startswith("method:")
            else func_key
        )
        other_files = [f for f in files if f != stubs_file]
        if other_files:
            content = _remove_func_definition(content, func_name)
            fixes.append(
                f"[consolidate] stubs: removed duplicate func "
                f"{func_name} (exists in {other_files[0]})"
            )

    # Если stubs стал пустым — удаляем файл
    remaining = content.strip()
    is_empty = (
        not remaining
        or re.fullmatch(
            r'package\s+main\s*'
            r'(?://[^\n]*\n?\s*)*',
            remaining,
        )
    )

    if is_empty:
        del code[stubs_file]
        fixes.append(
            "[consolidate] Removed stubs_generated.go "
            "(all stubs were duplicates)"
        )
    elif content != original:
        code[stubs_file] = content

    return code, fixes

# ═══════════════════════════════════════════════════
# Дополнительные утилиты
# ═══════════════════════════════════════════════════

def _get_all_go_content(code: Dict[str, str]) -> str:
    """Объединяет весь Go-код в одну строку для поиска."""
    return "\n".join(
        content for fname, content in code.items()
        if fname.endswith(".go")
    )

def _generate_return_stub(func_sig: str) -> str:
    """По сигнатуре функции генерирует return statement."""
    if not func_sig:
        return ""

    # Убираем всё до последнего ) перед {
    # Сигнатура: func (r *T) Name(params) (RetType1, RetType2) {
    # Нам нужно извлечь (RetType1, RetType2) или RetType

    # Находим позицию открывающей {
    brace_pos = func_sig.rfind('{')
    if brace_pos == -1:
        return ""

    # Берём часть между последним ) параметров и {
    before_brace = func_sig[:brace_pos].rstrip()

    # Ищем return type — это то, что после последней ) от параметров
    # Стратегия: идём с конца, ищем return type
    # Если заканчивается на ) — это множественный return (Type1, Type2)
    # Иначе — одиночный return type или ничего

    if before_brace.endswith(')'):
        # Может быть: ...params) (int, error)
        # или: ...params)
        # Нужно найти парную ( для последней )
        depth = 0
        i = len(before_brace) - 1
        while i >= 0:
            if before_brace[i] == ')':
                depth += 1
            elif before_brace[i] == '(':
                depth -= 1
                if depth == 0:
                    ret_part = before_brace[i:].strip()
                    # Проверяем: это return type или параметры?
                    # Если перед ( стоит ) — это return type
                    before_paren = before_brace[:i].rstrip()
                    if before_paren.endswith(')'):
                        # Это return type: (int, error)
                        inner = ret_part[1:-1]  # убираем ( )
                        types = [t.strip() for t in inner.split(',')]
                        values = [_zero_value(t) for t in types]
                        return f'return {", ".join(values)}'
                    else:
                        # Это параметры функции — нет return type
                        return ""
            i -= 1
    else:
        # Одиночный return type: func Foo() error {
        # Ищем последнюю ) и берём что после неё
        last_paren = before_brace.rfind(')')
        if last_paren == -1:
            return ""
        ret = before_brace[last_paren + 1:].strip()
        if ret:
            return f'return {_zero_value(ret)}'

    return ""


def _zero_value(go_type: str) -> str:
    """Возвращает zero value для Go типа."""
    t = go_type.strip()
    if t == 'error':
        return 'nil'
    if t.startswith('*') or t.startswith('[]') or t.startswith('map['):
        return 'nil'
    if t == 'string':
        return '""'
    if t == 'bool':
        return 'false'
    if t in ('int', 'int8', 'int16', 'int32', 'int64',
             'uint', 'uint8', 'uint16', 'uint32', 'uint64',
             'float32', 'float64', 'byte', 'rune'):
        return '0'
    if t == 'interface{}':
        return 'nil'
    if t and t[0].isupper():
        return 'nil'
    return 'nil'