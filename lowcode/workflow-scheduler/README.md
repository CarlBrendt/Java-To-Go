# Задача сервиса:
Запуск рабочих процессов по рассписанию

# Environment variables:

| Name | Description | Default value                                      |
| ------ | ------ |----------------------------------------------------|
| ```SECURITY_ENABLED``` | Security on/off  | true                                               |
| ```NODE_IP```  | Opentelemetry trace exporter host| 127.0.0.1                                          |
| ```TRACING_OTLP_COLLECTOR_PORT```  | Opentelemetry trace exporter port| 4318                                               |
| ```OPENTELEMETRY_SERVICE_NAME```  | Opentelemetry service name| IP.SharedServices.WorkflowScheduler                |
| ```OPENTELEMETRY_SERVICE_VERSION```  | Opentelemetry service version| 0.0.1                                              |
| ```MANAGEMENT_BASE_PATH```  | Management base path | /actuator                                          |
| ```SERVER_PORT```  | Server port | 9011                                               |
| ```METRICS_PATH```  | Prometheus metrics path | /prometheus                                        |
| ```OAUTH2_ISSUER_URI```  | URI that can either be an OpenID Connect discovery endpoint or an OAuth 2.0 Authorization Server Metadata endpoint defined by RFC 8414| https://isso-dev.mts.ru/auth/realms/mts            |
| ```WORKFLOW_SCRIPT_EXECUTOR```  | workflow-script-executor url | http://control-plane-workflow-script-executor:8080 |
