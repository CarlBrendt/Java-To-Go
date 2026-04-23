from __future__ import annotations

import asyncio
import logging
import os
import json
import re
import subprocess
from typing import Dict, Any, Optional, List, Set

from src.copilot.graph_state import MigrationGraphState

logger = logging.getLogger(__name__)
JAVA_PARSER_CONCURRENCY = 4


class JavaParser:
    """Wrapper around JavaParserTool to parse Java files using subprocess."""

    def __init__(self, jar_path: str):
        self.jar_path = jar_path
        if not os.path.exists(jar_path):
            raise FileNotFoundError(
                f"JAR file not found at: {jar_path}. "
                "Please run 'mvn clean package' first."
            )
        try:
            subprocess.run(
                ["java", "-version"],
                capture_output=True, check=True,
            )
        except (subprocess.CalledProcessError, FileNotFoundError):
            raise RuntimeError("Java is not installed or not in PATH.")


    async def parse_file_content(self, filepath: str) -> Optional[Dict[str, Any]]:
        """Parse a single Java file using the Java tool."""
        try:
            result = await asyncio.to_thread(
                subprocess.run,
                ["java", "-cp", self.jar_path, "JavaParserTool", filepath],
                capture_output=True,
                text=True,
                check=False,
                timeout=30,
            )
            if result.returncode != 0:
                logger.warning(
                    f"Skipping file {filepath}: {result.stderr.strip()}"
                )
                return None
            if not result.stdout.strip():
                return None
            try:
                return json.loads(result.stdout)
            except json.JSONDecodeError:
                logger.warning(f"Invalid JSON from parser for {filepath}")
                return None
        except subprocess.TimeoutExpired:
            logger.warning(f"Timeout parsing {filepath}")
            return None
        except Exception as e:
            logger.warning(f"Error parsing {filepath}: {e}")
            return None


    async def scan_directory(self, root_dir: str) -> Dict[str, Any]:
        """Scan a Java project directory and return full structure analysis."""
        structure: Dict[str, Any] = {
            "package": "",
            "controllers": [],
            "dtos": [],
            "services": [],
            "repositories": [],
            "api_contract": [],
            "dependency_graph": {},
            "exception_handlers": [],
            "feign_clients": [],
            "interfaces": [],
            "all_classes": [],
        }

        skip_dirs = {
            "__pycache__", ".git", "target", "build",
            ".idea", ".mvn", "node_modules", "test",
        }

        # Первый проход: собираем все классы
        all_parsed_classes: List[Dict[str, Any]] = []
        java_files_found = 0
        java_files_parsed = 0

        # Собираем задачи для параллельной обработки
        tasks = []
        file_paths = []
        parser_semaphore = asyncio.Semaphore(JAVA_PARSER_CONCURRENCY)

        async def parse_with_limit(filepath: str) -> Optional[Dict[str, Any]]:
            async with parser_semaphore:
                return await self.parse_file_content(filepath)
        
        for dirpath, dirnames, filenames in os.walk(root_dir):
            dirnames[:] = [d for d in dirnames if d not in skip_dirs]
            for filename in filenames:
                if not filename.endswith(".java"):
                    continue
                java_files_found += 1
                filepath = os.path.join(dirpath, filename)
                file_paths.append(filepath)
                tasks.append(parse_with_limit(filepath))
        
        # Параллельно обрабатываем все файлы
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        for filepath, file_data in zip(file_paths, results):
            if isinstance(file_data, Exception) or not file_data:
                continue
            java_files_parsed += 1

            if file_data.get("package") and not structure["package"]:
                structure["package"] = file_data["package"]

            for cls in file_data.get("classes", []):
                cls_name = cls.get("class_name", "")
                if not cls_name:
                    continue
                cls["source_file"] = os.path.relpath(filepath, root_dir)
                all_parsed_classes.append(cls)
                
        # Второй проход: строим индексы
        # Индекс: имя класса → класс
        class_index: Dict[str, Dict[str, Any]] = {}
        for cls in all_parsed_classes:
            class_index[cls.get("class_name", "")] = cls

        # Индекс: интерфейсы с HTTP-эндпоинтами
        interface_endpoints: Dict[str, List[Dict[str, Any]]] = {}
        for cls in all_parsed_classes:
            methods = cls.get("methods", [])
            http_methods = [m for m in methods if m.get("http_method")]
            if http_methods:
                interface_endpoints[cls.get("class_name", "")] = http_methods

        # Третий проход: классифицируем
        for cls in all_parsed_classes:
            cls_name = cls.get("class_name", "")
            annotations = cls.get("annotations", [])
            ann_str = " ".join(
                a if isinstance(a, str) else str(a)
                for a in annotations
            )

            is_feign = "FeignClient" in ann_str
            is_controller = (
                cls.get("is_controller", False)
                or "@RestController" in ann_str
                or ("@Controller" in ann_str and "@ControllerAdvice" not in ann_str)
            )
            is_exception_handler = (
                cls.get("is_exception_handler", False)
                or "@ControllerAdvice" in ann_str
            )
            is_service = (
                cls.get("is_service", False)
                or "@Service" in ann_str
            )
            is_repository = (
                cls.get("is_repository", False)
                or "@Repository" in ann_str
            )

            # ── FeignClient = внешний клиент, НЕ наш API ──
            if is_feign:
                structure["feign_clients"].append(cls)
                continue  # Не добавляем в контроллеры!

            # ── Exception Handler ──
            if is_exception_handler:
                structure["exception_handlers"].append(cls)
                for method in cls.get("methods", []):
                    structure["api_contract"].append({
                        "path": "/error",
                        "method": "EXCEPTION_HANDLER",
                        "handler_name": method.get("name", ""),
                        "request_type": "",
                        "response_type": method.get("return_type", ""),
                        "path_params": [],
                        "query_params": [],
                        "summary": f"Exception handler: {method.get('name', '')}",
                        "exception_types": method.get("exception_types", []),
                        "is_exception_handler": True,
                        "class_name": cls_name,
                    })
                continue

            # ── Controller (RestController) ──
            if is_controller:
                structure["controllers"].append(cls)
                base_path = await self._extract_base_path(cls)

                # Собираем эндпоинты:
                # 1. Из самого класса
                # 2. Из интерфейсов, которые он реализует
                all_endpoints = []

                # Эндпоинты из самого класса
                for method in cls.get("methods", []):
                    if method.get("http_method"):
                        all_endpoints.append(method)

                # Если у класса нет своих эндпоинтов — ищем в интерфейсах
                if not all_endpoints:
                    # Ищем реализуемые интерфейсы
                    implements = cls.get("implements", [])
                    # Также проверяем по имени: FtpExecutionApiImpl → FtpExecutionApi
                    if cls_name.endswith("Impl"):
                        interface_name = cls_name[:-4]  # убираем "Impl"
                        if interface_name not in implements:
                            implements.append(interface_name)

                    for iface_name in implements:
                        if iface_name in interface_endpoints:
                            all_endpoints.extend(
                                interface_endpoints[iface_name]
                            )
                            logger.info(
                                f"  Inherited {len(interface_endpoints[iface_name])} "
                                f"endpoints from {iface_name} → {cls_name}"
                            )

                # Также проверяем base_path из интерфейса
                if not base_path:
                    for iface_name in cls.get("implements", []):
                        if iface_name in class_index:
                            iface_base = await self._extract_base_path(
                                class_index[iface_name]
                            )
                            if iface_base:
                                base_path = iface_base
                                break
                    # Проверяем по имени Impl → Interface
                    if not base_path and cls_name.endswith("Impl"):
                        iface_name = cls_name[:-4]
                        if iface_name in class_index:
                            base_path = await self._extract_base_path(
                                class_index[iface_name]
                            )

                for method in all_endpoints:
                    method_path = method.get("path")
                    http_method = method.get("http_method")
                    if http_method and method_path is not None:
                        full_path = await self._join_paths(base_path, method_path)
                        structure["api_contract"].append({
                            "path": full_path,
                            "method": http_method,
                            "handler_name": method.get("name", ""),
                            "request_type": method.get("request_body_type", ""),
                            "response_type": method.get("return_type", ""),
                            "path_params": method.get("path_params", []),
                            "query_params": method.get("query_params", []),
                            "summary": f"{method.get('name', '')} endpoint",
                            "is_exception_handler": False,
                            "class_name": cls_name,
                        })

                continue

            # ── Service ──
            if is_service:
                structure["services"].append(cls)

            # ── Repository ──
            if is_repository:
                structure["repositories"].append(cls)

            # ── DTO ──
            if self._is_dto_class(cls):
                existing = {d.get("class_name") for d in structure["dtos"]}
                if cls_name not in existing:
                    structure["dtos"].append(cls)

            # ── Dependency graph ──
            # @RequiredArgsConstructor = все final поля инжектятся
            # ── Dependency graph ──
            injected = []

            has_required_args = any(
                "RequiredArgsConstructor" in (
                    a if isinstance(a, str) else str(a)
                )
                for a in annotations
            )

            has_all_args = any(
                "AllArgsConstructor" in (
                    a if isinstance(a, str) else str(a)
                )
                for a in annotations
            )

            for field in cls.get("fields", []):
                field_type = field.get("type", "")
                field_name = field.get("name", "")
                is_autowired = field.get("is_autowired", False)
                is_final = field.get("is_final", False)
                is_static = field.get("is_static", False)

                # Пропускаем статические поля
                if is_static:
                    continue

                # Пропускаем примитивы, строки, логгеры
                skip_types = {
                    "String", "int", "long", "boolean", "double", "float",
                    "Integer", "Long", "Boolean", "Double", "Float",
                    "byte", "short", "char", "Byte", "Short", "Character",
                    "Logger", "Log", "ObjectMapper",
                }
                if field_type in skip_types:
                    continue

                # Пропускаем поля без типа
                if not field_type:
                    continue

                should_inject = False

                # Случай 1: явный @Autowired или @Inject
                if is_autowired:
                    should_inject = True

                # Случай 2: @RequiredArgsConstructor + final поле
                elif has_required_args and is_final:
                    should_inject = True

                # Случай 3: @RequiredArgsConstructor, парсер не даёт is_final
                # Эвристика: если тип начинается с заглавной буквы
                # и это не примитив — скорее всего это инъекция
                elif has_required_args and field_type[0].isupper():
                    # Дополнительная проверка: тип похож на сервис/репозиторий/клиент
                    service_patterns = (
                        "Service", "Repository", "Client", "Mapper",
                        "Configuration", "Properties", "Provider",
                        "Factory", "Handler", "Resolver", "Storage",
                        "Helper", "Validator", "Compiler",
                        "Internationalizer", "BlobStorage",
                    )
                    if any(
                        pattern in field_type
                        for pattern in service_patterns
                    ):
                        should_inject = True
                    # Или если тип есть в наших классах
                    elif field_type in class_index:
                        should_inject = True

                # Случай 4: @AllArgsConstructor
                elif has_all_args and field_type[0].isupper():
                    if field_type in class_index:
                        should_inject = True

                if should_inject:
                    # Проверяем дубли
                    if not any(
                        i.get("type") == field_type for i in injected
                    ):
                        injected.append({
                            "field_name": field_name,
                            "type": field_type,
                        })

            # Также из injected_dependencies (если парсер вернул)
            for dep in cls.get("injected_dependencies", []):
                if isinstance(dep, dict):
                    dep_type = dep.get("type", "")
                    if dep_type and not any(
                        i.get("type") == dep_type for i in injected
                    ):
                        injected.append(dep)

            if injected:
                structure["dependency_graph"][cls_name] = injected

        structure["all_classes"] = all_parsed_classes

        logger.info(
            f"Scanned {java_files_found} Java files, "
            f"parsed {java_files_parsed}"
        )

        return structure

    @staticmethod
    async def _extract_base_path(cls: Dict[str, Any]) -> str:
        """Extract base path from @RequestMapping on the class level."""
        annotations = cls.get("annotations", [])
        for ann in annotations:
            ann_str = ann if isinstance(ann, str) else str(ann)
            if "RequestMapping" in ann_str:
                match = re.search(
                    r'RequestMapping\s*\(\s*(?:value\s*=\s*)?'
                    r'["\']([^"\']+)["\']',
                    ann_str,
                )
                if match:
                    return match.group(1)
            # Также проверяем @Tag — иногда путь в описании
        return ""

    @staticmethod
    async def _join_paths(base: str, path: str) -> str:
        base = base.rstrip("/") if base else ""
        if path and not path.startswith("/"):
            path = f"/{path}"
        elif not path:
            path = ""
        combined = f"{base}{path}"
        return combined if combined else "/"

    @staticmethod
    async def _is_dto_class(cls: Dict[str, Any]) -> bool:
        name = cls.get("class_name", "")
        dto_patterns = (
            "DTO", "Dto", "Request", "Response", "Config",
            "Model", "Entity", "Payload", "Form", "View",
            "Record", "Vo", "VO", "Req", "Res", "Result",
        )
        if any(name.endswith(suffix) or name.startswith(suffix)
               for suffix in dto_patterns):
            return True

        annotations = cls.get("annotations", [])
        dto_annotations = {"@Entity", "@Data", "@Value"}
        for a in annotations:
            a_str = a if isinstance(a, str) else str(a)
            if any(da in a_str for da in dto_annotations):
                return True

        if cls.get("is_record", False):
            return True

        return False


# ------------------------------------------------------------------
# LangGraph node
# ------------------------------------------------------------------
async def node_parse_java(state: MigrationGraphState) -> dict:
    """Stage 1: Deep Analysis & Contract Extraction."""
    project_path = state.get("java_project_path", "")

    jar_path = state.get("jar_path", "")
    if not jar_path:
        project_root = os.path.dirname(
            os.path.dirname(
                os.path.dirname(os.path.dirname(__file__))
            )
        )
        jar_name = "javaparser-tools-1.0-SNAPSHOT.jar"
        candidates = [
            os.path.join(project_root, "java-tools", "target", jar_name),
            f"/Users/pmedvedeva/Desktop/Projects/task-repo/java-tools/target/{jar_name}",
            os.path.join(os.getcwd(), "java-tools", "target", jar_name),
        ]
        for candidate in candidates:
            if os.path.exists(candidate):
                jar_path = candidate
                break

    logger.info(f"JavaParser JAR path: {jar_path}")

    error_state = {
        "java_structure": {
            "package": "",
            "controllers": [],
            "dtos": [],
            "services": [],
            "repositories": [],
            "exception_handlers": [],
            "feign_clients": [],
            "all_classes": [],
        },
        "api_contract": [],
        "dependency_graph": {},
        "exception_handlers": [],
    }

    if not project_path or not os.path.isdir(project_path):
        logger.error(f"Invalid Java project path: {project_path}")
        if project_path:
            abs_path = os.path.abspath(project_path)
            parent = os.path.dirname(abs_path)
            if os.path.isdir(parent):
                logger.error(
                    f"Resolved to: {abs_path}\n"
                    f"Contents of {parent}: "
                    f"{os.listdir(parent)[:15]}"
                )
        return {
            **error_state,
            "status": "error_invalid_path",
            "current_node": "parse",
        }

    if not jar_path or not os.path.exists(jar_path):
        logger.error(f"JAR file not found at: {jar_path}")
        return {
            **error_state,
            "status": "error_jar_not_found",
            "current_node": "parse",
        }

    try:
        parser = JavaParser(jar_path)
        raw_structure = await parser.scan_directory(project_path)

        # Подсчёт реальных API-эндпоинтов (без exception handlers и feign)
        real_endpoints = [
            e for e in raw_structure.get("api_contract", [])
            if not e.get("is_exception_handler")
        ]

        logger.info(
            f"Analysis complete:\n"
            f"  Controllers:        {len(raw_structure['controllers'])}\n"
            f"  Services:           {len(raw_structure['services'])}\n"
            f"  DTOs:               {len(raw_structure['dtos'])}\n"
            f"  Repositories:       {len(raw_structure['repositories'])}\n"
            f"  Exception Handlers: {len(raw_structure['exception_handlers'])}\n"
            f"  Feign Clients:      {len(raw_structure.get('feign_clients', []))}\n"
            f"  API Endpoints:      {len(real_endpoints)}\n"
            f"  Dependency Graph:   {len(raw_structure['dependency_graph'])} entries"
        )

        # Логируем найденные эндпоинты
        for ep in real_endpoints:
            logger.info(
                f"  Endpoint: {ep.get('method', '?'):6s} "
                f"{ep.get('path', '?'):35s} "
                f"→ {ep.get('handler_name', '?')} "
                f"({ep.get('class_name', '?')})"
            )

        # Логируем dependency graph
        for cls_name, deps in raw_structure.get("dependency_graph", {}).items():
            dep_types = [d.get("type", "?") for d in deps]
            logger.info(f"  DI: {cls_name} → {', '.join(dep_types)}")

        return {
            "java_structure": raw_structure,
            "api_contract": raw_structure.get("api_contract", []),
            "dependency_graph": raw_structure.get("dependency_graph", {}),
            "package": raw_structure.get("package", ""),
            "exception_handlers": raw_structure.get(
                "exception_handlers", []
            ),
            "status": "parsed",
            "current_node": "parse",
        }
    except Exception as e:
        logger.exception(f"Error during Java parsing: {e}")
        return {
            **error_state,
            "status": f"error_parsing: {e}",
            "current_node": "parse",
        }
