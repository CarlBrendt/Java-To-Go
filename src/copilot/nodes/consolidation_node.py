# src/copilot/nodes/consolidation_node.py

from __future__ import annotations

import re
import logging
from typing import Dict, List, Set, Tuple, Optional

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
    """
    Нода консолидации: превращает разрозненный LLM-вывод
    в единый компилируемый Go-проект (package main).

    Работает ПОЛНОСТЬЮ ДЕТЕРМИНИСТИЧЕСКИ — без вызовов LLM.

    Шаги:
    1. Унификация package → всё `package main`
    2. Сбор реестра определённых типов и функций
    3. Удаление фантомных импортов
    4. Удаление дублей типов
    5. Фикс ссылок между файлами
    6. Генерация недостающих заглушек
    7. Проверка и фикс import блоков
    """
    generated_code = dict(state.get("generated_go_code", {}))

    if not generated_code:
        logger.warning("No generated code to consolidate")
        return {
            "generated_go_code": generated_code,
            "status": "consolidate_skipped",
            "current_node": "consolidate",
        }

    fixes_applied: List[str] = []

    # ═══════════════════════════════════════════
    # PHASE 1: Унификация package
    # ═══════════════════════════════════════════
    generated_code, phase1_fixes = _phase_unify_packages(generated_code)
    fixes_applied.extend(phase1_fixes)

    # ═══════════════════════════════════════════
    # PHASE 2: Построение реестров
    # ═══════════════════════════════════════════
    type_registry = _build_type_registry(generated_code)
    func_registry = _build_func_registry(generated_code)
    logger.info(
        f"Registry: {len(type_registry)} types, "
        f"{len(func_registry)} functions"
    )

    # ═══════════════════════════════════════════
    # PHASE 3: Удаление фантомных импортов
    # ═══════════════════════════════════════════
    generated_code, phase3_fixes = _phase_remove_fake_imports(
        generated_code
    )
    fixes_applied.extend(phase3_fixes)

    # ═══════════════════════════════════════════
    # PHASE 4: Удаление дублей типов
    # ═══════════════════════════════════════════
    generated_code, phase4_fixes = _phase_remove_duplicate_types(
        generated_code, type_registry
    )
    fixes_applied.extend(phase4_fixes)

    # ═══════════════════════════════════════════
    # PHASE 5: Удаление дублей функций
    # ═══════════════════════════════════════════
    generated_code, phase5_fixes = _phase_remove_duplicate_funcs(
        generated_code, func_registry
    )
    fixes_applied.extend(phase5_fixes)

    # ═══════════════════════════════════════════
    # PHASE 6: Фикс ссылок (package prefixes)
    # ═══════════════════════════════════════════
    generated_code, phase6_fixes = _phase_fix_references(
        generated_code, type_registry, func_registry
    )
    fixes_applied.extend(phase6_fixes)

    # ═══════════════════════════════════════════
    # PHASE 7: Генерация недостающих заглушек
    # ═══════════════════════════════════════════
    generated_code, phase7_fixes = _phase_generate_stubs(
        generated_code, type_registry, func_registry
    )
    fixes_applied.extend(phase7_fixes)

    # ═══════════════════════════════════════════
    # PHASE 8: Пересборка import блоков
    # ═══════════════════════════════════════════
    generated_code, phase8_fixes = _phase_rebuild_imports(
        generated_code
    )
    fixes_applied.extend(phase8_fixes)

    # ═══════════════════════════════════════════
    # PHASE 9: Финальная очистка
    # ═══════════════════════════════════════════
    generated_code, phase9_fixes = _phase_final_cleanup(
        generated_code
    )
    fixes_applied.extend(phase9_fixes)

    # Обновляем реестр после всех фиксов
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
    """
    fixes = []
    all_content = "\n".join(code.values())

    # ── Собираем все использованные типы ──
    # Паттерн: слово с заглавной буквы, используемое как тип
    used_types: Set[str] = set()

    # В объявлениях полей: FieldName TypeName
    for match in re.finditer(
        r'\b(\w+)\s+([A-Z]\w+)\s+`', all_content
    ):
        used_types.add(match.group(2))

    # В сигнатурах функций
    for match in re.finditer(
        r'(?:func|var|:=|=)\s*.*?([A-Z]\w+)', all_content
    ):
        candidate = match.group(1)
        if len(candidate) > 2:
            used_types.add(candidate)

    # В []Type, *Type, map[...]Type
    for match in re.finditer(
        r'(?:\[\]|\*|map\[\w+\])([A-Z]\w+)', all_content
    ):
        used_types.add(match.group(1))

    # Типы, которые определены
    defined_types = set(type_registry.keys())

    # Go built-in типы, которые не нужно определять
    BUILTIN_TYPES = {
        "Context", "Time", "Duration", "Reader", "Writer",
        "Error", "Engine", "HandlerFunc", "RouterGroup",
        "H",  # gin.H
        "DB",  # gorm.DB
        "Model",  # gorm.Model
        "Logger",
        "Server",
    }

    missing_types = (
        used_types - defined_types - BUILTIN_TYPES
    )

    # Фильтруем: только те, что реально используются как типы
    # (не просто слова в комментариях)
    real_missing: List[str] = []
    for t in sorted(missing_types):
        # Проверяем, что это реально используется как тип
        # (после : или в struct field или в []Type)
        type_usage = re.search(
            rf'(?:'
            rf'\b\w+\s+\*?{re.escape(t)}\b'  # field Type
            rf'|\[\]{re.escape(t)}\b'           # []Type
            rf'|\*{re.escape(t)}\b'             # *Type
            rf'|map\[.*?\]{re.escape(t)}\b'     # map[K]Type
            rf'|{re.escape(t)}\{{'              # Type{
            rf'|&{re.escape(t)}\{{'             # &Type{
            rf')',
            all_content,
        )
        if type_usage:
            real_missing.append(t)

    if real_missing:
        stubs = ["package main\n"]
        stubs.append(
            "// Auto-generated stubs for missing types.\n"
            "// TODO: Replace with real implementations.\n"
        )

        for type_name in real_missing:
            stubs.append(
                f"// TODO: Define {type_name} properly\n"
                f"type {type_name} struct{{}}\n"
            )

        code["stubs_generated.go"] = "\n".join(stubs)
        fixes.append(
            f"[consolidate] Generated stubs for "
            f"{len(real_missing)} missing types: "
            f"{', '.join(real_missing[:10])}"
        )

    # ── Проверяем вызовы New*() конструкторов ──
    new_calls = re.findall(r'\bNew(\w+)\s*\(', all_content)
    defined_funcs = set(func_registry.keys())

    missing_constructors = []
    for name in set(new_calls):
        full_name = f"New{name}"
        if full_name not in defined_funcs:
            # Проверяем, что тип существует
            if name in defined_types or name in real_missing:
                missing_constructors.append((full_name, name))

    if missing_constructors:
        stub_file = code.get("stubs_generated.go", "package main\n\n")
        stub_file += "\n// Auto-generated constructor stubs\n\n"

        for func_name, type_name in missing_constructors:
            stub_file += (
                f"// TODO: Implement {func_name} properly\n"
                f"func {func_name}() *{type_name} {{\n"
                f"\treturn &{type_name}{{}}\n"
                f"}}\n\n"
            )

        code["stubs_generated.go"] = stub_file
        fixes.append(
            f"[consolidate] Generated {len(missing_constructors)} "
            f"constructor stubs: "
            f"{', '.join(f[0] for f in missing_constructors[:10])}"
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

def _phase_final_cleanup(
    code: Dict[str, str],
) -> Tuple[Dict[str, str], List[str]]:
    """
    Финальная чистка:
    1. Удаление пустых файлов
    2. Удаление лишних пустых строк
    3. Проверка баланса скобок
    4. Удаление Java-артефактов
    5. Фикс struct tags
    """
    fixes = []

    # ── Удаляем пустые файлы ──
    empty_files = [
        fname for fname, content in code.items()
        if fname.endswith(".go")
        and not content.strip()
        or (
            fname.endswith(".go")
            and re.fullmatch(
                r'\s*package\s+\w+\s*', content.strip()
            )
        )
    ]
    for fname in empty_files:
        del code[fname]
        fixes.append(f"[consolidate] Removed empty file: {fname}")

    # ── Чистка каждого файла ──
    for fname, content in code.items():
        if not fname.endswith(".go"):
            continue

        original = content

        # 1. Удаляем Java-аннотации, которые могли просочиться
        content = re.sub(
            r'@\w+(?:\([^)]*\))?\s*\n', '\n', content
        )

        # 2. Удаляем .class
        content = content.replace('.class', '')

        # 3. Фиксим множественные пустые строки
        content = re.sub(r'\n{4,}', '\n\n\n', content)

        # 4. Фиксим struct tags: `json:"name"` `validate:"required"`
        #    → `json:"name" validate:"required"`
        content = re.sub(
            r'`([^`]+)`\s+`([^`]+)`', r'`\1 \2`', content
        )

        # 5. Фиксим незакрытые struct tags
        lines = content.split('\n')
        fixed_lines = []
        for line in lines:
            stripped = line.rstrip()
            # Если строка содержит ровно 1 бэктик и заканчивается на "
            if stripped.count('`') == 1 and stripped.endswith('"'):
                line = stripped + '`'
            fixed_lines.append(line)
        content = '\n'.join(fixed_lines)

        # 6. Убираем Java generics, которые могли просочиться
        content = re.sub(r'\bOptional<(\w+)>', r'*\1', content)
        content = re.sub(
            r'\bList<(\w+)>',
            lambda m: '[]' + JAVA_TO_GO_TYPE.get(
                m.group(1), m.group(1)
            ),
            content,
        )
        content = re.sub(
            r'\bMap<(\w+),\s*(\w+)>',
            lambda m: (
                f'map[{JAVA_TO_GO_TYPE.get(m.group(1), m.group(1))}]'
                f'{JAVA_TO_GO_TYPE.get(m.group(2), m.group(2))}'
            ),
            content,
        )
        content = re.sub(
            r'\bSet<(\w+)>',
            lambda m: '[]' + JAVA_TO_GO_TYPE.get(
                m.group(1), m.group(1)
            ),
            content,
        )
        content = re.sub(
            r'\bResponseEntity<(\w+)>', r'\1', content
        )

        # 7. *error → error
        content = re.sub(r'\*error\b', 'error', content)

        # 8. uuid.UUID → string (убираем зависимость)
        content = re.sub(r'\buuid\.UUID\b', 'string', content)
        content = re.sub(
            r'\buuid\.New\(\)',
            '"" // TODO: generate UUID',
            content,
        )

        # 9. Баланс скобок — убираем лишние } на top level
        lines = content.split('\n')
        depth = 0
        balanced_lines = []
        for line in lines:
            s = line.strip()
            opens = s.count('{')
            closes = s.count('}')

            if s == '}' and depth == 0:
                continue  # лишняя закрывающая скобка

            depth += opens - closes
            if depth < 0:
                depth = 0
                continue

            balanced_lines.append(line)
        content = '\n'.join(balanced_lines)

        if content != original:
            code[fname] = content
            fixes.append(
                f"[consolidate] {fname}: final cleanup applied"
            )

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