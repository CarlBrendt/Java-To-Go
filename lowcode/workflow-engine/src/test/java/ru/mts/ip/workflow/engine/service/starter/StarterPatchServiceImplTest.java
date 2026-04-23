package ru.mts.ip.workflow.engine.service.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ru.mts.ip.workflow.engine.controller.dto.starter.patch.ReqKafkaStarterDetailsPatch;
import ru.mts.ip.workflow.engine.controller.dto.starter.patch.ReqMailStarterDetailsPatch;
import ru.mts.ip.workflow.engine.controller.dto.starter.patch.ReqRabbitmqStarterDetailsPatch;
import ru.mts.ip.workflow.engine.controller.dto.starter.patch.ReqSchedulerStarterDetailsPatch;
import ru.mts.ip.workflow.engine.controller.dto.starter.patch.ReqStarterPatch;
import ru.mts.ip.workflow.engine.entity.MailStarterDetails;
import ru.mts.ip.workflow.engine.entity.StarterEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;


class StarterPatchServiceImplTest {

  private StarterPatchServiceImpl patchService;

  @BeforeEach
  void setUp() {
    patchService = new StarterPatchServiceImpl();
  }

  @Nested
  @DisplayName("Основное обновление StarterEntity")
  class StarterEntityPatchTests {

    @Test
    @DisplayName("Когда патч null - не должен изменять entity")
    void whenPatchIsNull_shouldNotChangeEntity() {
      // Given
      StarterEntity entity = createStarterEntity();
      StarterEntity originalEntity = cloneEntity(entity);

      // When
      patchService.patchStarterEntity(entity, null);

      // Then
      assertThat(entity).usingRecursiveComparison().isEqualTo(originalEntity);
    }

    @Test
    @DisplayName("Когда startDateTime присутствует - должен обновить startDateTime")
    void whenStartDateTimePresent_shouldUpdateStartDateTime() {
      // Given
      StarterEntity entity = createStarterEntity();
      OffsetDateTime newStartDateTime = OffsetDateTime.now().plusDays(1);
      ReqStarterPatch patch = new ReqStarterPatch();
      patch.setStartDateTime(Optional.of(newStartDateTime));

      // When
      patchService.patchStarterEntity(entity, patch);

      // Then
      assertThat(entity.getStartDateTime()).isEqualTo(newStartDateTime);
    }

    @Test
    @DisplayName("Когда startDateTime empty - должен установить null")
    void whenStartDateTimeEmpty_shouldSetNull() {
      // Given
      StarterEntity entity = createStarterEntity();
      entity.setStartDateTime(OffsetDateTime.now());
      ReqStarterPatch patch = new ReqStarterPatch();
      patch.setStartDateTime(Optional.empty());

      // When
      patchService.patchStarterEntity(entity, patch);

      // Then
      assertThat(entity.getStartDateTime()).isNull();
    }

    @Test
    @DisplayName("Когда startDateTime null - не должен изменять")
    void whenStartDateTimeNull_shouldNotChange() {
      // Given
      StarterEntity entity = createStarterEntity();
      OffsetDateTime originalStartDateTime = entity.getStartDateTime();
      ReqStarterPatch patch = new ReqStarterPatch();
      patch.setStartDateTime(null);

      // When
      patchService.patchStarterEntity(entity, patch);

      // Then
      assertThat(entity.getStartDateTime()).isEqualTo(originalStartDateTime);
    }
  }

  @Nested
  @DisplayName("Обновление Mail Details")
  class MailDetailsPatchTests {

    @Test
    @DisplayName("Когда mailDetails присутствует - должен обновить mail details")
    void whenMailDetailsPresent_shouldUpdateMailDetails() {
      // Given
      StarterEntity entity = createStarterEntity();
      ReqStarterPatch patch = new ReqStarterPatch();

      ReqMailStarterDetailsPatch mailPatch = new ReqMailStarterDetailsPatch();
      mailPatch.setOutputTemplate(Optional.of(createJsonNode("{\"template\": \"test\"}")));

      patch.setMailConsumer(Optional.of(mailPatch));

      // When
      patchService.patchStarterEntity(entity, patch);

      // Then
      assertAll(
          () -> assertThat(entity.getMailConsumer()).isNotNull(),
          () -> assertThat(entity.getMailConsumer().getOutputTemplate())
              .isEqualTo(createJsonNode("{\"template\": \"test\"}"))
      );
    }

    @Test
    @DisplayName("Когда mailDetails empty - должен установить null")
    void whenMailDetailsEmpty_shouldSetNull() {
      // Given
      StarterEntity entity = createStarterEntity();
      entity.setMailConsumer(new MailStarterDetails());
      ReqStarterPatch patch = new ReqStarterPatch();
      patch.setMailConsumer(Optional.empty());

      // When
      patchService.patchStarterEntity(entity, patch);

      // Then
      assertThat(entity.getMailConsumer()).isNull();
    }

    @Test
    @DisplayName("Когда mailDetails null - не должен изменять")
    void whenMailDetailsNull_shouldNotChange() {
      // Given
      StarterEntity entity = createStarterEntity();
      MailStarterDetails originalDetails = new MailStarterDetails();
      entity.setMailConsumer(originalDetails);
      ReqStarterPatch patch = new ReqStarterPatch();
      patch.setMailConsumer(null);

      // When
      patchService.patchStarterEntity(entity, patch);

      // Then
      assertThat(entity.getMailConsumer()).isSameAs(originalDetails);
    }

    @Test
    @DisplayName("Когда connectionDef присутствует - должен обновить connection")
    void whenConnectionDefPresent_shouldUpdateConnection() {
      // Given
      StarterEntity entity = createStarterEntity();
      ReqStarterPatch patch = new ReqStarterPatch();

      ReqMailStarterDetailsPatch mailPatch = new ReqMailStarterDetailsPatch();
      ReqMailStarterDetailsPatch.MailConnectionPatch connectionPatch =
          new ReqMailStarterDetailsPatch.MailConnectionPatch();
      connectionPatch.setHost(Optional.of("mail.example.com"));
      connectionPatch.setPort(Optional.of(993));
      connectionPatch.setProtocol(Optional.of("imaps"));

      mailPatch.setConnectionDef(Optional.of(connectionPatch));
      patch.setMailConsumer(Optional.of(mailPatch));

      // When
      patchService.patchStarterEntity(entity, patch);

      // Then
      assertAll(
          () -> assertThat(entity.getMailConsumer()).isNotNull(),
          () -> assertThat(entity.getMailConsumer().getConnectionDef()).isNotNull(),
          () -> assertThat(entity.getMailConsumer().getConnectionDef().getHost())
              .isEqualTo("mail.example.com"),
          () -> assertThat(entity.getMailConsumer().getConnectionDef().getPort())
              .isEqualTo(993),
          () -> assertThat(entity.getMailConsumer().getConnectionDef().getProtocol())
              .isEqualTo("imaps")
      );
    }

    @Test
    @DisplayName("Когда mailFilter присутствует - должен обновить фильтр")
    void whenMailFilterPresent_shouldUpdateFilter() {
      // Given
      StarterEntity entity = createStarterEntity();
      ReqStarterPatch patch = new ReqStarterPatch();

      ReqMailStarterDetailsPatch mailPatch = new ReqMailStarterDetailsPatch();
      ReqMailStarterDetailsPatch.ReqMailFilterPatch filterPatch =
          new ReqMailStarterDetailsPatch.ReqMailFilterPatch();
      filterPatch.setSenders(Optional.of(List.of("sender@example.com")));
      filterPatch.setSubjects(Optional.of(List.of("Important", "Urgent")));
      filterPatch.setStartMailDateTime(Optional.of(OffsetDateTime.now().minusDays(1)));

      mailPatch.setMailFilter(Optional.of(filterPatch));
      patch.setMailConsumer(Optional.of(mailPatch));

      // When
      patchService.patchStarterEntity(entity, patch);

      // Then
      assertAll(
          () -> assertThat(entity.getMailConsumer()).isNotNull(),
          () -> assertThat(entity.getMailConsumer().getMailFilter()).isNotNull(),
          () -> assertThat(entity.getMailConsumer().getMailFilter().senders())
              .containsExactly("sender@example.com"),
          () -> assertThat(entity.getMailConsumer().getMailFilter().subjects())
              .containsExactly("Important", "Urgent"),
          () -> assertThat(entity.getMailConsumer().getMailFilter().startMailDateTime())
              .isNotNull()
      );
    }
  }

  @Nested
  @DisplayName("Обновление Kafka Details")
  class KafkaDetailsPatchTests {

    @Test
    @DisplayName("Когда все поля присутствуют - должен обновить все поля")
    void whenAllFieldsPresent_shouldUpdateAllFields() {
      // Given
      StarterEntity entity = createStarterEntity();
      ReqKafkaStarterDetailsPatch kafkaPatch = new ReqKafkaStarterDetailsPatch();
      kafkaPatch.setTopic(Optional.of("test-topic"));
      kafkaPatch.setConsumerGroupId(Optional.of("test-group"));
      kafkaPatch.setOutputTemplate(Optional.of(createJsonNode("{\"kafka\": \"test\"}")));

      ReqStarterPatch patch = new ReqStarterPatch();
      patch.setKafkaConsumer(Optional.of(kafkaPatch));

      // When
      patchService.patchStarterEntity(entity, patch);

      // Then
      assertAll(
          () -> assertThat(entity.getKafkaConsumer()).isNotNull(),
          () -> assertThat(entity.getKafkaConsumer().getTopic()).isEqualTo("test-topic"),
          () -> assertThat(entity.getKafkaConsumer().getConsumerGroupId()).isEqualTo("test-group"),
          () -> assertThat(entity.getKafkaConsumer().getOutputTemplate())
              .isEqualTo(createJsonNode("{\"kafka\": \"test\"}"))
      );
    }

    @Test
    @DisplayName("Когда connectionDef присутствует - должен обновить connection")
    void whenConnectionDefPresent_shouldUpdateConnection() {
      // Given
      StarterEntity entity = createStarterEntity();
      ReqKafkaStarterDetailsPatch kafkaPatch = new ReqKafkaStarterDetailsPatch();

      ReqKafkaStarterDetailsPatch.ReqKafkaConnectionPatch connectionPatch =
          new ReqKafkaStarterDetailsPatch.ReqKafkaConnectionPatch();
      connectionPatch.setBootstrapServers(Optional.of("kafka1:9092,kafka2:9092"));
      connectionPatch.setTags(Optional.of(List.of("prod", "eu")));

      kafkaPatch.setConnectionDef(Optional.of(connectionPatch));
      kafkaPatch.setTopic(Optional.of("test-topic"));

      ReqStarterPatch patch = new ReqStarterPatch();
      patch.setKafkaConsumer(Optional.of(kafkaPatch));

      // When
      patchService.patchStarterEntity(entity, patch);

      // Then
      assertAll(
          () -> assertThat(entity.getKafkaConsumer()).isNotNull(),
          () -> assertThat(entity.getKafkaConsumer().getConnectionDef()).isNotNull(),
          () -> assertThat(entity.getKafkaConsumer().getConnectionDef().getBootstrapServers())
              .isEqualTo("kafka1:9092,kafka2:9092"),
          () -> assertThat(entity.getKafkaConsumer().getConnectionDef().getTags())
              .containsExactly("prod", "eu")
      );
    }
  }

  @Nested
  @DisplayName("Обновление RabbitMQ Details")
  class RabbitmqDetailsPatchTests {

    @Test
    @DisplayName("Когда connection присутствует - должен обновить connection")
    void whenConnectionPresent_shouldUpdateConnection() {
      // Given
      StarterEntity entity = createStarterEntity();
      ReqRabbitmqStarterDetailsPatch rabbitmqPatch = new ReqRabbitmqStarterDetailsPatch();

      ReqRabbitmqStarterDetailsPatch.ReqRabbitmqConnectionPatch connectionPatch =
          new ReqRabbitmqStarterDetailsPatch.ReqRabbitmqConnectionPatch();
      connectionPatch.setUserName(Optional.of("rabbit-user"));
      connectionPatch.setUserPass(Optional.of("password123"));
      connectionPatch.setVirtualHost(Optional.of("/test"));
      connectionPatch.setAddresses(Optional.of(List.of("rabbit1:5672", "rabbit2:5672")));

      rabbitmqPatch.setConnectionDef(Optional.of(connectionPatch));
      rabbitmqPatch.setQueue(Optional.of("test-queue"));

      ReqStarterPatch patch = new ReqStarterPatch();
      patch.setRabbitmqConsumer(Optional.of(rabbitmqPatch));

      // When
      patchService.patchStarterEntity(entity, patch);

      // Then
      assertAll(
          () -> assertThat(entity.getRabbitmqConsumer()).isNotNull(),
          () -> assertThat(entity.getRabbitmqConsumer().getQueue()).isEqualTo("test-queue"),
          () -> assertThat(entity.getRabbitmqConsumer().getConnectionDef()).isNotNull(),
          () -> assertThat(entity.getRabbitmqConsumer().getConnectionDef().getUserName())
              .isEqualTo("rabbit-user"),
          () -> assertThat(entity.getRabbitmqConsumer().getConnectionDef().getVirtualHost())
              .isEqualTo("/test"),
          () -> assertThat(entity.getRabbitmqConsumer().getConnectionDef().getAddresses())
              .containsExactly("rabbit1:5672", "rabbit2:5672")
      );
    }
  }

  @Nested
  @DisplayName("Обновление Scheduler Details")
  class SchedulerDetailsPatchTests {

    @Test
    @DisplayName("Когда cron присутствует - должен обновить cron")
    void whenCronPresent_shouldUpdateCron() {
      // Given
      StarterEntity entity = createStarterEntity();
      ReqSchedulerStarterDetailsPatch schedulerPatch = new ReqSchedulerStarterDetailsPatch();

      ReqSchedulerStarterDetailsPatch.ReqCronPatch cronPatch =
          new ReqSchedulerStarterDetailsPatch.ReqCronPatch();
      cronPatch.setMinute(Optional.of("0"));
      cronPatch.setHour(Optional.of("12"));
      cronPatch.setDayOfMonth(Optional.of("15"));
      cronPatch.setMonth(Optional.of("*"));
      cronPatch.setDayOfWeek(Optional.of("MON-FRI"));

      schedulerPatch.setCron(Optional.of(cronPatch));
      schedulerPatch.setType(Optional.of("cron"));

      ReqStarterPatch patch = new ReqStarterPatch();
      patch.setScheduler(Optional.of(schedulerPatch));

      // When
      patchService.patchStarterEntity(entity, patch);

      // Then
      assertAll(
          () -> assertThat(entity.getScheduler()).isNotNull(),
          () -> assertThat(entity.getScheduler().getType()).isEqualTo("cron"),
          () -> assertThat(entity.getScheduler().getCron()).isNotNull(),
          () -> assertThat(entity.getScheduler().getCron().getMinute()).isEqualTo("0"),
          () -> assertThat(entity.getScheduler().getCron().getHour()).isEqualTo("12"),
          () -> assertThat(entity.getScheduler().getCron().getDayOfMonth()).isEqualTo("15"),
          () -> assertThat(entity.getScheduler().getCron().getMonth()).isEqualTo("*"),
          () -> assertThat(entity.getScheduler().getCron().getDayOfWeek()).isEqualTo("MON-FRI")
      );
    }

    @Test
    @DisplayName("Когда simple присутствует - должен обновить simple")
    void whenSimplePresent_shouldUpdateSimple() {
      // Given
      StarterEntity entity = createStarterEntity();
      ReqSchedulerStarterDetailsPatch schedulerPatch = new ReqSchedulerStarterDetailsPatch();

      ReqSchedulerStarterDetailsPatch.ReqSimpleDurationPatch simplePatch =
          new ReqSchedulerStarterDetailsPatch.ReqSimpleDurationPatch();
      simplePatch.setDuration(Optional.of(java.time.Duration.ofMinutes(30)));

      schedulerPatch.setSimple(Optional.of(simplePatch));
      schedulerPatch.setType(Optional.of("simple"));

      ReqStarterPatch patch = new ReqStarterPatch();
      patch.setScheduler(Optional.of(schedulerPatch));

      // When
      patchService.patchStarterEntity(entity, patch);

      // Then
      assertAll(
          () -> assertThat(entity.getScheduler()).isNotNull(),
          () -> assertThat(entity.getScheduler().getType()).isEqualTo("simple"),
          () -> assertThat(entity.getScheduler().getSimple()).isNotNull(),
          () -> assertThat(entity.getScheduler().getSimple().getDuration())
              .isEqualTo(java.time.Duration.ofMinutes(30))
      );
    }
  }

  @Nested
  @DisplayName("Множественное обновление")
  class MultipleDetailsPatchTests {

    @Test
    @DisplayName("Когда несколько details присутствуют - должен обновить все")
    void whenMultipleDetailsPresent_shouldUpdateAll() {
      // Given
      StarterEntity entity = createStarterEntity();
      ReqStarterPatch patch = new ReqStarterPatch();

      // Mail details
      ReqMailStarterDetailsPatch mailPatch = new ReqMailStarterDetailsPatch();
      mailPatch.setOutputTemplate(Optional.of(createJsonNode("{\"mail\": \"test\"}")));
      patch.setMailConsumer(Optional.of(mailPatch));

      // Kafka details
      ReqKafkaStarterDetailsPatch kafkaPatch = new ReqKafkaStarterDetailsPatch();
      kafkaPatch.setTopic(Optional.of("test-topic"));
      patch.setKafkaConsumer(Optional.of(kafkaPatch));

      // Scheduler details
      ReqSchedulerStarterDetailsPatch schedulerPatch = new ReqSchedulerStarterDetailsPatch();
      schedulerPatch.setType(Optional.of("cron"));
      patch.setScheduler(Optional.of(schedulerPatch));

      // When
      patchService.patchStarterEntity(entity, patch);

      // Then
      assertAll(
          () -> assertThat(entity.getMailConsumer()).isNotNull(),
          () -> assertThat(entity.getKafkaConsumer()).isNotNull(),
          () -> assertThat(entity.getScheduler()).isNotNull(),
          () -> assertThat(entity.getMailConsumer().getOutputTemplate())
              .isEqualTo(createJsonNode("{\"mail\": \"test\"}")),
          () -> assertThat(entity.getKafkaConsumer().getTopic()).isEqualTo("test-topic"),
          () -> assertThat(entity.getScheduler().getType()).isEqualTo("cron")
      );
    }
  }

  // Helper methods
  private StarterEntity createStarterEntity() {
    StarterEntity entity = new StarterEntity();
    entity.setId(UUID.randomUUID());
    entity.setType("TEST_TYPE");
    entity.setName("Test Starter");
    entity.setTenantId("test-tenant");
    entity.setDesiredStatus("STARTED");
    entity.setActualStatus("RUNNING");
    entity.setCreateTime(OffsetDateTime.now());
    entity.setChangeTime(OffsetDateTime.now());
    return entity;
  }

  private StarterEntity cloneEntity(StarterEntity original) {
    StarterEntity clone = new StarterEntity();
    clone.setId(original.getId());
    clone.setType(original.getType());
    clone.setName(original.getName());
    clone.setTenantId(original.getTenantId());
    clone.setDesiredStatus(original.getDesiredStatus());
    clone.setActualStatus(original.getActualStatus());
    clone.setCreateTime(original.getCreateTime());
    clone.setChangeTime(original.getChangeTime());
    clone.setStartDateTime(original.getStartDateTime());
    clone.setEndDateTime(original.getEndDateTime());
    return clone;
  }

  private JsonNode createJsonNode(String json) {
    try {
      return new ObjectMapper().readTree(json);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
