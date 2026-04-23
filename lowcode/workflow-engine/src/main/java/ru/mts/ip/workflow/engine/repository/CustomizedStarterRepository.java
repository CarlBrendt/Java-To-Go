package ru.mts.ip.workflow.engine.repository;



import ru.mts.ip.workflow.engine.service.dto.StarterSearching;
import ru.mts.ip.workflow.engine.service.dto.StarterShortListValue;

import java.util.List;

public interface CustomizedStarterRepository {
  List<StarterShortListValue> search(StarterSearching searchConfig);
  Long searchCount(StarterSearching searchConfig);
}
