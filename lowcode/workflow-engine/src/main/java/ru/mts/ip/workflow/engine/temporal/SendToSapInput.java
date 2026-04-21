package ru.mts.ip.workflow.engine.temporal;

import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.dto.Ref;

public record SendToSapInput(String activityId, Ref connectionRef, SapConnection connectionDef,
    SapIDoc idoc) {

    public record SapIDoc(String xml) {
    }

    public record SapConnection(Map<String, JsonNode> props){

    }
}
