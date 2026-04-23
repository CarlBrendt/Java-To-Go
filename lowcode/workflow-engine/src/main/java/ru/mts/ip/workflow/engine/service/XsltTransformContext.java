package ru.mts.ip.workflow.engine.service;

import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.dto.Ref;

@Data
@Accessors(chain = true)
public class XsltTransformContext {

  private String xsltTemplate;
  private Ref xsltTemplateRef;
  private String xsltTransformTarget;
  private Ref xsltTransformTargetRef;
  
}