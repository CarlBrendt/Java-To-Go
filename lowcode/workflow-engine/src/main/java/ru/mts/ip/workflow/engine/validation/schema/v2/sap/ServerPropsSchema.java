package ru.mts.ip.workflow.engine.validation.schema.v2.sap;



import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.NumberSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;

public class ServerPropsSchema extends ObjectSchema {
  
  private static final String JCO_SERVER_GWHOST = "jco.server.gwhost";
  private static final String JCO_SERVER_PROGID = "jco.server.progid";
  private static final String JCO_SERVER_GWSERV = "jco.server.gwserv";
  private static final String JCO_SERVER_CONNECTION_COUNT = "jco.server.connection_count";
  
  public ServerPropsSchema(Constraint... constraints) {
    super(constraints);
    putField(JCO_SERVER_GWHOST, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
    putField(JCO_SERVER_PROGID, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
    putField(JCO_SERVER_GWSERV, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
    putField(JCO_SERVER_CONNECTION_COUNT, new NumberSchema(NOT_NULL));
  }

}
