package org.apache.fineract.config.service.loader;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.PlannedChange;
import org.apache.fineract.config.model.client.Client;
import org.apache.fineract.config.provider.FineractApiClient;
import org.apache.fineract.config.service.ChangeDetectionService;
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
        existingClient =
            apiClient.get("/api/v1/clients?externalId=" + client.getExternalId(), Map.class);

        if (existingClient != null && existingClient.containsKey("id")) {
          clientId = ((Number) existingClient.get("id")).longValue();
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
    Map<String, Object> request = new HashMap<>();

    // Basic information
    request.put("firstname", client.getFirstName());
    request.put("lastname", client.getLastName());
    request.put("locale", LOCALE);
    request.put("dateFormat", DATE_FORMAT);

    if (client.getMiddleName() != null) {
      request.put("middlename", client.getMiddleName());
    }

    if (client.getExternalId() != null) {
      request.put("externalId", client.getExternalId());
    }

    // Resolve office
    if (client.getOfficeName() != null) {
      Long officeId = context.resolveEntityId("office", client.getOfficeName());
      if (officeId != null) {
        request.put("officeId", officeId);
      } else {
        throw new IllegalStateException(
            "Office '" + client.getOfficeName() + "' not found for client");
      }
    }

    // Resolve staff
    if (client.getStaffName() != null) {
      Long staffId = context.resolveEntityId("staff", client.getStaffName());
      if (staffId != null) {
        request.put("staffId", staffId);
      }
    }

    // Personal details
    if (client.getDateOfBirth() != null) {
      request.put("dateOfBirth", client.getDateOfBirth().format(DATE_FORMATTER));
    }

    if (client.getGender() != null) {
      request.put("genderId", mapGender(client.getGender()));
    }

    if (client.getMobileNo() != null) {
      request.put("mobileNo", client.getMobileNo());
    }

    if (client.getEmailAddress() != null) {
      request.put("emailAddress", client.getEmailAddress());
    }

    // Active flag determines if we activate immediately or just submit
    request.put("active", Boolean.TRUE.equals(client.getActive()));

    if (Boolean.TRUE.equals(client.getActive()) && client.getActivationDate() != null) {
      request.put("activationDate", client.getActivationDate().format(DATE_FORMATTER));
    }

    // Address information
    if (client.getAddressLine1() != null
        || client.getAddressLine2() != null
        || client.getCity() != null) {
      Map<String, Object> address = new HashMap<>();

      if (client.getAddressLine1() != null) {
        address.put("addressLine1", client.getAddressLine1());
      }
      if (client.getAddressLine2() != null) {
        address.put("addressLine2", client.getAddressLine2());
      }
      if (client.getCity() != null) {
        address.put("city", client.getCity());
      }
      if (client.getStateProvince() != null) {
        address.put("stateProvince", client.getStateProvince());
      }
      if (client.getCountryCode() != null) {
        address.put("countryCode", client.getCountryCode());
      }
      if (client.getPostalCode() != null) {
        address.put("postalCode", client.getPostalCode());
      }

      request.put("address", address);
    }

    return request;
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
    Map<String, Object> request = new HashMap<>();

    // Check if client is active
    Object statusObj = existingClient.get("status");
    boolean isActive = false;
    if (statusObj != null) {
      Map<String, Object> status = (Map<String, Object>) statusObj;
      String statusValue = (String) status.get("value");
      isActive = "Active".equalsIgnoreCase(statusValue);
    }

    if (isActive) {
      // Client is active - only update allowed fields
      // According to ChangeDetectionService: staffId, savingsProductId, genderId, clientTypeId
      log.debug("Client is active - limiting updateable fields");

      if (client.getStaffName() != null) {
        Long staffId = context.resolveEntityId("staff", client.getStaffName());
        if (staffId != null) {
          request.put("staffId", staffId);
        }
      }

      if (client.getGender() != null) {
        request.put("genderId", mapGender(client.getGender()));
      }

      // Note: savingsProductId and clientTypeId would go here if supported in Client model
    } else {
      // Client not active - can update most fields (excluding immutables)
      // Immutable: id, externalId, accountNo, activationDate
      request.put("firstname", client.getFirstName());
      request.put("lastname", client.getLastName());

      if (client.getMiddleName() != null) {
        request.put("middlename", client.getMiddleName());
      }

      // Resolve office
      if (client.getOfficeName() != null) {
        Long officeId = context.resolveEntityId("office", client.getOfficeName());
        if (officeId != null) {
          request.put("officeId", officeId);
        }
      }

      // Resolve staff
      if (client.getStaffName() != null) {
        Long staffId = context.resolveEntityId("staff", client.getStaffName());
        if (staffId != null) {
          request.put("staffId", staffId);
        }
      }

      // Personal details
      if (client.getDateOfBirth() != null) {
        request.put("dateOfBirth", client.getDateOfBirth().format(DATE_FORMATTER));
        request.put("locale", LOCALE);
        request.put("dateFormat", DATE_FORMAT);
      }

      if (client.getGender() != null) {
        request.put("genderId", mapGender(client.getGender()));
      }

      if (client.getMobileNo() != null) {
        request.put("mobileNo", client.getMobileNo());
      }

      if (client.getEmailAddress() != null) {
        request.put("emailAddress", client.getEmailAddress());
      }
    }

    return request;
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
      Map<String, Object> activationRequest = new HashMap<>();
      activationRequest.put("locale", LOCALE);
      activationRequest.put("dateFormat", DATE_FORMAT);
      activationRequest.put("activationDate", client.getActivationDate().format(DATE_FORMATTER));

      apiClient.post(
          "/api/v1/clients/" + clientId + "?command=activate", activationRequest, Map.class);

      log.info("Client activated: {} (ID: {})", client.getExternalId(), clientId);
    } catch (Exception ex) {
      log.warn("Failed to activate client {}: {}", clientId, ex.getMessage());
    }
  }

  private Integer mapGender(String gender) {
    return switch (gender.toUpperCase()) {
      case "MALE" -> 22;
      case "FEMALE" -> 23;
      case "OTHER" -> 24;
      default -> throw new IllegalArgumentException("Invalid gender: " + gender);
    };
  }
}
