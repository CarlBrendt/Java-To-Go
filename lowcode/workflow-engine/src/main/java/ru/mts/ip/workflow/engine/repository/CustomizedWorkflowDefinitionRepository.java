package ru.mts.ip.workflow.engine.repository;

import java.util.List;

import ru.mts.ip.workflow.engine.service.DefinitionListValue;
import ru.mts.ip.workflow.engine.service.DefinitionSearching;

public interface CustomizedWorkflowDefinitionRepository {

  List<DefinitionListValue> search(DefinitionSearching searchConfig);
  Long searchCount(DefinitionSearching searchConfig);

}
