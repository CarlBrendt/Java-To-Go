package ru.mts.ip.workflow.engine.service;

import ru.mts.ip.workflow.engine.exception.ClientErrorDescription;
import ru.mts.ip.workflow.engine.executor.ExternalProperties;

import java.util.Collection;

public interface EncryptedDataHandler {
  void applySecrets(Collection<ClientErrorDescription> errorPouch, ExternalProperties resolved);
  void removeCredentials();
}
