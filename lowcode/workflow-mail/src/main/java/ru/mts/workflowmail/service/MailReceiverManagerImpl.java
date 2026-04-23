package ru.mts.workflowmail.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.Lifecycle;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.stereotype.Service;
import ru.mts.workflowmail.activity.FetchedVaultPropertiesResolver;
import ru.mts.workflowmail.activity.SimpleCrypt;
import ru.mts.workflowmail.controller.dto.Worker;
import ru.mts.workflowmail.mapper.DtoMapper;
import ru.mts.workflowmail.service.dto.ConsumerError;
import ru.mts.workflowmail.service.dto.MailConnection;
import ru.mts.workflowmail.service.dto.MailConnection.MailAuth;
import ru.mts.workflowmail.service.integrationflow.DynamicMailListener;
import ru.mts.workflowmail.starter.MailListenerContainerWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailReceiverManagerImpl implements MailReceiverManager {

  private final Map<UUID, MailListenerContainerWrapper> allWorkers = new ConcurrentHashMap<>();
  private final DtoMapper mapper;
  private final DynamicMailListener listenerConfig;
  private final FetchedVaultPropertiesResolver vaultPropertiesResolver =
      new FetchedVaultPropertiesResolver();
  private final IntegrationFlowContext flowContext;

  @Override
  public void startWorker(Worker worker) {
    if (!allWorkers.containsKey(worker.getId())) {
      var starter = worker.getStarter();
      var mailDetails = starter.getMailConsumer();
      MailConnection conn = mailDetails.getConnectionDef();
      applyPlaneVaultSecrets(conn);
      var mailListenerWrapper = listenerConfig.addMailListener(worker);
      flowContext.registration(mailListenerWrapper.getIntegrationFlow())
          .id(worker.getId().toString())
          .register();
      allWorkers.put(worker.getId(), mailListenerWrapper);
    }
  }

  @Override
  public int getWorkerCount() {
    return allWorkers.size();
  }

  @Override
  public void stopStolenLocalWorkers(Set<UUID> workerIds) {
    var allLocalWorkers = allWorkers.keySet();
    for (UUID workerId : allLocalWorkers) {
      if (!workerIds.contains(workerId)) {
        stopProcessByWorkerId(workerId);
        log.info("stopStolenLocalWorkers. Worker has been removed with id {}", workerId);
      }
    }
  }

  @Override
  public List<ConsumerError> closeBrokenConsumers() {
    var allLocalWorkers = allWorkers.keySet();
    Set<UUID> healthyConsumers = getHealthyConsumers();
    List<ConsumerError> consumerErrors = new ArrayList<>();
    for (UUID workerId : allLocalWorkers) {
      if (!healthyConsumers.contains(workerId)) {
        var removed = stopProcessByWorkerId(workerId);
        log.info("closeBrokenConsumers. Worker has been removed with id {}", workerId);
        ConsumerError consumerError = new ConsumerError();
        consumerError.setWorkerId(workerId);
        consumerError.setError(removed.getExceptionHolder().getException());
        consumerErrors.add(consumerError);
      }
    }
    return consumerErrors;
  }

  private MailListenerContainerWrapper stopProcessByWorkerId(UUID workerId) {
    var exchangeService = allWorkers.get(workerId).getExchangeService();
    if (exchangeService != null) {
      exchangeService.close();
    }
    flowContext.remove(workerId.toString());
    return allWorkers.remove(workerId);
  }

  @Override
  public Set<UUID> getHealthyConsumers() {

    return allWorkers.values()
        .stream()
        .filter(wrapper -> !wrapper.containsError())
        .map(MailListenerContainerWrapper::getWorkerId)
        .filter(workerId -> {
          IntegrationFlowContext.IntegrationFlowRegistration registration =
              flowContext.getRegistrationById(workerId.toString());
          if (registration != null) {
            Object flow = registration.getIntegrationFlow();
            if (flow instanceof Lifecycle lifecycle) {
              return lifecycle.isRunning();
            }
          }
          return false;
        })
        .collect(Collectors.toSet());
  }

  private void applyPlaneVaultSecrets(MailConnection connection) {
    var mailAuth = Optional.ofNullable(connection.getMailAuth());
    var pass = mailAuth.map(MailAuth::getPassword).flatMap(vaultPropertiesResolver::findValue);
    var userName = mailAuth.map(MailAuth::getUsername).flatMap(vaultPropertiesResolver::findValue);

    var trustStoreCertificates = mailAuth.map(MailAuth::getCertificate)
        .map(MailConnection.MailCertificate::getTrustStoreCertificates).flatMap(vaultPropertiesResolver::findValue);

    var host = Optional.ofNullable(connection.getHost()).flatMap(vaultPropertiesResolver::findValue);
    if (pass.isPresent()
        || userName.isPresent()
        || trustStoreCertificates.isPresent()
        || host.isPresent()) {

      SimpleCrypt sc = new SimpleCrypt();
      pass.map(sc::decryp).ifPresent(decrypted -> mailAuth.get().setPassword(decrypted));
      userName.map(sc::decryp).ifPresent(decrypted -> mailAuth.get().setUsername(decrypted));
      trustStoreCertificates.map(sc::decryp).ifPresent(decrypted -> mailAuth.get().getCertificate().setTrustStoreCertificates(decrypted));
      host.map(sc::decryp).ifPresent(connection::setHost);
    }
  }

}
