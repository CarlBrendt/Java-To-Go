# TASK 3

## Title

Admin-managed custom validation suites.

## Goal

Allow admins to extend the validation system with additional test suites for different types of reference projects without changing the built-in baseline validation flow.

## Problem This Stage Solves

One built-in suite will not be sufficient for every service family in `lowcode/`.

Examples:

- CRUD-heavy services;
- messaging services;
- file-processing services;
- integration-heavy services;
- auth-sensitive services.

The platform needs controlled extensibility so new validation logic can be introduced without destabilizing the base user-facing flow.

## Scope

Stage 3 should include:

- admin ability to upload custom validation suites;
- suite metadata and versioning;
- suite activation rules;
- binding suites to project types or specific reference projects;
- diagnostics execution using those suites.

Stage 3 must preserve:

- built-in quick suite stability;
- built-in full suite stability;
- admin-only access for custom suite management.

## Core Design Principle

Custom suites must be an extension layer, not a replacement for the built-in baseline.

The built-in suites remain the stable product metric.

Custom suites serve:

- deeper project-specific validation;
- targeted regression checks;
- support for new project families;
- special integration scenarios.

## Suggested Functional Capabilities

- upload a suite package;
- validate its manifest before activation;
- store suite metadata and version;
- enable or disable a suite;
- mark supported project types;
- choose whether it participates in diagnostics for a given project.

## Suggested Metadata Model

Each custom suite should have metadata such as:

- suite name;
- version;
- supported project types;
- execution entrypoint;
- required environment variables;
- required fixtures or mocks;
- quick/full applicability;
- owner or uploader;
- creation timestamp.

## Suggested Safety Rules

- custom suites are admin-only;
- invalid packages cannot be activated;
- built-in suites cannot be overwritten through admin upload;
- every run must record which suite version was used;
- custom suite failures must be distinguishable from migration failures.

## Suggested Acceptance Criteria

- an admin can upload a new suite package;
- the system validates and stores its metadata;
- the suite can be selected for diagnostics;
- suite version is visible in the resulting validation run;
- built-in suites remain unchanged and available;
- failures in custom suites are reported as validation-system or suite-level issues when appropriate.

## Risks

- arbitrary suite upload can create chaos without a strict manifest contract;
- suite execution may require extra mocks or environment setup;
- version tracking is mandatory or run history becomes unreliable;
- project-type mapping may become inconsistent without governance.

## Recommendation

Do not implement custom suite upload as "upload any jar and run it". Define a controlled suite package contract with explicit metadata and validation rules before enabling this stage.
