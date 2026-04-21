package ru.mts.ip.workflow.engine.validation.schema.stringformat;

import com.networknt.schema.JsonMetaSchema;
import com.networknt.schema.JsonSchemaFactory;

public class CustomJsonSchemaFactory {

  public static JsonSchemaFactory getInstanceV4WithStringFormat() {
    JsonMetaSchema metaSchema = JsonMetaSchema.getV4();
    var stringFormatSchema =
        JsonMetaSchema.builder(metaSchema).keyword(new StringFormatKeyword()).build();

   return JsonSchemaFactory.builder()
        .defaultMetaSchemaIri(stringFormatSchema.getIri())
        .metaSchema(stringFormatSchema)
        .build();
  }
}
