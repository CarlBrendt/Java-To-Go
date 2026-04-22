# src/copilot/nodes/linter_node.py
from __future__ import annotations

import asyncio
import json
import logging
import os
import re
import shutil
import subprocess
import tempfile
from typing import Dict, List, Tuple

from src.copilot.graph_state import MigrationGraphState

logger = logging.getLogger(__name__)


async def node_lint_fix(state: MigrationGraphState) -> dict:
    """
    Запускает pre-lint + golangci-lint для автоматического исправления кода.
    """
    generated_code = dict(state.get("generated_go_code", {}))
    
    if not generated_code:
        return {
            "generated_go_code": generated_code,
            "lint_fixes_applied": [],
            "status": "lint_skipped",
            "current_node": "lint",
        }
    
    all_fixes = []
    
    # ═══════════════════════════════════════════════════════════
    # PHASE 0: PRE-LINT — исправляем ошибки ДО линтера
    # ═══════════════════════════════════════════════════════════
    
    # 0. СИНТАКСИЧЕСКИЕ ОШИБКИ (должны быть исправлены ДО линтера)
    generated_code, fixes = _fix_syntax_errors(generated_code)
    all_fixes.extend(fixes)
    
    # 1. Исправляем malformed types (type *Foo struct → type Foo struct)
    generated_code, fixes = _fix_malformed_types(generated_code)
    all_fixes.extend(fixes)
    
    # 2. Исправляем Java массивы Object[]
    generated_code, fixes = _fix_java_arrays(generated_code)
    all_fixes.extend(fixes)
    
    # 3. Исправляем missing import paths
    generated_code, fixes = _fix_missing_import_paths(generated_code)
    all_fixes.extend(fixes)
    
    # 4. Генерируем недостающие базовые типы
    generated_code, fixes = _generate_missing_base_types(generated_code)
    all_fixes.extend(fixes)
    
    # 5. Добавляем недостающие импорты
    generated_code, fixes = _add_missing_imports(generated_code)
    all_fixes.extend(fixes)
    
    # 6. Исправляем Java-specific типы
    generated_code, fixes = _fix_java_types(generated_code)
    all_fixes.extend(fixes)
    
    # 7. Генерируем Optional заглушки
    generated_code, fixes = _generate_optional_stubs(generated_code)
    all_fixes.extend(fixes)
    
    # 8. Исправляем рекурсивные типы
    generated_code, fixes = _fix_recursive_types(generated_code)
    all_fixes.extend(fixes)
    
    if all_fixes:
        logger.info(f"Pre-lint applied {len(all_fixes)} fixes")
        for fix in all_fixes[:10]:
            logger.info(f"  - {fix}")
    
    # ═══════════════════════════════════════════════════════════
    # PHASE 1: Запускаем golangci-lint
    # ═══════════════════════════════════════════════════════════
    
    # Проверяем наличие golangci-lint
    has_lint = await _check_golangci_lint()
    if not has_lint:
        logger.error("golangci-lint not found")
        return {
            "generated_go_code": generated_code,
            "lint_fixes_applied": all_fixes,
            "status": "lint_failed_no_tool",
            "current_node": "lint",
        }
    
    lint_fixes = []
    
    # Работаем во временной директории
    with tempfile.TemporaryDirectory() as tmpdir:
        # 1. Создаём go.mod
        go_mod_content = _generate_go_mod_for_lint(generated_code)
        with open(os.path.join(tmpdir, "go.mod"), "w") as f:
            f.write(go_mod_content)
        
        # 2. Копируем .golangci.yml если есть
        config_path = _find_golangci_config()
        if config_path:
            shutil.copy(config_path, os.path.join(tmpdir, ".golangci.yml"))
            logger.info(f"Using config: {config_path}")
        
        # 3. Сохраняем все .go файлы
        saved_files = []
        for filename, content in generated_code.items():
            if not filename.endswith(".go"):
                continue
            file_path = os.path.join(tmpdir, filename)
            os.makedirs(os.path.dirname(file_path) if os.path.dirname(file_path) else tmpdir, exist_ok=True)
            with open(file_path, "w") as f:
                f.write(content)
            saved_files.append(filename)
        
        logger.info(f"Linting {len(saved_files)} files...")
        
        # 4. Запускаем golangci-lint с авто-исправлением
        cmd = [
            "golangci-lint", "run",
            "--fix",
            "--fast",
            "--timeout=5m",
            "--out-format=colored-line-number",
            "./...",
        ]
        
        try:
            result = await asyncio.to_thread(
                subprocess.run,
                cmd,
                capture_output=True,
                text=True,
                cwd=tmpdir,
                timeout=300,
            )
            
            # Логируем вывод
            if result.stdout:
                for line in result.stdout.split('\n')[:20]:
                    if line.strip():
                        logger.info(f"  {line}")
            
            if result.stderr:
                logger.warning(f"Lint stderr: {result.stderr[:500]}")
            
            # 5. Читаем исправленные файлы
            for filename in saved_files:
                file_path = os.path.join(tmpdir, filename)
                if os.path.exists(file_path):
                    with open(file_path, "r") as f:
                        new_content = f.read()
                    if new_content != generated_code[filename]:
                        generated_code[filename] = new_content
                        lint_fixes.append(filename)
            
            if lint_fixes:
                logger.info(f"✅ Lint fixed {len(lint_fixes)} files: {', '.join(lint_fixes)}")
            else:
                logger.info("✅ Lint found no issues to fix")
            
        except subprocess.TimeoutExpired:
            logger.error("❌ Lint timeout after 5 minutes")
        except Exception as e:
            logger.error(f"❌ Lint error: {e}")
    
    all_fixes.extend([f"lint:{f}" for f in lint_fixes])
    
    return {
        "generated_go_code": generated_code,
        "lint_fixes_applied": all_fixes,
        "status": "lint_complete" if lint_fixes else "lint_clean",
        "current_node": "lint",
    }

# src/copilot/nodes/linter_node.py

def _fix_unclosed_braces(content: str) -> str:
    """Более агрессивное исправление незакрытых скобок."""
    lines = content.splitlines()
    fixed_lines = []
    stack = []

    for line in lines:
        cleaned_line = line
        i = 0
        while i < len(cleaned_line):
            if cleaned_line[i] == '{':
                stack.append('{')
            elif cleaned_line[i] == '}':
                if stack:
                    stack.pop()
                else:
                    # Лишняя закрывающая — удаляем
                    cleaned_line = cleaned_line[:i] + cleaned_line[i+1:]
                    continue
            i += 1
        fixed_lines.append(cleaned_line)

    # Добавляем недостающие }
    final_content = '\n'.join(fixed_lines)
    if stack:
        final_content += '\n' + '}' * len(stack)

    return final_content

async def node_lint_only_check(state: MigrationGraphState) -> dict:
    """
    Только проверяет код без исправлений (для отчёта).
    """
    generated_code = state.get("generated_go_code", {})
    
    if not generated_code:
        return {
            "lint_issues": [],
            "status": "lint_check_skipped",
            "current_node": "lint_check",
        }
    
    has_lint = await _check_golangci_lint()
    if not has_lint:
        return {
            "lint_issues": [{"error": "golangci-lint not installed"}],
            "status": "lint_check_failed",
            "current_node": "lint_check",
        }
    
    issues = []
    
    with tempfile.TemporaryDirectory() as tmpdir:
        # Создаём go.mod
        go_mod_content = _generate_go_mod_for_lint(generated_code)
        with open(os.path.join(tmpdir, "go.mod"), "w") as f:
            f.write(go_mod_content)
        
        # Сохраняем файлы
        for filename, content in generated_code.items():
            if not filename.endswith(".go"):
                continue
            file_path = os.path.join(tmpdir, filename)
            os.makedirs(os.path.dirname(file_path) if os.path.dirname(file_path) else tmpdir, exist_ok=True)
            with open(file_path, "w") as f:
                f.write(content)
        
        # Запускаем проверку в JSON формате
        cmd = [
            "golangci-lint", "run",
            "--fast",
            "--timeout=5m",
            "--out-format=json",
            "./...",
        ]
        
        try:
            result = await asyncio.to_thread(
                subprocess.run,
                cmd,
                capture_output=True,
                text=True,
                cwd=tmpdir,
                timeout=300,
            )
            
            if result.stdout:
                try:
                    data = json.loads(result.stdout)
                    for issue in data.get("Issues", []):
                        issues.append({
                            "file": issue.get("Pos", {}).get("Filename", "unknown"),
                            "line": issue.get("Pos", {}).get("Line", 0),
                            "column": issue.get("Pos", {}).get("Column", 0),
                            "severity": issue.get("Severity", "warning"),
                            "message": issue.get("Text", ""),
                            "linter": issue.get("FromLinter", ""),
                        })
                except json.JSONDecodeError:
                    # Fallback: парсим текстовый вывод
                    for line in result.stdout.split('\n'):
                        if ":" in line and (".go" in line):
                            issues.append({"message": line.strip()})
            
            logger.info(f"Lint check found {len(issues)} issues")
            
        except Exception as e:
            logger.error(f"Lint check error: {e}")
    
    return {
        "lint_issues": issues,
        "status": "lint_check_complete",
        "current_node": "lint_check",
    }


def _fix_syntax_errors(code: Dict[str, str]) -> Tuple[Dict[str, str], List[str]]:
    """Исправляет синтаксические ошибки ДО запуска линтера."""
    fixes = []
    
    for filename, content in code.items():
        if not filename.endswith(".go"):
            continue
        
        original = content

        content = _fix_unclosed_braces(content)
        
        # 1. type *Name struct → type Name struct
        content = re.sub(r'type\s+\*\s*(\w+)\s+struct', r'type \1 struct', content)
        
        # 2. type *Name interface → type Name interface
        content = re.sub(r'type\s+\*\s*(\w+)\s+interface', r'type \1 interface', content)
        
        # 3. type *Name = ... → type Name = ...
        content = re.sub(r'type\s+\*\s*(\w+)\s*=', r'type \1 =', content)
        
        # 4. **DoublePointer → *DoublePointer (в полях)
        content = re.sub(r'(\s+\w+)\s+\*\*(\w+)', r'\1 *\2', content)
        
        # 5. Просто **X → *X везде
        content = re.sub(r'\*\*(\w+)', r'*\1', content)
        
        # 6. Незакрытые скобки в struct (если есть лишняя })
        lines = content.split('\n')
        depth = 0
        fixed_lines = []
        skip_until_reset = False
        
        for line in lines:
            if skip_until_reset:
                if '}' in line:
                    skip_until_reset = False
                continue
            
            # Считаем скобки
            depth += line.count('{') - line.count('}')
            
            # Если глубина отрицательная - лишняя закрывающая скобка
            if depth < 0:
                # Убираем лишнюю закрывающую скобку из строки
                line = re.sub(r'^\s*}\s*', '', line)
                depth = 0
                if not line.strip():
                    continue
            
            # Если строка только с } и мы не на top level
            if line.strip() == '}' and depth == 0:
                continue
                
            fixed_lines.append(line)
        
        content = '\n'.join(fixed_lines)
        
        # 7. Удаляем пустые import blocks
        content = re.sub(r'import\s*\(\s*\)', '', content)
        
        # 8. Исправляем import без пути
        content = re.sub(r'import\s+"\s*"', '', content)
        
        if content != original:
            code[filename] = content
            fixes.append(f"{filename}: fixed syntax errors")
    
    return code, fixes


def _fix_malformed_types(code: Dict[str, str]) -> Tuple[Dict[str, str], List[str]]:
    """Исправляет malformed type declarations."""
    fixes = []
    
    for filename, content in code.items():
        if not filename.endswith(".go"):
            continue
        
        original = content
        
        # Исправляем type *Name struct → type Name struct
        content = re.sub(r'type\s+\*\s*(\w+)\s+struct', r'type \1 struct', content)
        
        # Исправляем type *Name interface → type Name interface
        content = re.sub(r'type\s+\*\s*(\w+)\s+interface', r'type \1 interface', content)
        
        # Исправляем field **Name → *Name
        content = re.sub(r'(\s+\w+)\s+\*\*(\w+)', r'\1 *\2', content)
        
        # Исправляем *Name **Name → *Name *Name
        content = re.sub(r'\*\*(\w+)(?=\s|$|`|,|})', r'*\1', content)
        
        if content != original:
            code[filename] = content
            fixes.append(f"{filename}: fixed malformed type declarations")
    
    return code, fixes


def _fix_java_arrays(code: Dict[str, str]) -> Tuple[Dict[str, str], List[str]]:
    """Исправляет Java массивы Object[] на Go слайсы."""
    fixes = []
    
    for filename, content in code.items():
        if not filename.endswith(".go"):
            continue
        
        original = content
        
        # Object[] → []interface{}
        content = re.sub(r'\bObject\s*\[\s*\]', '[]interface{}', content)
        # MessageArgs Object[] → MessageArgs []interface{}
        content = re.sub(r'(\w+)\s+Object\s*\[\s*\]', r'\1 []interface{}', content)
        # : Object[] → : []interface{}
        content = re.sub(r':\s*Object\s*\[\s*\]', ': []interface{}', content)
        # , Object[] → , []interface{}
        content = re.sub(r',\s*Object\s*\[\s*\]', ', []interface{}', content)
        
        if content != original:
            code[filename] = content
            fixes.append(f"{filename}: fixed Object[] → []interface{{}}")
    
    return code, fixes


def _fix_missing_import_paths(code: Dict[str, str]) -> Tuple[Dict[str, str], List[str]]:
    """Исправляет неправильные import statements."""
    fixes = []
    
    for filename, content in code.items():
        if not filename.endswith(".go"):
            continue
        
        original = content
        
        # Удаляем пустые import statements
        content = re.sub(r'import\s*\(\s*\)', '', content)
        
        # Исправляем import без пути
        content = re.sub(r'import\s+"\s*"', '', content)
        
        if content != original:
            code[filename] = content
            fixes.append(f"{filename}: fixed missing import paths")
    
    return code, fixes


def _generate_missing_base_types(code: Dict[str, str]) -> Tuple[Dict[str, str], List[str]]:
    """Генерирует недостающие базовые типы."""
    fixes = []
    all_content = "\n".join(code.values())
    
    missing_types = []
    
    type_patterns = [
        (r'\bWorkflowRef\b', 'type WorkflowRef = string'),
        (r'\bWorkflowStartConfig\b', 'type WorkflowStartConfig = map[string]interface{}'),
        (r'\bBusinessKey\b', 'type BusinessKey = string'),
        (r'\bTaskTimeout\b', 'type TaskTimeout = int64'),
        (r'\bRunTimeout\b', 'type RunTimeout = int64'),
        (r'\bExecutionTimeout\b', 'type ExecutionTimeout = int64'),
        (r'\bId\b(?!entifier)', 'type Id = string'),
        (r'\bcipher\.Cipher\b', 'type Cipher struct{}'),
        (r'\bClientError\b', 'type ClientError = string'),
        (r'\bApplicationInstance\b', 'type ApplicationInstance struct{}'),
        (r'\bConstraintViolation\b', 'type ConstraintViolation struct{}'),
        (r'\bResErrorDescription\b', 'type ResErrorDescription struct{}'),
        (r'\bProperty\b', 'type Property struct{}'),
        (r'\bErrorDescription\b', 'type ErrorDescription struct{}'),
        (r'\bErrors2\b', 'type Errors2 struct{}'),
    ]
    
    for pattern, type_def in type_patterns:
        if re.search(pattern, all_content):
            type_name = type_def.split()[1] if 'type' in type_def else None
            if type_name:
                if not re.search(rf'type\s+{re.escape(type_name)}\b', all_content):
                    missing_types.append(type_def)
    
    if missing_types:
        types_file = code.get("types_common.go", "package main\n\n")
        
        if "package main" not in types_file:
            types_file = "package main\n\n" + types_file
        
        types_file += "\n// Auto-generated missing types\n"
        for type_def in missing_types:
            types_file += type_def + "\n"
        
        code["types_common.go"] = types_file
        fixes.append(f"Generated {len(missing_types)} missing type aliases")
    
    return code, fixes


def _add_missing_imports(code: Dict[str, str]) -> Tuple[Dict[str, str], List[str]]:
    """Добавляет недостающие импорты в файлы."""
    fixes = []
    
    import_map = {
        r'\btime\.': 'import "time"',
        r'\bcipher\.': 'import "crypto/cipher"',
        r'\bsql\.': 'import "database/sql"',
        r'\bjson\.': 'import "encoding/json"',
        r'\bhttp\.': 'import "net/http"',
        r'\bgin\.': 'import "github.com/gin-gonic/gin"',
        r'\bgorm\.': 'import "gorm.io/gorm"',
        r'\bfmt\.': 'import "fmt"',
        r'\berrors\.': 'import "errors"',
        r'\bcontext\.': 'import "context"',
    }
    
    for filename, content in code.items():
        if not filename.endswith(".go"):
            continue
        
        original = content
        needed_imports = []
        
        for pattern, imp in import_map.items():
            if re.search(pattern, content):
                imp_path = imp.split('"')[1]
                if f'"{imp_path}"' not in content:
                    needed_imports.append(imp)
        
        if needed_imports:
            new_content = _add_imports_to_file(content, needed_imports)
            if new_content != content:
                code[filename] = new_content
                fixes.append(f"{filename}: added {len(needed_imports)} imports")
    
    return code, fixes


def _add_imports_to_file(content: str, imports: List[str]) -> str:
    """Добавляет импорты в Go файл."""
    # Ищем существующий import block
    import_block = re.search(r'import\s*\(\s*\n([\s\S]*?)\n\s*\)', content)
    
    if import_block:
        existing_imports = import_block.group(1)
        new_imports = existing_imports
        for imp in imports:
            imp_path = imp.split('"')[1]
            if f'"{imp_path}"' not in existing_imports:
                new_imports += f'\n\t{imp}'
        
        new_block = f'import (\n{new_imports}\n)'
        content = content[:import_block.start()] + new_block + content[import_block.end():]
    else:
        pkg_match = re.search(r'^package\s+\w+', content, re.MULTILINE)
        if pkg_match:
            import_section = '\n\nimport (\n'
            for imp in imports:
                import_section += f'\t{imp}\n'
            import_section += ')\n'
            content = content[:pkg_match.end()] + import_section + content[pkg_match.end():]
    
    return content


def _fix_java_types(code: Dict[str, str]) -> Tuple[Dict[str, str], List[str]]:
    """Исправляет Java-specific типы на Go."""
    fixes = []
    
    for filename, content in code.items():
        if not filename.endswith(".go"):
            continue
        
        original = content
        
        # Optional<T> → *T
        content = re.sub(r'\bOptional<([^>]+)>', r'*\1', content)
        
        # ResponseEntity<T> → T
        content = re.sub(r'\bResponseEntity<([^>]+)>', r'\1', content)
        
        # List<T> → []T
        content = re.sub(r'\bList<([^>]+)>', r'[]\1', content)
        
        # Map<K,V> → map[K]V
        content = re.sub(r'\bMap<([^,]+),\s*([^>]+)>', r'map[\1]\2', content)
        
        # Object → interface{}
        content = re.sub(r'\bObject\b(?!\.)', 'interface{}', content)
        
        # String → string
        content = re.sub(r'\bString\b(?!\.)', 'string', content)
        
        # Integer → int
        content = re.sub(r'\bInteger\b(?!\.)', 'int', content)
        
        # Long → int64
        content = re.sub(r'\bLong\b(?!\.)', 'int64', content)
        
        if content != original:
            code[filename] = content
            fixes.append(f"{filename}: fixed Java types")
    
    return code, fixes


def _generate_optional_stubs(code: Dict[str, str]) -> Tuple[Dict[str, str], List[str]]:
    """Генерирует заглушки для Optional типов."""
    fixes = []
    all_content = "\n".join(code.values())
    
    if 'Optional[' in all_content and 'type Optional' not in all_content:
        optional_stub = '''
// Optional is a stub for Java Optional<T>
// TODO: Replace with proper implementation or remove
type Optional[T any] struct {
    value   T
    present bool
}

func (o Optional[T]) IsPresent() bool {
    return o.present
}

func (o Optional[T]) Get() T {
    return o.value
}

func (o Optional[T]) OrElse(other T) T {
    if o.present {
        return o.value
    }
    return other
}

func (o Optional[T]) OrElseGet(f func() T) T {
    if o.present {
        return o.value
    }
    return f()
}
'''
        types_file = code.get("types_common.go", "package main\n\n")
        if "type Optional" not in types_file:
            types_file += optional_stub
            code["types_common.go"] = types_file
            fixes.append("Generated Optional[T] generic stub")
    
    return code, fixes


def _fix_recursive_types(code: Dict[str, str]) -> Tuple[Dict[str, str], List[str]]:
    """Исправляет рекурсивные типы (type A struct { A })"""
    fixes = []
    
    for filename, content in code.items():
        if not filename.endswith(".go"):
            continue
        
        original = content
        
        # Находим struct с полем того же типа
        struct_pattern = r'type\s+(\w+)\s+struct\s*\{([^}]*)\}'
        for match in re.finditer(struct_pattern, content, re.DOTALL):
            type_name = match.group(1)
            struct_body = match.group(2)
            
            # Проверяем есть ли поле с таким же типом
            if re.search(rf'\b{type_name}\b', struct_body):
                # Заменяем на указатель
                content = re.sub(
                    rf'(\b{type_name}\b)(?!\s*\{{)',
                    rf'*\1',
                    content
                )
                fixes.append(f"{filename}: fixed recursive type {type_name}")
                break
        
        if content != original:
            code[filename] = content
    
    return code, fixes


# ═══════════════════════════════════════════════════════════
# UTILITY FUNCTIONS
# ═══════════════════════════════════════════════════════════

async def _check_golangci_lint() -> bool:
    """Проверяет наличие golangci-lint."""
    try:
        result = await asyncio.to_thread(
            subprocess.run,
            ["golangci-lint", "--version"],
            capture_output=True,
            text=True,
        )
        version = result.stdout.strip().split('\n')[0]
        logger.info(f"✅ golangci-lint available: {version}")
        return result.returncode == 0
    except FileNotFoundError:
        logger.error("❌ golangci-lint not found in PATH")
        return False


def _generate_go_mod_for_lint(code_files: Dict[str, str]) -> str:
    """Генерирует минимальный go.mod для линтера."""
    all_content = "\n".join(code_files.values())
    
    deps = ['\tgithub.com/gin-gonic/gin v1.9.1']
    
    if "gorm.io" in all_content:
        deps.append('\tgorm.io/gorm v1.25.7')
        deps.append('\tgorm.io/driver/postgres v1.5.7')
    if "github.com/jlaffaye/ftp" in all_content:
        deps.append('\tgithub.com/jlaffaye/ftp v0.2.0')
    if "github.com/pkg/sftp" in all_content:
        deps.append('\tgithub.com/pkg/sftp v1.13.6')
    if "go-playground/validator" in all_content:
        deps.append('\tgithub.com/go-playground/validator/v10 v10.19.0')
    if "rs/zerolog" in all_content:
        deps.append('\tgithub.com/rs/zerolog v1.32.0')
    
    deps_str = "\n".join(deps) if deps else ""
    
    return f"""module migration-lint

go 1.22

require (
{deps_str}
)
"""


def _find_golangci_config() -> str:
    """Ищет .golangci.yml в проекте."""
    possible_paths = [
        "/app/golangci.yml",
        "/app/.golangci.yml",
        "golangci.yml",
        ".golangci.yml",
        "../golangci.yml",
    ]
    
    for path in possible_paths:
        if os.path.exists(path):
            return path
    
    logger.warning("No .golangci.yml found, using defaults")
    return None