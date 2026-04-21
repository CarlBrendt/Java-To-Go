package ru.mts.ip.workflow.engine.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.dto.Ref;

@Data
@Accessors(chain = true)
@JsonInclude(Include.NON_NULL)
public class XsltTransformConfig {

  private String activityId;
  @Valid
  private Ref xsltTemplateRef;
  private String xsltTemplate;

  @Valid
  private Ref xsltTransformTargetRef;
  private String xsltTransformTarget;
  
  public XsltTransformConfig copy() {
    return new XsltTransformConfig()
        .setActivityId(activityId)
        .setXsltTransformTargetRef(xsltTransformTargetRef == null ? null : xsltTransformTargetRef.copy())
        .setXsltTransformTarget(xsltTransformTarget)
        .setXsltTemplateRef(xsltTemplateRef == null ? null : xsltTemplateRef.copy())
        .setXsltTemplate(xsltTemplate);
  }

}
