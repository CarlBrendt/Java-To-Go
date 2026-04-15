from __future__ import annotations

import json
import logging
import re

from langchain_core.messages import HumanMessage

from src.copilot.graph_state import MigrationGraphState
from src.services.mws_llm_factory import build_mws_chat_llm
from src.settings.config import APISettings

logger = logging.getLogger(__name__)


async def node_generate_api_layer(state: MigrationGraphState) -> dict:
    """Stage 5: API Layer & Routing.

    Гибридный подход:
    1. router.go и main.go генерируются ДЕТЕРМИНИСТИЧЕСКИ (шаблон)
    2. handlers.go генерируется LLM (тела функций)
    3. Если LLM не справился — fallback с TODO-заглушками
    """
    contract = state.get("api_contract", [])
    models_code = state.get("generated_models_code", "")
    service_code = state.get("generated_service_code", "")
    exception_handlers = state.get("exception_handlers", [])

    api_endpoints = [
        e for e in contract if not e.get("is_exception_handler")
    ]

    if not api_endpoints:
        logger.warning("No API endpoints available")
        return {
            "generated_handlers_code": "",
            "generated_go_code": state.get("generated_go_code", {}),
            "status": "api_layer_skipped_no_endpoints",
            "current_node": "api_layer",
        }

    existing_code = dict(state.get("generated_go_code", {}))

    # ── 1. Генерируем router.go детерминистически ──
    router_code = _generate_router(api_endpoints)
    existing_code["router.go"] = router_code

    # ── 2. Генерируем main.go детерминистически ──
    main_code = _generate_main()
    existing_code["main.go"] = main_code

    # ── 3. Генерируем handlers.go через LLM ──
    settings = APISettings()
    llm = build_mws_chat_llm(settings)

    handlers_code = await _generate_handlers_via_llm(
        llm, api_endpoints, models_code, service_code,
        exception_handlers,
    )

    existing_code["handlers.go"] = handlers_code

    total_chars = sum(len(v) for v in existing_code.values())
    logger.info(
        f"API layer complete: {len(existing_code)} files, "
        f"{total_chars} total chars"
    )

    return {
        "generated_handlers_code": handlers_code,
        "generated_go_code": existing_code,
        "status": "api_layer_complete",
        "current_node": "api_layer",
    }


async def _generate_handlers_via_llm(
    llm,
    endpoints: list,
    models_code: str,
    service_code: str,
    exception_handlers: list,
) -> str:
    """Генерирует handlers.go через LLM.
    Если LLM не справился — возвращает fallback с TODO."""

    endpoints_desc = ""
    for ep in endpoints:
        handler = _go_handler_name(ep.get("handler_name", ""))
        req_type = ep.get("request_type", "")
        resp_type = ep.get("response_type", "")
        endpoints_desc += (
            f"- {ep['method']} {ep['path']} → func {handler}"
            f"(c *gin.Context)\n"
            f"  Request body: {req_type or 'none'}\n"
            f"  Response: {resp_type or 'JSON'}\n\n"
        )

    prompt = (
        "You are a Senior Go engineer. Write ONLY Go code. "
        "No JSON wrapping. No markdown. Just pure Go code.\n\n"

        "Write handlers.go for a Gin HTTP server.\n\n"

        "```\npackage main\n```\n\n"

        f"## Endpoints to implement:\n\n{endpoints_desc}\n"

        f"## Available models (already in models_*.go):\n\n"
        f"```go\n{models_code[:2000] if models_code else '// none'}\n```\n\n"

        f"## Available services (already in service.go):\n\n"
        f"```go\n{service_code[:2000] if service_code else '// none'}\n```\n\n"

        "## Requirements:\n"
        "1. package main\n"
        "2. Import: net/http, github.com/gin-gonic/gin\n"
        "3. Each handler function signature: "
        "func HandlerName(c *gin.Context)\n"
        "4. For POST handlers: parse body with c.ShouldBindJSON\n"
        "5. Return JSON: c.JSON(http.StatusOK, response)\n"
        "6. Handle errors: c.JSON(http.StatusBadRequest, "
        "gin.H{\"error\": err.Error()})\n"
        "7. Add ErrorHandlerMiddleware() gin.HandlerFunc\n"
        "8. Write COMPLETE function bodies, not stubs\n"
        "9. Mark complex logic with // TODO comments\n\n"

        "Write the COMPLETE handlers.go file now. "
        "Start with 'package main'.\n"
    )

    try:
        msg = await llm.ainvoke([HumanMessage(content=prompt)])
        content = (
            msg.content if isinstance(msg.content, str)
            else str(msg.content)
        )

        # Извлекаем Go-код из ответа
        code = _extract_go_from_response(content)

        if code and len(code) > 200:
            logger.info(
                f"LLM generated handlers.go: {len(code)} chars"
            )
            return code
        else:
            logger.warning(
                f"LLM handlers too short ({len(code)} chars), "
                f"using fallback"
            )

    except Exception as e:
        logger.exception(f"Error generating handlers: {e}")

    # Fallback
    logger.info("Using fallback handlers.go")
    return _generate_fallback_handlers(endpoints)


def _extract_go_from_response(text: str) -> str:
    """Извлекает Go-код из ответа LLM."""
    # 1. Из markdown code block
    matches = re.findall(r'```(?:go)?\s*\n([\s\S]*?)\n```', text)
    if matches:
        # Берём самый длинный блок
        code = max(matches, key=len)
        if code.strip().startswith("package"):
            return code.strip()

    # 2. Ищем package declaration в тексте
    lines = text.split('\n')
    code_start = -1
    for i, line in enumerate(lines):
        if line.strip().startswith("package "):
            code_start = i
            break

    if code_start >= 0:
        code_lines = lines[code_start:]
        # Убираем markdown артефакты в конце
        while code_lines and code_lines[-1].strip() in ('```', ''):
            code_lines.pop()
        return '\n'.join(code_lines)

    # 3. Весь текст как код
    if "package " in text and "func " in text:
        return text.strip()

    return ""


def _go_handler_name(java_name: str) -> str:
    """camelCase → PascalCaseHandler"""
    if not java_name:
        return "DefaultHandler"
    return java_name[0].upper() + java_name[1:] + "Handler"


def _generate_router(endpoints: list) -> str:
    """Генерирует router.go детерминистически."""
    routes = []
    for ep in endpoints:
        method = ep.get("method", "GET")
        path = ep.get("path", "/")
        handler = _go_handler_name(ep.get("handler_name", "default"))
        routes.append(f'\tr.{method}("{path}", {handler})')

    routes_str = "\n".join(routes)

    return f'''package main

import (
\t"github.com/gin-gonic/gin"
)

// SetupRouter configures all routes and middleware
func SetupRouter() *gin.Engine {{
\tr := gin.Default()

\t// Middleware
\tr.Use(gin.Logger())
\tr.Use(gin.Recovery())
\tr.Use(CORSMiddleware())
\tr.Use(ErrorHandlerMiddleware())

\t// API routes
{routes_str}

\t// Health check
\tr.GET("/health", func(c *gin.Context) {{
\t\tc.JSON(200, gin.H{{"status": "ok"}})
\t}})

\treturn r
}}

// CORSMiddleware handles Cross-Origin Resource Sharing
func CORSMiddleware() gin.HandlerFunc {{
\treturn func(c *gin.Context) {{
\t\tc.Header("Access-Control-Allow-Origin", "*")
\t\tc.Header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
\t\tc.Header("Access-Control-Allow-Headers", "Content-Type, Authorization")

\t\tif c.Request.Method == "OPTIONS" {{
\t\t\tc.AbortWithStatus(204)
\t\t\treturn
\t\t}}

\t\tc.Next()
\t}}
}}
'''


def _generate_main() -> str:
    """Генерирует main.go детерминистически."""
    return '''package main

import (
\t"context"
\t"log"
\t"net/http"
\t"os"
\t"os/signal"
\t"syscall"
\t"time"
)

func main() {
\trouter := SetupRouter()

\tport := os.Getenv("PORT")
\tif port == "" {
\t\tport = "8080"
\t}

\tsrv := &http.Server{
\t\tAddr:    ":" + port,
\t\tHandler: router,
\t}

\t// Start server in goroutine
\tgo func() {
\t\tlog.Printf("Starting server on port %s", port)
\t\tif err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
\t\t\tlog.Fatalf("Server error: %v", err)
\t\t}
\t}()

\t// Graceful shutdown
\tquit := make(chan os.Signal, 1)
\tsignal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
\t<-quit

\tlog.Println("Shutting down server...")
\tctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
\tdefer cancel()

\tif err := srv.Shutdown(ctx); err != nil {
\t\tlog.Fatalf("Forced shutdown: %v", err)
\t}
\tlog.Println("Server exited")
}
'''


def _generate_fallback_handlers(endpoints: list) -> str:
    """Генерирует handlers.go с TODO-заглушками."""
    handlers = []

    for ep in endpoints:
        method = ep.get("method", "GET")
        path = ep.get("path", "/")
        handler_name = _go_handler_name(ep.get("handler_name", ""))
        req_type = ep.get("request_type", "")
        resp_type = ep.get("response_type", "")
        java_class = ep.get("class_name", "")

        if method == "GET" and path == "/":
            handlers.append(f'''
// {handler_name} handles {method} {path}
// Migrated from: {java_class}
func {handler_name}(c *gin.Context) {{
\tc.Redirect(http.StatusFound, "/swagger-ui/index.html")
}}''')
        elif method == "POST":
            bind_code = ""
            if req_type:
                bind_code = f'''
\tvar req {req_type}
\tif err := c.ShouldBindJSON(&req); err != nil {{
\t\tc.JSON(http.StatusBadRequest, gin.H{{"error": err.Error()}})
\t\treturn
\t}}
'''
            handlers.append(f'''
// {handler_name} handles {method} {path}
// Migrated from: {java_class}
// TODO: Implement business logic from Java service
func {handler_name}(c *gin.Context) {{
{bind_code}
\t// TODO: Call service layer and return result
\tc.JSON(http.StatusOK, gin.H{{"status": "not implemented"}})
}}''')
        else:
            handlers.append(f'''
// {handler_name} handles {method} {path}
// Migrated from: {java_class}
// TODO: Implement business logic
func {handler_name}(c *gin.Context) {{
\tc.JSON(http.StatusOK, gin.H{{"status": "ok"}})
}}''')

    handlers_str = "\n".join(handlers)

    return f'''package main

import (
\t"net/http"

\t"github.com/gin-gonic/gin"
)

// ErrorHandlerMiddleware handles panics and returns JSON errors
func ErrorHandlerMiddleware() gin.HandlerFunc {{
\treturn func(c *gin.Context) {{
\t\tdefer func() {{
\t\t\tif err := recover(); err != nil {{
\t\t\t\tc.JSON(http.StatusInternalServerError, gin.H{{
\t\t\t\t\t"error": "Internal server error",
\t\t\t\t}})
\t\t\t\tc.Abort()
\t\t\t}}
\t\t}}()
\t\tc.Next()
\t}}
}}
{handlers_str}
'''