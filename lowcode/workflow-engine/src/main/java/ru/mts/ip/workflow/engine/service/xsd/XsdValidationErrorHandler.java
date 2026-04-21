package ru.mts.ip.workflow.engine.service.xsd;

import java.util.ArrayList;
import java.util.List;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import lombok.Data;
import ru.mts.ip.workflow.engine.Const.Errors2;
import ru.mts.ip.workflow.engine.exception.ClientErrorDescription;

@Data
public class XsdValidationErrorHandler implements ErrorHandler {

  private List<ClientErrorDescription> errors = new ArrayList<>();
  
  @Override
  public void warning(SAXParseException exception) throws SAXException {
    
  }

  @Override
  public void error(SAXParseException exception) throws SAXException {
    errors.add(new ClientErrorDescription(Errors2.XSD_VALIDATION_FAILURE)
        .setAdviceMessageArgs(new Object[]{exception.getMessage()}));
  }

  @Override
  public void fatalError(SAXParseException exception) throws SAXException {
    errors.add(new ClientErrorDescription(Errors2.XSD_VALIDATION_FAILURE)
        .setAdviceMessageArgs(new Object[]{exception.getMessage()}));
  }
  
  

}
