# Fineract Custom Modules


> **Note:** This document is a summary of the official Fineract documentation for custom modules. For the complete and most up-to-date information, please refer to the [official documentation](https://fineract.apache.org/docs/current/#_custom_modules).
This document provides a guide for creating custom modules to extend or override Fineract's default functionality, based on the official documentation.

> **Note:** Currently, custom modules are a proof of concept feature in Fineract.

## Introduction

Custom modules in Fineract allow for easy customization of services. The approach is designed to work with future "clean room" module guidelines and avoids the need for major refactorings. The key principle is a folder structure convention that prevents extensions from clashing with Fineract's internal code, making downstream forks easier to sync. Editing core sources in `fineract-provider` is not recommended.

Currently, the primary services prepared for overriding are `NoteReadPlatformService` and `NoteWritePlatformService`. For other services, it is recommended to consult the developer mailing list.

## Benefits of the Convention

Following the recommended folder structure provides significant advantages:
-   You do not need to edit `settings.gradle` to include your new custom modules; they are picked up automatically.
-   Your modules will be automatically included in custom Fineract Docker image builds.

## Folder Structure and Instructions

The process involves creating a specific directory structure and configuration files.

### Step 1: Create Company and Domain Folders

1.  Inside the `custom` directory, create a folder named after your company or organization (e.g., `acme`). This acts as a namespace to prevent clashes with modules from other organizations.
2.  Inside your company folder, create a folder for the category or domain your module targets (e.g., `note`, `loan`, `client`).

An example structure would be `custom/acme/note`.

### Step 2: Set Up Module Libraries

Within your domain folder, create subdirectories for your actual libraries. Common libraries include:
-   `service`: For your custom service implementations that extend or replace existing Fineract services.
-   `core`: If you need to add additional Data Transfer Objects (DTOs) or other core components.
-   `starter`: A mandatory Spring Boot auto-configuration library to ensure your module is integrated seamlessly.

### Step 3: Configure the Build Files

Each module library (`service`, `core`, `starter`) must have its own `build.gradle` and `dependencies.gradle` files.

A typical `build.gradle` would define the project's description, group, and archive name. A `dependencies.gradle` file would declare dependencies on other Fineract modules like `fineract-core` and `fineract-provider`.

**Important:** Do not add your custom module as a dependency in `fineract-provider`'s `dependencies.gradle` file, as this will create a circular dependency and fail the build.

### Step 4: Implement the Starter and Auto-Configuration

The `starter` library is crucial for integrating your module. It uses Spring Boot's auto-configuration capabilities.
1.  Create a Java configuration class annotated with `@Configuration`. This class will define the beans for your custom services. To replace a default Fineract service, you can use the `@ConditionalOnMissingBean` annotation on your bean definition. This tells Spring to only create your bean if one doesn't already exist, allowing your custom module to override the default one.
2.  Create a file at `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
3.  In this file, add the fully qualified name of your auto-configuration class, for example: `com.acme.fineract.portfolio.note.starter.AcmeNoteAutoConfiguration`.

This setup allows Fineract to automatically discover and load your custom module without any manual configuration.

## Custom Business Steps for Close of Business (COB)

You can also add custom business steps to Fineract's default COB processing.
1.  Create a custom module as described above (e.g., `custom/acme/steps`).
2.  Create a class that implements the `org.apache.fineract.cob.COBBusinessStep` interface.
3.  Provide a custom database migration script to add the necessary information about your new business step into the `m_batch_business_steps` table.

## Custom Database Migration

If your customizations require database changes, you can add your own migration scripts by following these conventions:

1.  **Create Changelog Folders**: In one of your module's `resources` folders (the `starter` library is recommended), create the directory path `db/custom-changelog`.
2.  **Create a Changelog File**: Inside `db/custom-changelog`, create an XML changelog file (e.g., `changelog-acme-note.xml`). It's best to use a consistent naming convention to avoid classpath conflicts.
3.  **Create a 'parts' Folder**: Inside `db/custom-changelog`, create a `parts` folder. This is where your specific changelog scripts will be placed.

By following this structure, Fineract will automatically pick up and run your custom database migration scripts.

## Deploying Custom Modules

Once your custom modules are built, they need to be deployed into your Fineract instance.

### JAR Deployment

If you are running Fineract from the Spring Boot JAR file, you can simply drop your custom module JAR files into Fineract's `libs` folder. Dynamic loading of external JARs has been supported since Fineract version 1.5.0.

### Docker Deployment

For convenience, a separate Docker image module is provided that automatically includes your custom modules. You can find this in the `custom/docker` directory.

To build the custom Docker image, run the following command from the Fineract root directory:

```bash
./gradlew :custom:docker:jibDockerBuild
```

The resulting Docker image, which includes your custom modules, will be named `fineract-custom`.

## Custom Batch Jobs

Fineract provides extension points to define custom batch jobs using the module system. This allows you to define and configure custom jobs that run alongside Fineract's default batch jobs to extend or customize batch processing.

Batch jobs in Fineract are implemented using **Spring Batch**, and automatic scheduling is handled by the **Quartz Scheduler**. It is also possible to trigger batch jobs via regular APIs.

### Defining a Custom Job

To define a custom batch job, follow these steps:

1.  **Create a Custom Module**: First, create a custom module for your job (e.g., `custom/acme/loan/job`), following the standard instructions for creating a custom module.
2.  **Create a Job Configuration**: Create a job configuration class to register the job, its steps, and its tasklet with the job builder factory (e.g., `com.acme.fineract.loan.job.AcmeNoopJobConfiguration`).
3.  **Create a Tasklet**: Create a tasklet class that contains the execution functionality for the job (e.g., `com.acme.fineract.loan.job.AcmeNoopJobTasklet`).
4.  **Provide a Custom Database Migration**: Provide a custom database migration to add the necessary information about your job in the `job` table.
