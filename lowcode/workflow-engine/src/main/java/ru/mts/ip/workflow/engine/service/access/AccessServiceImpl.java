package ru.mts.ip.workflow.engine.service.access;

import com.google.common.base.Strings;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.EngineConfigurationProperties;
import ru.mts.ip.workflow.engine.dto.DetailedWorkflowDefinition;
import ru.mts.ip.workflow.engine.dto.WorkflowAccessList;
import ru.mts.ip.workflow.engine.dto.WorkflowAccessList.AccessEntry;
import ru.mts.ip.workflow.engine.entity.WorkflowAccessWhiteListId;
import ru.mts.ip.workflow.engine.entity.WorkflowAccessWhitelist;
import ru.mts.ip.workflow.engine.repository.WorkflowAccessWhiteListRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static ru.mts.ip.workflow.engine.Const.DefinitionAvailabilityStatus.DECOMMISSIONED;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessServiceImpl implements AccessService{

  private final WorkflowAccessWhiteListRepository whiteListRepository;
  private final EngineConfigurationProperties appProperties;

  @Override
  @Transactional
  public void appendAccessConfig(WorkflowAccessList list) {
    var whiteListToSave = list.getAccessEntries().stream()
        .filter(entry -> !Strings.isNullOrEmpty(entry.getOauth2ClientId()))
        .distinct()
        .map(entry -> new WorkflowAccessWhiteListId(entry.getOauth2ClientId(), entry.getWorkflowId()))
        .map(WorkflowAccessWhitelist::new)
        .toList();
    whiteListRepository.saveAll(whiteListToSave);
  }

  @Override
  public boolean isPermissionsEnoughToRunWorkflow(DetailedWorkflowDefinition definition) {
    return findAccessTroubleToStartWorkflow(definition).isEmpty();
  }

  @Override
  public Optional<Const.Errors2> findAccessTroubleToStartWorkflow(DetailedWorkflowDefinition definition) {
    if (DECOMMISSIONED.equals(definition.getAvailabilityStatus())) {
      return Optional.of(Const.Errors2.ACCESS_DENIED_TO_START_DECOMMISSIONED_WORKFLOW);
    } else if (appProperties.isSecurityEnabled() && RequestContextHolder.getRequestAttributes() != null) {
      var innerAuthClientId = appProperties.getAuth2ClientId();
      var jwtOptional = extractJwt();
      if (jwtOptional.isEmpty()) {
        return Optional.of(Const.Errors2.JWT_NOT_FOUND);
      }
      var jwt = jwtOptional.get();
      String userName =
          ObjectUtils.firstNonNull(jwt.getClaim("clientId"), jwt.getClaim("client_id"));
      if (userName == null) {
        return Optional.of(Const.Errors2.JWT_CLIENTID_IS_NULL);
      } else if (!innerAuthClientId.equals(userName)
                 && !whiteListRepository.existsByOauth2ClientIdAndWorkflowDefinitionId(userName,
          definition.getId())) {
        return Optional.of(Const.Errors2.JWT_CLIENTID_NOT_IN_ACCESS_LIST);
      }
    } return Optional.empty();
  }


  @Override
  @Transactional
  public void replaceAccessList(WorkflowAccessList list) {
    var accessEntries = Optional.ofNullable(list.getAccessEntries()).orElse(List.of());
    Set<UUID> toDelete = accessEntries.stream().map(AccessEntry::getWorkflowId).collect(Collectors.toSet());
    whiteListRepository.clearWorkflowAccessList(toDelete);

    appendAccessConfig(list);
  }

  @Override
  public List<String> getClientIds(UUID definitionId) {
    return whiteListRepository.getByWorkflowDefinitionId(definitionId)
        .stream().map(WorkflowAccessWhitelist::getOauth2ClientId).distinct().toList();
  }

  private Optional<Jwt> extractJwt() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwtToken) {
      var creds = jwtToken.getCredentials();
      if (creds instanceof Jwt jwt) {
        return Optional.of(jwt);
      }
    }
    return Optional.empty();
  }

}
