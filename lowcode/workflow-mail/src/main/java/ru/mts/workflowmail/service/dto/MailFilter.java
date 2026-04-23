package ru.mts.workflowmail.service.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class MailFilter {
  private List<String> senders;
  private List<String> subjects;
  private OffsetDateTime startMailDateTime;
}
