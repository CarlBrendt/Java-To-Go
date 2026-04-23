# Описание сервиса:
Выполняет роль API Gateway Low-code.   
Отвечает за CRUD, валидацию и запуск схем, историю исполнения.   

# Environment variables:

| Name | Description | Default value|
| ------ | ------ | ------ |
| ```SECURITY_ENABLED``` | Security on/off  | true |
| ```NODE_IP```  | Opentelemetry trace exporter host| 127.0.0.1 |
| ```TRACING_OTLP_COLLECTOR_PORT```  | Opentelemetry trace exporter port| 4318 |
| ```OPENTELEMETRY_SERVICE_NAME```  | Opentelemetry service name| IP.SharedServices.WorkflowEngine |
| ```OPENTELEMETRY_SERVICE_VERSION```  | Opentelemetry service version| 0.0.1 |
| ```VAULT_ENDPOINT```  | Vault endpoint | https://vault.mts-corp.ru |
| ```VAULT_ROLE_ID```  | Vault role id | nope |
| ```VAULT_SECRET_ID```  | Vault secret id | nope |
| ```VAULT_APP_ROLE_PATH```  | Vault role id | approle |
| ```VAULT_SECRET_ENGINE_NAME```  | Vault secret engine name |  |
| ```VAULT_SECRET_PATH_PREFIX```  | Vault secret path prefix |  |
| ```MANAGEMENT_BASE_PATH```  | Management base path | /actuator |
| ```SERVER_PORT```  | Server port | 9090 |
| ```METRICS_PATH```  | Prometheus metrics path | /prometheus |
| ```WORKFLOW_XSLT_TRANSFORM```  | workflow-xslt url | http://workflow-xslt:8080|
| ```WORKFLOW_PRIMITIVES```  | workflow-primitives url | http://workflow-primitives:8080 |
| ```WORKFLOW_DATABASE_CALL```  | workflow-db-call url | http://workflow-db-call:8080 |
| ```WORKFLOW_SAP```  | workflow-sap url | http://workflow-sap:8080 |
| ```WORKFLOW_RABBITMQ```  | workflow-rabbitmq url | http://workflow-rabbitmq:8080 |
| ```WORKFLOW_S3```  | workflow-s3 url | http://workflow-s3:8080 |
| ```OAUTH2_CLIENT_ID```  | OAuth2 client id | wf-sap-dev |
| ```OAUTH2_CLIENT_SECRET```  | OAuth2 client secret | nope |
| ```OAUTH2_TOKEN_URI```  | OAuth2 token uri | https://isso-dev.mts.ru/auth/realms/mts/protocol/openid-connect/token |
| ```OAUTH2_ISSUER_URI```  | URI that can either be an OpenID Connect discovery endpoint or an OAuth 2.0 Authorization Server Metadata endpoint defined by RFC 8414| https://isso-dev.mts.ru/auth/realms/mts |
| ```TEMPORAL_NAMESPACE```  | Temporal namespace | default |
| ```TEMPORAL_HOST_PORT```  | Temporal host port | 127.0.0.1:7233 |
| ```DB_JDBC_URL```  | JDBC url | jdbc:postgresql://127.0.0.1:5432/postgres | 
| ```DB_USERNAME```  | Database user name | postgres |
| ```DB_PASSWORD```  | Database user pass | 1 |
| ```DB_SCHEMA_NAME```  | Database schema name | wf_engine |
| ```DB_FILE_STORAGE```  | db-file-storage url | http://control-plane-db-file-storage:8080 |
| ```MAX_WORKFLOW_THREAD_COUNT```  | Maximum number of threads available for workflow execution across all workers created by the Factory. This includes cached workflows. | 300 |
| ```WORKFLOW_CACHE_SIZE```  | Workflow cache size | 300 |
| ```MAX_CONCURRENT_ACTIVITY_EXECUTION_SIZE```  | Maximum number of activities executed in parallel | 100 |
| ```MAX_CONCURRENT_WORKFLOW_TASK_EXECUTION_SIZE```  | Maximum number of simultaneously executed workflow tasks | 100 |
| ```SYNC_START_TIMEOUT_LIMIT_SECONDS```  | Sync start timeout limit seconds | 300 |
| ```SYNC_START_TIMEOUT_DEFAULT_SECONDS```  | Sync start timeout default seconds | 180 |
| ```DEFAULT_EXECUTION_TIMEOUT_SECONDS```  | Default workflow execution timeout seconds | 172800 |
| ```MAX_VARIABLE_SIZE_BYTES```  | variables size limit bytes | 256000 |
| ```SECURITY_CLIENT_CREDENTIALS_ENABLED```  | hide client credentials | true |

