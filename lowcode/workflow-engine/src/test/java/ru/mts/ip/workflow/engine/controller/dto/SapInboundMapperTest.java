package ru.mts.ip.workflow.engine.controller.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.mts.ip.workflow.engine.dto.Ref;
import ru.mts.ip.workflow.engine.dto.SapConnection;
import ru.mts.ip.workflow.engine.dto.SapInbound;
import ru.mts.ip.workflow.engine.dto.SapInboundStarter;
import ru.mts.ip.workflow.engine.entity.SapStarterDetails;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SapInboundMapperTest {

  private DtoMapper mapper;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mapper = Mappers.getMapper(DtoMapper.class);
    objectMapper = new ObjectMapper();
  }

  @Test
  void testToSapStarterDetails_WithValidInput() {
    // Given
    SapInboundStarter starter = new SapInboundStarter();

    // Create inboundDef with props and connectionDef
    SapInbound inboundDef = new SapInbound();
    SapConnection connectionDef = new SapConnection();

    // Create test properties
    Map<String, JsonNode> inboundProps = new HashMap<>();
    Map<String, JsonNode> connectionProps = new HashMap<>();

    ObjectNode inboundProp1 = objectMapper.createObjectNode();
    inboundProp1.put("key1", "value1");
    inboundProps.put("inboundProp", inboundProp1);

    ObjectNode connectionProp1 = objectMapper.createObjectNode();
    connectionProp1.put("connectionProp1", "connectionProp1Value");
    connectionProps.put("connectionProp", connectionProp1);

    inboundDef.setProps(inboundProps);
    connectionDef.setProps(connectionProps);
    inboundDef.setConnectionDef(connectionDef);

    starter.setInboundDef(inboundDef);
    starter.setInboundRef(new Ref());

    // When
    SapStarterDetails result = mapper.toSapStarterDetails(starter);

    // Then
    assertNotNull(result);
    assertEquals(inboundProps, result.getServerProps());
    assertEquals(connectionProps, result.getDestinationProps());
  }

  @Test
  void testToSapStarterDetails_WithNullInput() {
    // When
    SapStarterDetails result = mapper.toSapStarterDetails(null);

    // Then
    assertNull(result);
  }

  @Test
  void testToSapStarterDetails_WithNullProperties() {
    // Given
    SapInboundStarter starter = new SapInboundStarter();
    SapInbound inboundDef = new SapInbound();
    SapConnection connectionDef = new SapConnection();

    // Set null properties
    inboundDef.setProps(null);
    connectionDef.setProps(null);
    inboundDef.setConnectionDef(connectionDef);

    starter.setInboundDef(inboundDef);
    starter.setInboundRef(new Ref());

    // When
    SapStarterDetails result = mapper.toSapStarterDetails(starter);

    // Then
    assertNotNull(result);
    assertNull(result.getServerProps());
    assertNull(result.getDestinationProps());
  }

  @Test
  void testToSapInboundStarter_WithValidInput() {
    // Given
    SapStarterDetails details = new SapStarterDetails();

    Map<String, JsonNode> serverProps = new HashMap<>();
    Map<String, JsonNode> destinationProps = new HashMap<>();

    ObjectNode serverProp1 = objectMapper.createObjectNode();
    serverProp1.put("serverKey", "serverValue");
    serverProps.put("serverProp", serverProp1);

    ObjectNode destinationProp1 = objectMapper.createObjectNode();
    destinationProp1.put("destinationKey", "destinationValue");
    destinationProps.put("destinationProp", destinationProp1);

    details.setServerProps(serverProps);
    details.setDestinationProps(destinationProps);

    // When
    SapInboundStarter result = mapper.toSapInboundStarter(details);

    // Then
    assertNotNull(result);
    assertNotNull(result.getInboundDef());
    assertNotNull(result.getInboundDef().getConnectionDef());

    assertEquals(serverProps, result.getInboundDef().getProps());
    assertEquals(destinationProps, result.getInboundDef().getConnectionDef().getProps());

    // Verify that inboundRef is ignored (should be null)
    assertNull(result.getInboundRef());
  }

  @Test
  void testToSapInboundStarter_WithNullInput() {
    // When
    SapInboundStarter result = mapper.toSapInboundStarter((SapStarterDetails) null);

    // Then
    assertNull(result);
  }

  @Test
  void testToSapInboundStarter_WithNullProperties() {
    // Given
    SapStarterDetails details = new SapStarterDetails();
    details.setServerProps(null);
    details.setDestinationProps(null);

    // When
    SapInboundStarter result = mapper.toSapInboundStarter(details);

    // Then
    assertNotNull(result);
    assertNotNull(result.getInboundDef());
    assertNotNull(result.getInboundDef().getConnectionDef());

    assertNull(result.getInboundDef().getProps());
    assertNull(result.getInboundDef().getConnectionDef().getProps());
    assertNull(result.getInboundRef());
  }

  @Test
  void testToSapInboundStarter_WithEmptyProperties() {
    // Given
    SapStarterDetails details = new SapStarterDetails();
    details.setServerProps(new HashMap<>());
    details.setDestinationProps(new HashMap<>());

    // When
    SapInboundStarter result = mapper.toSapInboundStarter(details);

    // Then
    assertNotNull(result);
    assertNotNull(result.getInboundDef());
    assertNotNull(result.getInboundDef().getConnectionDef());

    assertTrue(result.getInboundDef().getProps().isEmpty());
    assertTrue(result.getInboundDef().getConnectionDef().getProps().isEmpty());
    assertNull(result.getInboundRef());
  }

}
