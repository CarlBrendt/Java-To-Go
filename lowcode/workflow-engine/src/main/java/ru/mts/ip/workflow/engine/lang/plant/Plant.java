package ru.mts.ip.workflow.engine.lang.plant;

public class Plant {
  public static final String EXAMPLE = """
      @startuml
      start
          /'xslt_transform'/:Замена значение DOCNUM на 12345 в IDoc;
          /'send_to_sap'/:Отправка IDoc в SAP;
          /'send_to_rabbit'/:Отправка IDoc в rabbitmq;
          /'transform'/:Преобразование IDoc в json;
          /'db_function_call'/:Выполнение функции в БД;
      stop
      @enduml
      /'
      {
          "tenantId": "portal",
          "name": "wfplunt_SAPwithoutREST",
          "description": "Преобразование idoc, отправка его в очередь, запись в БД",
          "type": "complex",
          "details": {
              "starters": [
                  {
                      "type": "sap_inbound",
                      "sapInbound": {
                          "inboundDef": {
                              "connectionDef": {
                                  "name": "123",
                                  "props": {
                                      "jco.client.lang": "EN",
                                      "jco.destination.peak_limit": 10,
                                      "jco.client.client": 400,
                                      "jco.client.sysnr": 10,
                                      "jco.destination.pool_capacity": 3,
                                      "jco.client.ashost": "ashost",
                                      "jco.client.user": "user",
                                      "jco.client.passwd": "passwd"
                                  }
                              },
                              "name": "SapStartInbound15041139-01",
                              "tenantId": "default",
                              "props": {
                                  "jco.server.gwhost": "gwhost",
                                  "jco.server.progid": "progid",
                                  "jco.server.gwserv": "gwserv",
                                  "jco.server.connection_count": 2
                              }
                          }
                      }
                  },
                  {
                      "type": "rest_call"
                  }
              ],
              "inputValidateSchema": {
                  "$schema": "http://json-schema.org/draft-04/schema#",
                  "type": "object",
                  "properties": {
                      "idoc": {
                          "type": "string",
                          "stringFormat": "xml"
                      }
                  },
                  "required": [
                      "idoc"
                  ]
              }
          },
          "activities": {
              "xslt_transform": {
                  "type": "workflow_call",
                  "workflowCall": {
                      "workflowDef": {
                          "type": "xslt_transform",
                          "details": {
                              "xsltTransformConfig": {
                                  "xsltTemplate": "<xsl:stylesheet version=\\"1.0\\" xmlns:xsl=\\"http:\\/\\/www.w3.org\\/1999\\/XSL\\/Transform\\" xmlns:foo=\\"http:\\/\\/www.foo.org\\/\\" xmlns:bar=\\"http:\\/\\/www.bar.org\\">\\r\\n    <xsl:template match=\\"node()|@*\\">\\r\\n        <xsl:copy>\\r\\n            <xsl:apply-templates select=\\"node()|@*\\"\\/>\\r\\n        <\\/xsl:copy>\\r\\n    <\\/xsl:template> \\r\\n    <xsl:template match=\\"SENDER_1C\\/text()\\">RS52.TVR-\\"\\u0442\\u0435\\u043A\\u0441\\u0442 \\u0441\\u043A\\u043B\\u0435\\u0439\\u043A\\u0438\\"<\\/xsl:template> \\r\\n<\\/xsl:stylesheet>",
                                  "xsltTransformTarget": "jp{idoc}"
                              }
                          }
                      }
                  }
              },
              "send_to_sap": {
                  "type": "workflow_call",
                  "workflowCall": {
                      "workflowDef": {
                          "type": "send_to_sap",
                          "details": {
                              "sendToSapConfig": {
                                  "connectionDef": {
                                      "props": {
                                          "jco.client.lang": "EN",
                                          "jco.client.passwd": "passwd",
                                          "jco.client.sysnr": 10,
                                          "jco.destination.pool_capacity": 3,
                                          "jco.destination.peak_limit": 10,
                                          "jco.client.client": 400,
                                          "jco.client.user": "user",
                                          "jco.client.ashost": "ashost.ashost.ru"
                                      }
                                  },
                                  "idoc": {
                                      "xml": "jp{xsltTransformResult}"
                                  }
                              }
                          }
                      }
                  }
              },
              "send_to_rabbit": {
                  "type": "workflow_call",
                  "workflowCall": {
                      "workflowDef": {
                          "type": "send_to_rabbitmq",
                          "details": {
                              "sendToRabbitmqConfig": {
                                  "connectionDef": {
                                      "userName": "userName",
                                      "userPass": "userPass",
                                      "virtualHost": "/",
                                      "addresses": [
                                          "11.11.111.111:5672"
                                      ]
                                  },
                                  "exchange": "amq.direct",
                                  "routingKey": "rk-to-testq",
                                  "message": "jp{xsltTransformResult}",
                                  "messageProperties": {
                                      "contentType": "application/xml"
                                  }
                              }
                          }
                      }
                  }
              },
              "transform": {
                  "type": "workflow_call",
                      "workflowCall": {
                          "workflowDef": {
                              "type": "transform",
                              "details": {
                                  "transformConfig": {
                                      "type": "xml_to_json",
                                      "target": {
                                          "xml": {
                                              "idoc_json": "jp{xsltTransformResult}"
                                          }
                                      }
                                  }
                              }
                          }
                      }
              },
              "db_function_call": {
                  "type": "workflow_call",
                  "workflowCall": {
                      "args": {
                          "_doc_num": "jp{idoc_json.IDOC.EDI_DC40.DOCNUM}",
                          "_sta_con": "jp{idoc_json.IDOC.EDI_DC40.IDOCTYP}",
                          "_sta_txt": "jp{idoc_json.IDOC.ZSTCK902_H.SENDER_1C}"
                      },
                      "workflowDef": {
                          "type": "db_call",
                          "details": {
                              "databaseCallConfig": {
                                  "databaseCallDef": {
                                      "type": "function",
                                      "schema": "schema",
                                      "functionName": "functionName",
                                      "inParameters": {
                                          "_doc_num": "VARCHAR",
                                          "_sta_con": "VARCHAR",
                                          "_sta_txt": "VARCHAR"
                                      },
                                      "outParameters": {
                                          "res": "INTEGER"
                                      }
                                  },
                                  "dataSourceDef": {
                                      "url": "jdbc:postgresql://11.12.121.109:5432/test",
                                      "className": "org.postgresql.Driver",
                                      "userName": "userName",
                                      "userPass": "userPass"
                                  }
                              }
                          }
                      }
                  }
              }
          }
      }
      '/
      """;
  

}
