package ru.mts.workflowmail.service.integrationflow;

import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.FromTerm;
import jakarta.mail.search.OrTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import jakarta.mail.search.SubjectTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.mail.SearchTermStrategy;
import ru.mts.workflowmail.service.dto.MailFilter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class SearchTermStrategyImpl implements SearchTermStrategy {

  private final MailFilter mailFilter;

  @Override
  public SearchTerm generateSearchTerm(Flags supportedFlags, Folder folder) {
    List<SearchTerm> andTerms = new ArrayList<>();

    //только непрочитанные
    var flags = new Flags();
    flags.add(Flags.Flag.SEEN);
    var unseenTerm = new FlagTerm(flags, false);
    andTerms.add(unseenTerm);

    if (mailFilter != null) {
      //отправитель
      var senders = mailFilter.getSenders();
      if (senders != null && !senders.isEmpty()) {
        FromTerm[] fromTerms = senders.stream().map(sender -> {
          try {
            return new FromTerm(new InternetAddress(sender));
          } catch (AddressException e) {
            log.error("Unable to parse InternetAddress from {}", sender);
            throw new RuntimeException(e);
          }
        }).toArray(FromTerm[]::new);
        andTerms.add(new OrTerm(fromTerms));
      }
      //письма старше или равные заданной даты
      var startDate = mailFilter.getStartMailDateTime();
      if (startDate != null) {
        andTerms.add(new ReceivedDateTerm(ComparisonTerm.GE, Date.from(startDate.toInstant())));
      }
      //тема письма
      var subjects = mailFilter.getSubjects();
      if (subjects != null && !subjects.isEmpty()) {
        SubjectTerm[] subjectTerms =
            subjects.stream().map(SubjectTerm::new).toArray(SubjectTerm[]::new);
        andTerms.add(new OrTerm(subjectTerms));
      }
    }

    return new AndTerm(andTerms.toArray(new SearchTerm[0]));
  }
}
