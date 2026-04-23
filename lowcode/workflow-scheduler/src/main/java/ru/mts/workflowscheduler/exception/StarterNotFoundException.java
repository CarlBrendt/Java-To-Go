package ru.mts.workflowscheduler.exception;


import org.springframework.http.HttpStatus;
import ru.mts.workflowscheduler.controller.dto.ReqStopStarter;
import ru.mts.workflowscheduler.share.script.ClientError;
import ru.mts.workflowscheduler.share.validation.Errors2;

import java.util.Optional;

public class StarterNotFoundException extends ClientError {
  public StarterNotFoundException(ReqStopStarter reqStopStarter) {
    super(HttpStatus.NOT_FOUND, Errors2.SCHEDULER_STARTER_IS_NOT_FOUND,
        new ErrorMessagePouch().setMessageAargs(new Object[] {Optional.ofNullable(reqStopStarter).map(ReqStopStarter::getName).orElse(null),
            Optional.ofNullable(reqStopStarter).map(ReqStopStarter::getWorkflowId).orElse(null),
            Optional.ofNullable(reqStopStarter).map(ReqStopStarter::getTenantId).orElse(null),}));
  }

}
