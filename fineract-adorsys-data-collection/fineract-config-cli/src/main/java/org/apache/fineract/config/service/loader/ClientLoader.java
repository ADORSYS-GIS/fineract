package org.apache.fineract.config.service.loader;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.PlannedChange;
import org.apache.fineract.config.model.client.Client;
import org.apache.fineract.config.provider.FineractApiClient;
import org.apache.fineract.config.service.ChangeDetectionService;
import org.apache.fineract.config.util.FineractEnumMapper;
import org.apache.fineract.config.util.RequestBuilder;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for clients.
 *
 * <p>Creates individual client records in the system.
 *
 * <p>API Endpoint: POST /api/v1/clients
 */
@Slf4j
@Component
public class ClientLoader {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd MMMM yyyy");
  private static final String LOCALE = "en";
  private static final String DATE_FORMAT = "dd MMMM yyyy";

  private final FineractApiClient apiClient;
  private final ChangeDetectionService changeDetectionService;

  public ClientLoader(FineractApiClient apiClient, ChangeDetectionService changeDetectionService) {
    this.apiClient = apiClient;
    this.changeDetectionService = changeDetectionService;
  }

  /**
   * Loads clients.
   *
   * @param clients list of clients
   * @param context import context
   * @param result import result
   */
  public void load(List<Client> clients, ImportContext context, ImportResult result) {
    log.debug("Loading {} clients", clients.size());

    for (Client client : clients) {
      try {
        loadSingleClient(client, context, result);
      } catch (Exception ex) {
        log.error("Failed to load client '{}': {}", client.getExternalId(), ex.getMessage());
        result.recordEntity("client", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Loads a single client.
   *
   * @param client client to load
   * @param context import context
   * @param result import result
   */
  private void loadSingleClient(Client client, ImportContext context, ImportResult result) {
    log.debug("Loading client: {}", client.getExternalId());

    Long clientId = null;
    Map<String, Object> existingClient = null;

    // Check if client already exists by external ID
    if (client.getExternalId() != null) {
      try {
        // Search endpoint returns a paged response with pageItems array
        Map<String, Object> searchResult =
            apiClient.get("/api/v1/clients?externalId=" + client.getExternalId(), Map.class);

        // Check if result is a paged response
        if (searchResult != null && searchResult.containsKey("pageItems")) {
          @SuppressWarnings("unchecked")
          List<Map<String, Object>> pageItems =
              (List<Map<String, Object>>) searchResult.get("pageItems");
          if (pageItems != null && !pageItems.isEmpty()) {
            existingClient = pageItems.get(0);
            clientId = ((Number) existingClient.get("id")).longValue();
          }
        } else if (searchResult != null && searchResult.containsKey("id")) {
          // Direct client object returned
          existingClient = searchResult;
          clientId = ((Number) existingClient.get("id")).longValue();
        }

        if (clientId != null) {
          log.debug("Client already exists: {} (ID: {})", client.getExternalId(), clientId);
        }
      } catch (Exception ex) {
        // Client not found, proceed with creation
        log.debug("Client not found by external ID, will create new client");
      }
    }

    if (existingClient != null && clientId != null) {
      // Client exists - check if we can update
      if (!changeDetectionService.canUpdate("client", existingClient)) {
        log.warn(
            "Client {} is activated and cannot be fully updated. Only limited fields can be changed.",
            client.getExternalId());
      }

      // Build update request with appropriate fields based on activation status
      Map<String, Object> proposedData = buildUpdateRequest(client, context, existingClient);

      // Detect changes
      Map<String, PlannedChange.FieldChange> changes =
          changeDetectionService.detectChangesForEntityType("client", existingClient, proposedData);

      if (!changes.isEmpty()) {
        // Update client
        log.info("Updating client: {} ({} fields changed)", client.getExternalId(), changes.size());
        apiClient.put("/api/v1/clients/" + clientId, proposedData, Map.class);
        result.recordEntity("client", ImportResult.EntityAction.UPDATED);
      } else {
        log.debug("Client unchanged: {}", client.getExternalId());
        result.recordEntity("client", ImportResult.EntityAction.UNCHANGED);
      }
    } else {
      // Create new client
      Map<String, Object> request = buildRequest(client, context);
      Map<String, Object> response = apiClient.post("/api/v1/clients", request, Map.class);
      clientId = ((Number) response.get("clientId")).longValue();

      log.info(
          "Client created: {} {} {} (ID: {})",
          client.getFirstName(),
          client.getMiddleName() != null ? client.getMiddleName() + " " : "",
          client.getLastName(),
          clientId);
      result.recordEntity("client", ImportResult.EntityAction.CREATED);

      // Activate client if requested
      if (Boolean.TRUE.equals(client.getActive()) && client.getActivationDate() != null) {
        activateClient(clientId, client, context, result);
      }
    }

    // Store for reference
    if (client.getExternalId() != null) {
      context.registerEntity("client", client.getExternalId(), clientId);
    }
  }

  /**
   * Builds API request for client.
   *
   * @param client client
   * @param context import context
   * @return request map
   */
  private Map<String, Object> buildRequest(Client client, ImportContext context) {
    RequestBuilder builder = RequestBuilder.create().withLocaleDateFormat();

    // Basic information
    builder.put("firstname", client.getFirstName()).put("lastname", client.getLastName());

    builder.putIfNotNull("middlename", client.getMiddleName());
    builder.putIfNotNull("externalId", client.getExternalId());

    // Resolve office
    if (client.getOfficeName() != null) {
      Long officeId = context.resolveEntityId("office", client.getOfficeName());
      if (officeId != null) {
        builder.put("officeId", officeId);
      } else {
        throw new IllegalStateException(
            "Office '" + client.getOfficeName() + "' not found for client");
      }
    }

    // Resolve staff
    if (client.getStaffName() != null) {
      Long staffId = context.resolveEntityId("staff", client.getStaffName());
      if (staffId != null) {
        builder.put("staffId", staffId);
      }
    }

    // Personal details
    if (client.getDateOfBirth() != null) {
      builder.put("dateOfBirth", client.getDateOfBirth().format(DATE_FORMATTER));
    }

    // Resolve IDs using labels from YAML, ignoring hardcoded IDs which are often incorrect for the
    // current DB state.
    // 1. Gender
    String genderLabel = client.getGender();
    if (genderLabel != null) {
      builder.put("genderId", FineractEnumMapper.mapGender(genderLabel));
    }

    // 2. Client Type
    String typeLabel = client.getClientType();
    if (typeLabel != null) {
      Long clientTypeId = context.resolveEntityId("codeValue", "ClientType:" + typeLabel);
      if (clientTypeId != null) {
        builder.put("clientTypeId", clientTypeId);
      } else {
        log.warn("ClientType label '{}' not found in cache", typeLabel);
      }
    } else if (client.getClientTypeId() != null) {
      builder.put("clientTypeId", client.getClientTypeId());
    }

    // 3. Client Classification
    String classLabel = client.getClientClassification();
    if (classLabel != null) {
      Long classId = context.resolveEntityId("codeValue", "ClientClassification:" + classLabel);
      if (classId != null) {
        builder.put("clientClassificationId", classId);
      } else {
        log.warn("ClientClassification label '{}' not found in cache", classLabel);
      }
    } else if (client.getClientClassificationId() != null) {
      builder.put("clientClassificationId", client.getClientClassificationId());
    }

    builder.putIfNotNull("mobileNo", client.getMobileNo());
    builder.putIfNotNull("emailAddress", client.getEmailAddress());

    // Legal form is required by Fineract API: 1 = Person, 2 = Entity
    if (client.getLegalFormId() != null) {
      builder.put("legalFormId", client.getLegalFormId());
    } else {
      // Default to Person (Individual) if not specified
      builder.put("legalFormId", 1);
    }

    // Active flag determines if we activate immediately or just submit
    builder.put("active", Boolean.TRUE.equals(client.getActive()));

    if (Boolean.TRUE.equals(client.getActive()) && client.getActivationDate() != null) {
      builder.put("activationDate", client.getActivationDate().format(DATE_FORMATTER));
    }

    // Address information
    if (client.getAddress() != null) {
      RequestBuilder addressBuilder = RequestBuilder.create();
      addressBuilder.putIfNotNull("addressLine1", client.getAddress().getAddressLine1());
      addressBuilder.putIfNotNull("addressLine2", client.getAddress().getAddressLine2());
      addressBuilder.putIfNotNull("city", client.getAddress().getCity());
      addressBuilder.putIfNotNull("postalCode", client.getAddress().getPostalCode());

      // Resolve IDs for address fields, prioritizing labels to avoid using incorrect hardcoded IDs
      String addrTypeLabel = client.getAddress().getAddressType();
      if (addrTypeLabel != null) {
        Long typeId = context.resolveEntityId("codeValue", "AddressType:" + addrTypeLabel);
        if (typeId != null) {
          addressBuilder.put("addressTypeId", typeId);
        } else {
          log.warn("AddressType label '{}' not found in cache", addrTypeLabel);
        }
      } else if (client.getAddress().getAddressTypeId() != null) {
        addressBuilder.put("addressTypeId", client.getAddress().getAddressTypeId());
      }

      String stateLabel = client.getAddress().getStateProvince();
      if (stateLabel != null) {
        Long stateId = context.resolveEntityId("codeValue", "State:" + stateLabel);
        if (stateId != null) {
          addressBuilder.put("stateProvinceId", stateId);
        }
      }

      String countryLabel = client.getAddress().getCountry();
      if (countryLabel != null) {
        Long countryId = context.resolveEntityId("codeValue", "Country:" + countryLabel);
        if (countryId != null) {
          addressBuilder.put("countryId", countryId);
        }
      }

      builder.put("address", List.of(addressBuilder.build()));
    }

    return builder.build();
  }

  /**
   * Builds API request for client update.
   *
   * <p>Respects activation status - if client is active, only updateable fields are included.
   *
   * @param client client
   * @param context import context
   * @param existingClient existing client data from API
   * @return request map for update
   */
  private Map<String, Object> buildUpdateRequest(
      Client client, ImportContext context, Map<String, Object> existingClient) {
    // Check if client is active
    Object statusObj = existingClient.get("status");
    boolean isActive = false;
    if (statusObj != null) {
      Map<String, Object> status = (Map<String, Object>) statusObj;
      String statusValue = (String) status.get("value");
      isActive = "Active".equalsIgnoreCase(statusValue);
    }

    RequestBuilder builder = RequestBuilder.create();

    if (isActive) {
      // Client is active - only update allowed fields
      log.debug("Client is active - limiting updateable fields");

      if (client.getStaffName() != null) {
        Long staffId = context.resolveEntityId("staff", client.getStaffName());
        if (staffId != null) {
          builder.put("staffId", staffId);
        }
      }

      // Resolve gender, prioritizing label over hardcoded ID
      String genderLabel = client.getGender();
      if (genderLabel != null) {
        builder.put("genderId", FineractEnumMapper.mapGender(genderLabel));
      }

      // Client Type
      String typeLabel = client.getClientType();
      if (typeLabel != null) {
        Long clientTypeId = context.resolveEntityId("codeValue", "ClientType:" + typeLabel);
        if (clientTypeId != null) {
          builder.put("clientTypeId", clientTypeId);
        }
      } else if (client.getClientTypeId() != null) {
        builder.put("clientTypeId", client.getClientTypeId());
      }
    } else {
      // Client not active - can update most fields (excluding immutables)
      builder.put("firstname", client.getFirstName()).put("lastname", client.getLastName());

      builder.putIfNotNull("middlename", client.getMiddleName());

      // Resolve office
      if (client.getOfficeName() != null) {
        Long officeId = context.resolveEntityId("office", client.getOfficeName());
        if (officeId != null) {
          builder.put("officeId", officeId);
        }
      }

      // Resolve staff
      if (client.getStaffName() != null) {
        Long staffId = context.resolveEntityId("staff", client.getStaffName());
        if (staffId != null) {
          builder.put("staffId", staffId);
        }
      }

      // Personal details
      if (client.getDateOfBirth() != null) {
        builder
            .withLocaleDateFormat()
            .put("dateOfBirth", client.getDateOfBirth().format(DATE_FORMATTER));
      }

      // Resolve gender, prioritizing label over hardcoded ID
      String genderLabel = client.getGender();
      if (genderLabel != null) {
        Long genderId = context.resolveEntityId("codeValue", "Gender:" + genderLabel);
        if (genderId != null) {
          builder.put("genderId", genderId);
        }
      }

      builder.putIfNotNull("mobileNo", client.getMobileNo());
      builder.putIfNotNull("emailAddress", client.getEmailAddress());
    }

    return builder.build();
  }

  /**
   * Activates a client.
   *
   * @param clientId client ID
   * @param client client
   * @param context import context
   * @param result import result
   */
  private void activateClient(
      Long clientId, Client client, ImportContext context, ImportResult result) {
    try {
      Map<String, Object> activationRequest =
          RequestBuilder.create()
              .withLocaleDateFormat()
              .put("activationDate", client.getActivationDate().format(DATE_FORMATTER))
              .build();

      apiClient.post(
          "/api/v1/clients/" + clientId + "?command=activate", activationRequest, Map.class);

      log.info("Client activated: {} (ID: {})", client.getExternalId(), clientId);
    } catch (Exception ex) {
      log.warn("Failed to activate client {}: {}", clientId, ex.getMessage());
    }
  }
}
