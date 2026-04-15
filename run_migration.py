import asyncio
import os
import logging

from src.copilot.graph import build_migration_graph
from src.copilot.graph_state import MigrationGraphState
from src.settings.config import APISettings # Assuming APISettings can be imported and initialized without args

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

async def run_migration_pipeline():
    # Initialize APISettings - it should load from .env or environment variables
    # For a test script, ensure your .env has placeholder values or actual values
    # For instance, create a .env file in the root with:
    # HOST="localhost"
    # PORT=8080
    # SECRET_KEY="supersecret"
    # MODEL_API_KEY="dummy_key"
    # MWS_GPT_BASE_URL="http://localhost:8000"
    # MINIO_ENDPOINT="localhost:9000"
    # MINIO_ACCESS_KEY="minioadmin"
    # MINIO_SECRET_KEY="minioadmin"
    # MINIO_SECURE=False

    settings = APISettings()
    # Define the path to your Java project
    java_project_path = os.path.abspath("./workflow-ftp")
    output_dir = os.path.abspath("./go_output")

    # Ensure output directory exists
    os.makedirs(output_dir, exist_ok=True)

    # Initial state for the graph
    initial_state: MigrationGraphState = {
        "java_project_path": java_project_path,
        "output_dir": output_dir,
        "java_structure": {},
        "api_contract": [],
        "dependency_graph": {},
        "package": "",
        "migration_plan": "",
        "plan_steps": [],
        "human_feedback": "",
        "generated_models_code": "",
        "generated_service_code": "",
        "generated_handlers_code": "",
        "generated_go_code": {},
        "current_node": "start",
        "status": "initializing",
        "retry_attempts": 0,
        "verification_passed": False,
        "verification_errors": [],
    }

    # Build the graph
    app = build_migration_graph(settings)

    logger.info("Starting migration pipeline...")
    # Run the graph
    final_state = await app.ainvoke(initial_state)

    logger.info("Migration pipeline finished.")

    generated_go_code = final_state.get("generated_go_code", {})
    if generated_go_code:
        logger.info(f"Generated {len(generated_go_code)} Go files:")
        for filename, content in generated_go_code.items():
            file_path = os.path.join(output_dir, filename)
            with open(file_path, "w", encoding="utf-8") as f:
                f.write(content)
            logger.info(f"  - Wrote {filename} to {file_path}")
            # Optionally print content for inspection
            # logger.info(f"---- {filename} ----\n{content}\n--------------------")
    else:
        logger.warning("No Go code was generated.")

    logger.info(f"Final status: {final_state.get('status')}")
    logger.info(f"Migration Plan:\n{final_state.get('migration_plan')}")

if __name__ == "__main__":
    asyncio.run(run_migration_pipeline())
