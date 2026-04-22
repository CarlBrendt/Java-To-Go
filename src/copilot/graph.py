from __future__ import annotations

import logging

from langgraph.graph import StateGraph, END, START

from src.copilot.graph_state import MigrationGraphState
from src.copilot.nodes import (
    node_parse_java,
    node_plan,
    node_data_layer,
    node_business_logic,
    node_generate_api_layer,
    node_verify_node,
    node_build_check,
    node_reporting_packaging,
    node_consolidate,
    node_lint_fix
)
from src.settings.config import APISettings

logger = logging.getLogger(__name__)

def build_migration_graph(settings: APISettings):
    workflow = StateGraph(MigrationGraphState)

    # Узлы (без syntax_fix_llm_feedback)
    workflow.add_node("parse", node_parse_java)
    workflow.add_node("plan", node_plan)
    workflow.add_node("data_layer", node_data_layer)
    workflow.add_node("business_logic", node_business_logic)
    workflow.add_node("api_layer", node_generate_api_layer)
    workflow.add_node("consolidate", node_consolidate)
    workflow.add_node("linter", node_lint_fix)
    workflow.add_node("verify", node_verify_node)
    workflow.add_node("build_check", node_build_check)
    workflow.add_node("report", node_reporting_packaging)

    # Граф
    workflow.add_edge(START, "parse")
    workflow.add_edge("parse", "plan")
    workflow.add_edge("plan", "data_layer")
    workflow.add_edge("data_layer", "business_logic")
    workflow.add_edge("business_logic", "api_layer")
    workflow.add_edge("api_layer", "consolidate")
    workflow.add_edge("consolidate", "linter")  
    workflow.add_edge("linter", "verify")
    workflow.add_edge("verify", "build_check")
    workflow.add_edge("build_check", "report")
    workflow.add_edge("report", END)

    return workflow.compile()