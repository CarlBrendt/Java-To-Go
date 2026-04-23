package ru.mts.workflowmail.starter;

import lombok.Data;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.service.folder.Folder;
import org.springframework.integration.dsl.IntegrationFlow;
import ru.mts.workflowmail.controller.dto.Worker;
import ru.mts.workflowmail.service.integrationflow.ExceptionHolder;

import java.util.UUID;


@Data
public class MailListenerContainerWrapper {
  private UUID workerId;
  private IntegrationFlow integrationFlow;
  private ExceptionHolder exceptionHolder;
  private ExchangeService exchangeService;

  public MailListenerContainerWrapper(Worker worker, IntegrationFlow flow, ExceptionHolder exceptionHolder) {
    this(worker, flow, null, exceptionHolder);
  }

  public MailListenerContainerWrapper(Worker worker, IntegrationFlow flow, ExchangeService exchangeService, ExceptionHolder exceptionHolder) {
    this.workerId = worker.getId();
    this.integrationFlow = flow;
    this.exchangeService = exchangeService;
    this.exceptionHolder = exceptionHolder;
  }


  public boolean containsError() {
    if(exceptionHolder.getException() != null) {
      return true;
    } else if (exchangeService != null) {
      try {
        Folder inbox = Folder.bind(exchangeService, WellKnownFolderName.Inbox);
      } catch (Exception e) {
        return true;
      }
    }
    return false;
  }
}
