package ru.mts.ip.workflow.engine.testutils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.SneakyThrows;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;
import ru.mts.ip.workflow.engine.repository.WorkflowDefinitionRepository;
import ru.mts.ip.workflow.engine.service.DefinitionSearching;
import ru.mts.ip.workflow.engine.service.DefinitionListValue;
import ru.mts.ip.workflow.engine.service.WorkflowVersion;

public class JsonWorkflowDefinitionRepository implements WorkflowDefinitionRepository {
	
  private final Map<String, WorkflowDefinition> definitions;
  ObjectMapper om = new ObjectMapper();
  
  @SneakyThrows
  public JsonWorkflowDefinitionRepository(){
    InputStream st = JsonWorkflowDefinitionRepository.class.getResourceAsStream("/defs.json");
    TestData td = om.readValue(st, TestData.class);
    definitions = td.getDefinitions()
        .stream().map(d -> d.setAvailabilityStatus(Const.DefinitionAvailabilityStatus.ACTIVE))
        .collect(Collectors.toMap(v -> v.getName(), v -> v));
  }
  
  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class TestData{
    private List<WorkflowDefinition> definitions = new ArrayList<>();
  }
  
  
  @Override
  public void flush() {
  }
  
  @Override
  public <S extends WorkflowDefinition> S saveAndFlush(S entity) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public <S extends WorkflowDefinition> List<S> saveAllAndFlush(Iterable<S> entities) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public void deleteAllInBatch(Iterable<WorkflowDefinition> entities) {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  public void deleteAllByIdInBatch(Iterable<UUID> ids) {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  public void deleteAllInBatch() {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  public WorkflowDefinition getOne(UUID id) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public WorkflowDefinition getById(UUID id) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public WorkflowDefinition getReferenceById(UUID id) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public <S extends WorkflowDefinition> List<S> findAll(Example<S> example) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public <S extends WorkflowDefinition> List<S> findAll(Example<S> example, Sort sort) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public <S extends WorkflowDefinition> List<S> saveAll(Iterable<S> entities) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public List<WorkflowDefinition> findAll() {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public List<WorkflowDefinition> findAllById(Iterable<UUID> ids) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public <S extends WorkflowDefinition> S save(S entity) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public Optional<WorkflowDefinition> findById(UUID id) {
    // TODO Auto-generated method stub
    return Optional.empty();
  }
  
  @Override
  public boolean existsById(UUID id) {
    // TODO Auto-generated method stub
    return false;
  }
  
  @Override
  public long count() {
    // TODO Auto-generated method stub
    return 0;
  }
  
  @Override
  public void deleteById(UUID id) {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  public void delete(WorkflowDefinition entity) {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  public void deleteAllById(Iterable<? extends UUID> ids) {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  public void deleteAll(Iterable<? extends WorkflowDefinition> entities) {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  public void deleteAll() {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  public List<WorkflowDefinition> findAll(Sort sort) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public Page<WorkflowDefinition> findAll(Pageable pageable) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public <S extends WorkflowDefinition> Optional<S> findOne(Example<S> example) {
    // TODO Auto-generated method stub
    return Optional.empty();
  }
  
  @Override
  public <S extends WorkflowDefinition> Page<S> findAll(Example<S> example, Pageable pageable) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public <S extends WorkflowDefinition> long count(Example<S> example) {
    // TODO Auto-generated method stub
    return 0;
  }
  
  @Override
  public <S extends WorkflowDefinition> boolean exists(Example<S> example) {
    // TODO Auto-generated method stub
    return false;
  }
  
  @Override
  public <S extends WorkflowDefinition, R> R findBy(Example<S> example,
      Function<FetchableFluentQuery<S>, R> queryFunction) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public Optional<WorkflowDefinition> findFirstByNameOrderByVersionDesc(String name) {
    return Optional.ofNullable(definitions.get(name));
  }
  
  @Override
  public Optional<WorkflowDefinition> findFirstByNameAndTenantIdAndStatusOrderByVersionDesc(String name, String tenantId, String s) {
    return Optional.ofNullable(definitions.get(name));
  }
  
  @Override
  public Optional<WorkflowDefinition> findFirstByNameAndTenantIdAndVersionAndStatusOrderByVersionDesc(String name,
      String tenantId, Integer version, String s) {
    return Optional.ofNullable(definitions.get(name));
  }
  
  @Override
  public Optional<WorkflowDefinition> findOne(Specification<WorkflowDefinition> spec) {
    // TODO Auto-generated method stub
    return Optional.empty();
  }
  
  @Override
  public List<WorkflowDefinition> findAll(Specification<WorkflowDefinition> spec) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public Page<WorkflowDefinition> findAll(Specification<WorkflowDefinition> spec,
      Pageable pageable) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public List<WorkflowDefinition> findAll(Specification<WorkflowDefinition> spec, Sort sort) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public long count(Specification<WorkflowDefinition> spec) {
    // TODO Auto-generated method stub
    return 0;
  }
  
  @Override
  public boolean exists(Specification<WorkflowDefinition> spec) {
    // TODO Auto-generated method stub
    return false;
  }
  
  @Override
  public long delete(Specification<WorkflowDefinition> spec) {
    // TODO Auto-generated method stub
    return 0;
  }
  
  @Override
  public <S extends WorkflowDefinition, R> R findBy(Specification<WorkflowDefinition> spec,
      Function<FetchableFluentQuery<S>, R> queryFunction) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public List<DefinitionListValue> search(DefinitionSearching searchConfig) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public Long searchCount(DefinitionSearching searchConfig) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public void markRemoved(String name, String tenantId, String status) {
    // TODO Auto-generated method stub
  }
  
  
  @Override
  public boolean existsByNameAndTenantIdAndStatusAndDeleted(String name, String tenantId, String status, boolean deleted) {
    return false;
  }
  
  @Override
  public Optional<WorkflowDefinition> findByIdAndStatusAndDeleted(UUID id, String status, boolean deleted) {
    return Optional.empty();
  }
  
  @Override
  public Optional<WorkflowVersion> findFirstVersionByNameAndTenantIdAndStatusOrderByVersionDesc(
      String name, String tenantId, String status) {
    return Optional.empty();
  }

  @Override
  public Optional<WorkflowDefinition> findFirstByNameAndTenantIdAndStatusAndAvailabilityStatusOrderByVersionDesc(
      String name, String tenantId, String status, String availabilityStatus) {
    return Optional.ofNullable(definitions.get(name));
  }

  @Override
  public void markNotLatest(UUID id) {
    // TODO Auto-generated method stub
  }

  @Override
  public void markAllNotLatest(String name, String tenantId, String status) {
    // TODO Auto-generated method stub
  }

  @Override
  public void markRemoved(String name, String tenantId, String status, Integer version) {
    
  }


}
