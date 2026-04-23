package ru.mts.ip.workflow.engine.validation.schema.v2.kafka;



import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

import java.util.List;

import static ru.mts.ip.workflow.engine.validation.Constraint.ACCEPTABLE_SSL_TRUST_STORE_TYPE;
import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;


public class SslSchema extends ObjectSchema {

  public static final String TRUST_STORE_TYPE = "trustStoreType";
  public static final String TRUST_STORE_SERTIFICATES = "trustStoreCertificates";
  
  public SslSchema(Constraint...constraints) {
    super(List.of(constraints));
    putField(TRUST_STORE_TYPE, new StringSchema(FILLED, NOT_NULL, NOT_BLANK, ACCEPTABLE_SSL_TRUST_STORE_TYPE));
    putField(TRUST_STORE_SERTIFICATES, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
  }

}
