package ru.mts.ip.workflow.engine.testutils;

public interface TestControllerBodies {
  static String SAP_STARTER_CREATE = """
      {
          "type": "sap_inbound",
          "name": "name",
          "description": "description",
          "startDateTime": "2023-10-01T12:00:00+03:00",
          "endDateTime": "2023-10-01T12:00:00+03:00",
          "workflowDefinitionToStartId": "a5c8f570-039f-407e-9e2f-2483bf7b9e5f",
          "sapInbound": {
              "serverProps": {
                  "serverProp1": "propval1"
              },
              "destinationProps": {
                  "destinationProp1": "destProp1"
              }
          }
      }
      """;
}
