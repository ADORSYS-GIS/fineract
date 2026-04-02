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

      boolean isEntityClient = client.getLegalFormId() != null && client.getLegalFormId() == 2;
      if (isEntityClient && client.getFullname() != null) {
        log.info("Client created: {} (ID: {})", client.getFullname(), clientId);
      } else {
        log.info(
            "Client created: {} {} {} (ID: {})",
            client.getFirstName(),
            client.getMiddleName() != null ? client.getMiddleName() + " " : "",
            client.getLastName(),
            clientId);
      }
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

    // Name fields depend on legal form: entity clients use fullname, persons use firstname/lastname
    boolean isEntity = client.getLegalFormId() != null && client.getLegalFormId() == 2;
    if (isEntity && client.getFullname() != null) {
      builder.put("fullname", client.getFullname());
    } else {
      builder.put("firstname", client.getFirstName()).put("lastname", client.getLastName());
      builder.putIfNotNull("middlename", client.getMiddleName());
    }

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

    // Resolve gender from code values (Gender code)
    if (client.getGender() != null) {
      Long genderId = resolveGenderId(client.getGender());
      if (genderId != null) {
        builder.put("genderId", genderId);
      } else {
        // Fall back to enum mapper if retrieval fails
        log.warn(
            "Gender code value '{}' not found via API, falling back to hardcoded mapping",
            client.getGender());
        builder.put("genderId", FineractEnumMapper.mapGender(client.getGender()));
      }
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
    if (client.getAddressLine1() != null
        || client.getAddressLine2() != null
        || client.getCity() != null) {
      Map<String, Object> address =
          RequestBuilder.create()
              .putIfNotNull("addressLine1", client.getAddressLine1())
              .putIfNotNull("addressLine2", client.getAddressLine2())
              .putIfNotNull("city", client.getCity())
              .putIfNotNull("stateProvince", client.getStateProvince())
              .putIfNotNull("countryCode", client.getCountryCode())
              .putIfNotNull("postalCode", client.getPostalCode())
              .build();

      builder.put("address", address);
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
      // According to ChangeDetectionService: staffId, savingsProductId, genderId,
      // clientTypeId
      log.debug("Client is active - limiting updateable fields");

      if (client.getStaffName() != null) {
        Long staffId = context.resolveEntityId("staff", client.getStaffName());
        if (staffId != null) {
          builder.put("staffId", staffId);
        }
      }

      if (client.getGender() != null) {
        // Resolve gender from code values cache first
        Long genderId = context.resolveEntityId("codeValue", "Gender:" + client.getGender());
        if (genderId != null) {
          builder.put("genderId", genderId);
        } else {
          log.warn("Gender '{}' not found in cache for active client update", client.getGender());
        }
      }

      // Note: savingsProductId and clientTypeId would go here if supported in Client
      // model
    } else {
      // Client not active - can update most fields (excluding immutables)
      // Immutable: id, externalId, accountNo, activationDate
      boolean isEntity = client.getLegalFormId() != null && client.getLegalFormId() == 2;
      if (isEntity && client.getFullname() != null) {
        builder.put("fullname", client.getFullname());
      } else {
        builder.put("firstname", client.getFirstName()).put("lastname", client.getLastName());
        builder.putIfNotNull("middlename", client.getMiddleName());
      }

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

      if (client.getGender() != null) {
        // Resolve gender from code values cache first
        Long genderId = context.resolveEntityId("codeValue", "Gender:" + client.getGender());
        if (genderId != null) {
          builder.put("genderId", genderId);
        } else {
          log.warn("Gender '{}' not found in cache for client update", client.getGender());
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

  private final java.util.Map<String, Long> genderCache = new java.util.HashMap<>();

  private Long resolveGenderId(String genderName) {
    if (genderName == null) {
      return null;
    }

    if (genderCache.isEmpty()) {
      try {
        // 1. Find Gender Code ID
        Long genderCodeId = 4L; // Default for system defined Gender
        try {
          List<?> codes = apiClient.get("/api/v1/codes", List.class);
          if (codes != null) {
            for (Object obj : codes) {
              Map<String, Object> code = (Map<String, Object>) obj;
              if ("Gender".equalsIgnoreCase((String) code.get("name"))) {
                genderCodeId = ((Number) code.get("id")).longValue();
                break;
              }
            }
          }
        } catch (Exception e) {
          log.warn("Failed to lookup Gender code ID, using default 4: {}", e.getMessage());
        }

        // 2. Fetch Code Values
        List<?> values = apiClient.get("/api/v1/codes/" + genderCodeId + "/codevalues", List.class);
        if (values != null) {
          for (Object obj : values) {
            Map<String, Object> val = (Map<String, Object>) obj;
            String name = (String) val.get("name");
            Long id = ((Number) val.get("id")).longValue();
            genderCache.put(name.toUpperCase(), id);
          }
        }
      } catch (Exception e) {
        log.warn("Failed to populate gender cache: {}", e.getMessage());
      }
    }

    return genderCache.get(genderName.toUpperCase());
  }
}
