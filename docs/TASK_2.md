# TASK 2

## Title

Role separation between user quick check and admin diagnostics.

## Goal

Split the validation experience into two different product layers:

- a fast and simple validation result for the regular user;
- a deeper diagnostics workflow for the admin or engineer.

## Problem This Stage Solves

Once Stage 1 exists, every validation run could expose too much internal detail. That is not desirable for normal product usage.

Regular users need a fast confidence signal. Engineers need deep failure analysis. These are different jobs and should not share the same interface by default.

## Scope

Stage 2 should include:

- quick validation flow for the regular user;
- diagnostics-oriented detailed view for the admin;
- run history for admin;
- stage-level breakdown for admin;
- artifact and mismatch access for admin.

Stage 2 should not yet include:

- custom suite uploads;
- broad suite management workflows.

## User Experience

The user should get:

- one obvious action to trigger validation;
- concise progress stages;
- fast summary result;
- parity percentage;
- no raw logs by default.

Expected user summary:

- overall status;
- parity percentage;
- count of passed and failed checks;
- short recommendation such as regenerate, inspect diagnostics, or continue.

## Admin Experience

The admin should get:

- access to the same base validation runs;
- detailed stage timeline;
- mismatch summaries by scenario or endpoint;
- build and startup failure details;
- access to artifacts and logs;
- history of previous runs.

## High-Level Deliverables

- user-facing quick validation screen or section;
- admin-facing diagnostics screen;
- backend support for richer run metadata;
- stage-level result model;
- detailed report model;
- run history retrieval.

## Suggested Data To Show In Admin Diagnostics

- reference project name;
- selected suite name;
- run timestamps;
- stage statuses;
- failure type;
- parity percentage;
- passed/failed scenario counts;
- mismatch examples;
- build report;
- generated artifacts;
- raw logs as optional drill-down.

## Suggested Acceptance Criteria

- the regular user can understand the result without opening logs;
- the admin can diagnose where the run failed;
- diagnostics clearly separates generation, build, startup, and mismatch failures;
- run history can be reviewed after completion;
- the same built-in base suite remains available to both user and admin.

## Risks

- user and admin interfaces may drift unless they share the same run model;
- too much admin detail in the user flow will hurt usability;
- too little detail in diagnostics will make engineers return to raw logs immediately.

## Recommendation

Keep Quick Check intentionally opinionated and minimal. Push depth into diagnostics rather than making the main user flow configurable.
