package ru.mts.workflowmail.service.integrationflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.PropertySet;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.enumeration.search.LogicalOperator;
import microsoft.exchange.webservices.data.core.service.folder.Folder;
import microsoft.exchange.webservices.data.core.service.item.Item;
import microsoft.exchange.webservices.data.core.service.schema.EmailMessageSchema;
import microsoft.exchange.webservices.data.core.service.schema.ItemSchema;
import microsoft.exchange.webservices.data.search.FindItemsResults;
import microsoft.exchange.webservices.data.search.ItemView;
import microsoft.exchange.webservices.data.search.filter.SearchFilter;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.core.MessageSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import ru.mts.workflowmail.controller.dto.Worker;
import ru.mts.workflowmail.service.dto.MailFilter;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ExchangeMessageSource implements MessageSource<List<Item>>{
  private final Worker worker;
  private final ExchangeService exchangeService;
  private final int maxFetchSize;
  private final ExceptionHolder exceptionHolder;

  @Override
  public Message<List<Item>> receive() {
    var starter = worker.getStarter();
    var starterDetails = starter.getMailConsumer();
    var mailFilter = starterDetails.getMailFilter();
    try {

      var searchFilter = getSearchFilter(mailFilter);
      var itemView = new ItemView(maxFetchSize);

      Folder inbox = Folder.bind(exchangeService, WellKnownFolderName.Inbox);

      FindItemsResults<Item> results =
          exchangeService.findItems(inbox.getId(), searchFilter, itemView);

      if (results.getTotalCount() == 0){
        return null;
      }

      PropertySet propertySet = new PropertySet();
      propertySet.add(EmailMessageSchema.From);
      propertySet.add(EmailMessageSchema.Subject);
      propertySet.add(EmailMessageSchema.Sender);
      propertySet.add(EmailMessageSchema.ToRecipients);
      propertySet.add(EmailMessageSchema.CcRecipients);
      propertySet.add(EmailMessageSchema.DateTimeReceived);
      propertySet.add(EmailMessageSchema.DateTimeSent);
      propertySet.add(EmailMessageSchema.Body);

      List<Item> items = results.getItems();

      for (Item item : items) {
        item.load(propertySet);
      }

      return MessageBuilder.withPayload(items).build();
    } catch (Exception e) {
      log.error("EWS mail error", e);
      exceptionHolder.setException(e);
      throw new RuntimeException("EWS mail error", e);
    }
  }

  private SearchFilter getSearchFilter(MailFilter mailFilter) {
    SearchFilter.SearchFilterCollection searchFilter = new SearchFilter.SearchFilterCollection();
    SearchFilter.IsEqualTo isUnreadFilter =
        new SearchFilter.IsEqualTo(EmailMessageSchema.IsRead, false);
    List<String> subjects = mailFilter.getSubjects();
    if (subjects != null && !subjects.isEmpty()) {
      SearchFilter.SearchFilterCollection subjectFilter =
          new SearchFilter.SearchFilterCollection(LogicalOperator.Or);
      subjects.forEach(subject -> subjectFilter.add(
          new SearchFilter.IsEqualTo(EmailMessageSchema.Subject, subject)));
      searchFilter.add(subjectFilter);
    }
    List<String> senders = mailFilter.getSenders();
    if (senders != null && !senders.isEmpty()) {
      SearchFilter.SearchFilterCollection sendersFilter =
          new SearchFilter.SearchFilterCollection(LogicalOperator.Or);
      senders.forEach(subject -> sendersFilter.add(
          new SearchFilter.IsEqualTo(EmailMessageSchema.Sender, subject)));
      searchFilter.add(sendersFilter);
    }
    OffsetDateTime startMailDateTome = mailFilter.getStartMailDateTime();
    if (startMailDateTome != null) {
      //Exchange Web Services не поддерживает OffsetDateTime
      Date date = Date.from(startMailDateTome.toInstant());
      searchFilter.add(new SearchFilter.IsGreaterThanOrEqualTo(ItemSchema.DateTimeReceived,
          date));
    }

    searchFilter.add(isUnreadFilter);
    return searchFilter;
  }

  @Override
  public IntegrationPatternType getIntegrationPatternType() {
    return MessageSource.super.getIntegrationPatternType();
  }
}
