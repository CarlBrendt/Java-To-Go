package ru.mts.ip.workflow.engine.service;

import java.util.List;
import javax.xml.validation.Schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.xml.sax.SAXException;
import ru.mts.ip.workflow.engine.dto.XsdValidation;
import ru.mts.ip.workflow.engine.dto.XsdValidation.XsdImport;
import ru.mts.ip.workflow.engine.exception.ClientErrorDescription;

public interface XsdService {
  Schema creteSchema(String rootSchema, List<XsdImport> imports) throws SAXException;
  List<ClientErrorDescription> validateJsonWithXml(JsonNode jsonNode, XsdValidation xsdValidation);
}
