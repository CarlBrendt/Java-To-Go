package ru.mts.ip.workflow.engine.validation.schema.v2.mail;

import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

import static ru.mts.ip.workflow.engine.validation.Constraint.ACCEPTABLE_SSL_TRUST_STORE_TYPE;
import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;

public class MailCertificateSchema extends ObjectSchema {

  public static final String TRUST_STORE_TYPE = "trustStoreType";
  public static final String TRUST_STORE_CERTIFICATES = "trustStoreCertificates";

  public MailCertificateSchema(Constraint... constraints) {
    super(constraints);
    putField(TRUST_STORE_TYPE, new StringSchema(NOT_NULL, ACCEPTABLE_SSL_TRUST_STORE_TYPE));
    putField(TRUST_STORE_CERTIFICATES, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
  }
}
