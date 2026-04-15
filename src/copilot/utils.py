from __future__ import annotations

import json
import re
import logging
from typing import Dict, Any

logger = logging.getLogger(__name__)


def extract_json_files(llm_response: str) -> Dict[str, str]:
    """
    Извлекает файлы из ответа LLM.
    Поддерживает форматы:
    1. {"files": [{"name": "...", "content": "..."}]}
    2. {"filename.go": "content", ...}
    3. {"go_mod": {...}, "main_go": "...", ...}
    4. Markdown code blocks с именами файлов
    """
    if not llm_response:
        return {}

    # 1. Убираем Markdown code fences
    cleaned = re.sub(
        r'```(?:json|JSON)?\s*([\s\S]*?)\s*```',
        r'\1',
        llm_response,
    )

    # 2. Находим JSON-объект
    start_idx = cleaned.find('{')
    end_idx = cleaned.rfind('}')

    if start_idx == -1 or end_idx == -1 or start_idx >= end_idx:
        start_idx = llm_response.find('{')
        end_idx = llm_response.rfind('}')
        if start_idx == -1 or end_idx == -1 or start_idx >= end_idx:
            # Попробуем извлечь из markdown code blocks
            return _extract_from_markdown(llm_response)
        cleaned = llm_response

    json_str = cleaned[start_idx:end_idx + 1]

    # 3. Попытка 1: стандартный парсинг
    try:
        data = json.loads(json_str)
        result = _parse_files_from_data(data)
        if result:
            return result
    except json.JSONDecodeError:
        pass

    # 4. Попытка 2: исправляем бэктики
    try:
        fixed = _fix_backticks_in_json(json_str)
        data = json.loads(fixed)
        result = _parse_files_from_data(data)
        if result:
            return result
    except (json.JSONDecodeError, Exception) as e:
        logger.debug(f"Backtick fix failed: {e}")

    # 5. Попытка 3: исправляем escape
    try:
        fixed = _fix_invalid_escapes(json_str)
        data = json.loads(fixed)
        result = _parse_files_from_data(data)
        if result:
            return result
    except (json.JSONDecodeError, Exception) as e:
        logger.debug(f"Escape fix failed: {e}")

    # 6. Попытка 4: regex
    try:
        result = _extract_files_via_regex(json_str)
        if result:
            logger.info(f"Extracted {len(result)} files via regex")
            return result
    except Exception as e:
        logger.debug(f"Regex fallback failed: {e}")

    # 7. Попытка 5: извлечь из markdown code blocks
    result = _extract_from_markdown(llm_response)
    if result:
        logger.info(f"Extracted {len(result)} files from markdown")
        return result

    logger.warning(
        f"All JSON parsing attempts failed. "
        f"Response preview: {llm_response[:300]}"
    )
    return {}


def _parse_files_from_data(data: Any) -> Dict[str, str]:
    """Извлекает файлы из распарсенного JSON — поддерживает разные форматы."""
    files: Dict[str, str] = {}

    if not isinstance(data, dict):
        return files

    # Формат 1: {"files": [{"name": "...", "content": "..."}]}
    files_list = data.get("files", [])
    if isinstance(files_list, list) and files_list:
        for item in files_list:
            if isinstance(item, dict):
                name = item.get("name", "") or item.get("filename", "")
                content = item.get("content", "") or item.get("code", "")
                if name and content:
                    if not isinstance(content, str):
                        content = str(content)
                    content = _unescape_content(content)
                    files[name] = content
        if files:
            return files

    # Формат 2: {"main.go": "content", "handlers.go": "content"}
    for key, value in data.items():
        if isinstance(value, str) and key.endswith(".go"):
            files[key] = _unescape_content(value)

    if files:
        return files

    # Формат 3: {"main_go": "content"} или {"main": {"content": "..."}}
    for key, value in data.items():
        # main_go → main.go
        if key.endswith("_go"):
            filename = key.replace("_go", ".go")
            if isinstance(value, str):
                files[filename] = _unescape_content(value)
            elif isinstance(value, dict) and "content" in value:
                files[filename] = _unescape_content(value["content"])

        # Вложенные объекты с content
        elif isinstance(value, dict):
            content = value.get("content", "")
            if content and isinstance(content, str):
                # Пытаемся определить имя файла
                filename = _guess_filename(key, content)
                files[filename] = _unescape_content(content)

    if files:
        return files

    # Формат 4: {"code": {"handlers.go": "...", "main.go": "..."}}
    for key, value in data.items():
        if isinstance(value, dict):
            for inner_key, inner_value in value.items():
                if isinstance(inner_value, str) and inner_key.endswith(".go"):
                    files[inner_key] = _unescape_content(inner_value)

    return files


def _extract_from_markdown(text: str) -> Dict[str, str]:
    """
    Извлекает Go-файлы из markdown code blocks.
    Ищет паттерны:
      // filename.go
      ```go
      code
      ```
    или
      **filename.go**
      ```go
      code
      ```
    """
    files: Dict[str, str] = {}

    # Паттерн: имя файла перед code block
    pattern = (
        r'(?:'
        r'(?://\s*|#\s*|\*\*)'  # // или # или **
        r'(\S+\.go)'             # filename.go
        r'(?:\*\*)?'             # закрывающие **
        r'\s*\n'
        r')'
        r'```(?:go)?\s*\n'       # ```go
        r'([\s\S]*?)'            # code
        r'\n```'                  # ```
    )

    for match in re.finditer(pattern, text):
        name = match.group(1).strip()
        content = match.group(2).strip()
        if name and content:
            files[name] = content

    # Если не нашли с именами — берём все go code blocks
    if not files:
        go_blocks = re.findall(
            r'```go\s*\n([\s\S]*?)\n```', text
        )
        for i, block in enumerate(go_blocks):
            block = block.strip()
            if not block:
                continue
            # Пытаемся определить имя из package/содержимого
            if "func main()" in block:
                name = "main.go"
            elif "func Setup" in block or "func Register" in block:
                name = "router.go"
            elif "Handler" in block or "handler" in block:
                name = "handlers.go"
            elif "type " in block and "struct" in block:
                name = f"models_{i+1}.go"
            else:
                name = f"generated_{i+1}.go"
            files[name] = block

    return files


def _guess_filename(key: str, content: str) -> str:
    """Угадывает имя файла по ключу и содержимому."""
    # Если ключ похож на имя файла
    if "." in key:
        return key

    # По ключу
    key_lower = key.lower()
    if "main" in key_lower:
        return "main.go"
    if "handler" in key_lower or "controller" in key_lower:
        return "handlers.go"
    if "router" in key_lower or "route" in key_lower:
        return "router.go"
    if "model" in key_lower or "dto" in key_lower or "type" in key_lower:
        return "models.go"
    if "service" in key_lower:
        return "service.go"
    if "repo" in key_lower:
        return "repository.go"
    if "middleware" in key_lower:
        return "middleware.go"

    # По содержимому
    if "func main()" in content:
        return "main.go"
    if "gin.Engine" in content or "router" in content.lower():
        return "router.go"

    return f"{key}.go"


def _unescape_content(content: str) -> str:
    """Декодирует escape-последовательности."""
    if not content:
        return content
    content = content.replace('\\n', '\n')
    content = content.replace('\\t', '\t')
    content = content.replace('\\"', '"')
    content = content.replace('\\\\', '\\')
    return content


def _fix_backticks_in_json(json_str: str) -> str:
    """Заменяет бэктики на кавычки."""
    def replacer(match):
        prefix = match.group(1)
        content = match.group(2)
        content = content.replace('\\', '\\\\')
        content = content.replace('"', '\\"')
        content = content.replace('\n', '\\n')
        content = content.replace('\r', '\\r')
        content = content.replace('\t', '\\t')
        return f'{prefix}"{content}"'

    pattern = r'("(?:[^"\\]|\\.)*"\s*:\s*)`([\s\S]*?)`'
    return re.sub(pattern, replacer, json_str)


def _fix_invalid_escapes(json_str: str) -> str:
    """Исправляет невалидные escape-последовательности."""
    valid_escapes = set('"\\bfnrtu/')
    result = []
    i = 0
    in_string = False

    while i < len(json_str):
        char = json_str[i]
        if char == '"' and (i == 0 or json_str[i - 1] != '\\'):
            in_string = not in_string
            result.append(char)
        elif char == '\\' and in_string:
            if i + 1 < len(json_str):
                next_char = json_str[i + 1]
                if next_char in valid_escapes:
                    result.append(char)
                else:
                    result.append('\\\\')
            else:
                result.append('\\\\')
        else:
            result.append(char)
        i += 1

    return ''.join(result)


def _extract_files_via_regex(text: str) -> Dict[str, str]:
    """Извлекает файлы через regex."""
    files: Dict[str, str] = {}

    pattern = (
        r'"name"\s*:\s*"([^"]+\.go)"'
        r'\s*,\s*'
        r'"content"\s*:\s*"((?:[^"\\]|\\.)*)"'
    )

    for match in re.finditer(pattern, text, re.DOTALL):
        name = match.group(1)
        content = match.group(2)
        content = _unescape_content(content)
        if name and content.strip():
            files[name] = content

    return files