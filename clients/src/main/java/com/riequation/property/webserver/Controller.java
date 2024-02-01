package com.riequation.property.webserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riequation.property.flows.*;
import com.riequation.property.states.*;
import com.riequation.property.webserver.Dto.AgentStateDTO;
import com.riequation.property.webserver.Dto.OwnerStateDTO;
import com.riequation.property.webserver.Dto.TenantStateDTO;
import net.corda.client.jackson.JacksonSupport;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/")
@CrossOrigin(origins = "*")
public class Controller {

    private static final Logger logger = LoggerFactory.getLogger(RestController.class);
    private final CordaRPCOps proxy;
    private final CordaX500Name me;

    private final RestTemplate restTemplate = new RestTemplate();
    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
        this.me = proxy.nodeInfo().getLegalIdentities().get(0).getName();
    }

    /** Helpers for filtering the network map cache. */
    public String toDisplayString(X500Name name) {
        return BCStyle.INSTANCE.toString(name);
    }

    private boolean isNotary(NodeInfo nodeInfo) {
        return proxy.notaryIdentities()
                .stream()
                .anyMatch(el -> nodeInfo.isLegalIdentity(el));
    }

    private boolean isMe(NodeInfo nodeInfo) {
        return nodeInfo.getLegalIdentities().get(0).getName().equals(me);
    }

    private boolean isNetworkMap(NodeInfo nodeInfo) {
        return nodeInfo.getLegalIdentities().get(0).getName().getOrganisation().equals("Network Map Service");
    }

    @Configuration
    class Plugin {
        @Bean
        public ObjectMapper registerModule() {
            return JacksonSupport.createNonRpcMapper();
        }
    }

//    ----------------------------------- Owner Api -------------------------------

    @PostMapping(value = "/create-owner", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    private ResponseEntity<String> createOwner(@RequestBody HashMap<String, String> ownerDetails) {
        try {
            String name = ownerDetails.get("name");
            String email = ownerDetails.get("email");
            String password = ownerDetails.get("password"); // Ensure password is hashed
            String mobileNumber = ownerDetails.get("mobileNumber");
            String address = ownerDetails.get("address");

            UniqueIdentifier id = proxy.startTrackedFlowDynamic(
                    CreateOwnerFlow.class, name, email, password, mobileNumber, address
            ).getReturnValue().get();

            JSONObject responseJson = new JSONObject();
            responseJson.put("id", id.toString());
            responseJson.put("message", "Owner created successfully");

            return ResponseEntity.status(HttpStatus.CREATED).body(responseJson.toString());
        } catch (Exception e) {
            JSONObject errorJson = new JSONObject();
            errorJson.put("error", "Error creating owner: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorJson.toString());
        }
    }

    @GetMapping(value = "/owners", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<OwnerStateDTO>> getAllOwners() {
        try {
            List<OwnerState> owners = proxy.startTrackedFlowDynamic(GetAllOwnersFlow.class).getReturnValue().get();
            List<OwnerStateDTO> ownerDtos = owners.stream()
                    .map(owner -> new OwnerStateDTO(owner.getName(), owner.getEmail(), owner.getMobileNumber(), owner.getAddress(), owner.getHost(), owner.getLinearId()))
                    .collect(Collectors.toList());
            return new ResponseEntity<>(ownerDtos, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value = "/get-owner/{linearIdStr}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getOwner(@PathVariable String linearIdStr) {
        try {
            UniqueIdentifier linearId = UniqueIdentifier.Companion.fromString(linearIdStr);

            OwnerState ownerState = proxy.startTrackedFlowDynamic(
                    GetOwnerFlow.class, linearId
            ).getReturnValue().get();

            JSONObject responseJson = new JSONObject();
            responseJson.put("name", ownerState.getName());
            responseJson.put("email", ownerState.getEmail());
            responseJson.put("mobileNumber", ownerState.getMobileNumber());
            responseJson.put("address", ownerState.getAddress());
            // Note: Password is intentionally excluded for security reasons

            return ResponseEntity.status(HttpStatus.OK).body(responseJson.toString());
        } catch (Exception e) {
            JSONObject errorJson = new JSONObject();
            errorJson.put("error", "Error retrieving owner: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorJson.toString());
        }
    }



//    ------------------------------- Owner Property Api...........................

    @PostMapping(value = "/add-property", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    private ResponseEntity<String> addProperty(@RequestBody Map<String, Object> propertyDetails) {
        try {
            String details = (String) propertyDetails.get("details");
            String ownerId = (String) propertyDetails.get("ownerId");

            // Parsing the owner's UniqueIdentifier from the provided ownerId string
            UniqueIdentifier ownerUniqueId = UniqueIdentifier.Companion.fromString(ownerId);

            // Parse other fields correctly, handling BigDecimal and List types
            String address = (String) propertyDetails.get("address");
            String pincode = (String) propertyDetails.get("pincode");
            Double price = ((BigDecimal) propertyDetails.get("price")).doubleValue();
            String ownerName = (String) propertyDetails.get("ownerName");
            Double sqrtFeet = ((BigDecimal) propertyDetails.get("sqrtFeet")).doubleValue();

            // Handle amenities as a List
            @SuppressWarnings("unchecked")
            List<String> amenities = (List<String>) propertyDetails.get("amenities");

            String propertyType = (String) propertyDetails.get("propertyType");
            String bhkInfo = (String) propertyDetails.get("bhkInfo");
            String description = (String) propertyDetails.get("description");

            // Start the flow with all parameters
            UniqueIdentifier id = proxy.startTrackedFlowDynamic(
                    AddPropertyFlow.class, details, ownerUniqueId, address, pincode, price,
                    ownerName, sqrtFeet, amenities, propertyType, bhkInfo, description
            ).getReturnValue().get();

            return ResponseEntity.status(HttpStatus.CREATED).body("Property added with ID: " + id.toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error adding property: " + e.getMessage());
        }
    }




    @GetMapping(value = "/properties", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PropertyState>> getAllProperties() {
        try {
            List<PropertyState> properties = proxy.startTrackedFlowDynamic(GetAllPropertiesFlow.class)
                    .getReturnValue()
                    .get()
                    .stream()
                    .map(StateAndRef::getState)
                    .map(TransactionState::getData)
                    .collect(Collectors.toList());
            return new ResponseEntity<>(properties, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving properties", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value = "/properties/owner/{ownerId}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PropertyState>> getPropertiesByOwnerId(@PathVariable String ownerId) {
        try {
            UniqueIdentifier ownerUniqueId = UniqueIdentifier.Companion.fromString(ownerId);
            List<PropertyState> properties = proxy.startTrackedFlowDynamic(GetPropertyByOwnerIdFlow.class, ownerUniqueId)
                    .getReturnValue()
                    .get()
                    .stream()
                    .map(StateAndRef::getState)
                    .map(TransactionState::getData)
                    .collect(Collectors.toList());
            return new ResponseEntity<>(properties, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving properties for owner ID: " + ownerId, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value = "/get-property/{linearIdStr}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<PropertyState> getPropertyById(@PathVariable String linearIdStr) {
        try {
            UniqueIdentifier linearId = UniqueIdentifier.Companion.fromString(linearIdStr);

            PropertyState property = proxy.startTrackedFlowDynamic(
                    GetPropertyFlow.class, linearId
            ).getReturnValue().get();

            return new ResponseEntity<>(property, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving property by ID: " + linearIdStr, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }




//    ------------------------------------ Agent Api -------------------------------------

    @PostMapping(value = "/create-agent", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createAgent(@RequestBody HashMap<String, String> agentDetails) {
        try {
            String name = agentDetails.get("name");
            String email = agentDetails.get("email");
            String password = agentDetails.get("password"); // Ensure password is hashed
            String mobileNumber = agentDetails.get("mobileNumber");
            String address = agentDetails.get("address");

            UniqueIdentifier id = proxy.startTrackedFlowDynamic(
                    CreateAgentFlow.class, name, email, password, mobileNumber, address
            ).getReturnValue().get();

            JSONObject responseJson = new JSONObject();
            responseJson.put("id", id.toString());
            responseJson.put("message", "Agent created successfully");

            return ResponseEntity.status(HttpStatus.CREATED).body(responseJson.toString());
        } catch (Exception e) {
            JSONObject errorJson = new JSONObject();
            errorJson.put("error", "Error creating agent: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorJson.toString());
        }
    }
    @GetMapping(value = "/agents", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AgentStateDTO>> getAllAgents() {
        try {
            List<AgentState> agents = proxy.startTrackedFlowDynamic(GetAllAgentsFlow.class).getReturnValue().get();
            List<AgentStateDTO> agentDtos = agents.stream()
                    .map(agent -> new AgentStateDTO(agent.getName(), agent.getEmail(), agent.getMobileNumber(), agent.getAddress(), agent.getHost(), agent.getLinearId()))
                    .collect(Collectors.toList());
            return new ResponseEntity<>(agentDtos, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping(value = "/create-owner-agent-property-contract", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createOwnerAgentPropertyContract(@RequestBody Map<String, String> contractDetails) {
        try {
            UniqueIdentifier ownerId = UniqueIdentifier.Companion.fromString(contractDetails.get("ownerId"));
            UniqueIdentifier agentId = UniqueIdentifier.Companion.fromString(contractDetails.get("agentId"));
            UniqueIdentifier propertyId = UniqueIdentifier.Companion.fromString(contractDetails.get("propertyId"));
            Date startDate = new SimpleDateFormat("yyyy-MM-dd").parse(contractDetails.get("startDate"));
            Date endDate = new SimpleDateFormat("yyyy-MM-dd").parse(contractDetails.get("endDate"));
            String contractDetailsString = contractDetails.get("contractDetails"); // JSON or specific format
            String status = contractDetails.get("status");

            // Fetch the list of valid agent IDs
            List<UniqueIdentifier> validAgentIds = fetchValidAgentIds();

            UniqueIdentifier id = proxy.startTrackedFlowDynamic(
                    CreateOwnerAgentPropertyContractFlow.class, ownerId, agentId, propertyId,
                    startDate, endDate, contractDetailsString, status, validAgentIds
            ).getReturnValue().get();

            return ResponseEntity.status(HttpStatus.CREATED).body("Owner-Agent-Property Contract created with ID: " + id.toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error creating contract: " + e.getMessage());
        }
    }


    private List<UniqueIdentifier> fetchValidAgentIds() throws Exception {
        String agentNodeUrl = "http://localhost:8090/agents"; // Replace with the actual agent node URL
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                agentNodeUrl, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new Exception("Failed to fetch agent IDs from agent node");
        }

        // Extract the list of agent IDs from the response
        return response.getBody().stream()
                .map(agentInfo -> (Map<String, Object>)agentInfo.get("linearId"))
                .map(linearIdMap -> (String)linearIdMap.get("id"))
                .map(UniqueIdentifier.Companion::fromString)
                .collect(Collectors.toList());
    }

    @GetMapping(value = "/get-contracts-by-owner", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getContractsByOwner(@RequestParam("ownerId") String ownerIdString) {
        try {
            UniqueIdentifier ownerId = UniqueIdentifier.Companion.fromString(ownerIdString);
            List<OwnerAgentPropertyContractState> contracts = proxy.startTrackedFlowDynamic(GetContractsByOwnerFlow.class, ownerId)
                    .getReturnValue()
                    .get();

            // Convert contracts to a suitable response format (if needed)
            // ...

            return ResponseEntity.ok(contracts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error fetching contracts: " + e.getMessage());
        }
    }








//    ----------------------------- Owner Login Api----------------------------------------


    @PostMapping(value = "owner-login", consumes = "application/json", produces = "application/json")
    public ResponseEntity<String> loginUser(@RequestBody Map<String, String> params) {
        try {
            String email = params.get("email");
            String password = params.get("password");

            UniqueIdentifier ownerId = authenticateOwner(email, password);
            if (ownerId != null) {
                String token = JwtUtil.generateToken(email);
                JSONObject responseJson = new JSONObject();
                responseJson.put("token", token);
                responseJson.put("ownerId", ownerId.toString());
                responseJson.put("message", "Login successful");
                return ResponseEntity.ok(responseJson.toString());
            } else {
                JSONObject errorJson = new JSONObject();
                errorJson.put("error", "Invalid credentials");
                errorJson.put("message", "Login failed due to invalid credentials");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorJson.toString());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            JSONObject errorJson = new JSONObject();
            errorJson.put("error", "Authentication failed");
            errorJson.put("message", "Login failed due to an internal error");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorJson.toString());
        }
    }

    private UniqueIdentifier authenticateOwner(String email, String password) {
        try {
            // Query the ledger for an owner with the given email
            List<StateAndRef<OwnerState>> ownerStates = proxy.vaultQuery(OwnerState.class).getStates();
            return ownerStates.stream()
                    .map(StateAndRef::getState)
                    .map(net.corda.core.contracts.TransactionState::getData)
                    .filter(owner -> owner.getEmail().equals(email) && owner.getPassword().equals(password))
                    .findFirst()
                    .map(OwnerState::getLinearId)
                    .orElse(null); // Return null if owner not found or password does not match
        } catch (Exception e) {
            logger.error("Authentication failed: " + e.getMessage(), e);
            return null;
        }
    }

//    ----------------------------------- Agent Login--------------------------------

    @PostMapping(value = "/agent-login", consumes = "application/json", produces = "application/json")
    public ResponseEntity<String> loginAgent(@RequestBody Map<String, String> params) {
        try {
            String email = params.get("email");
            String password = params.get("password");

            UniqueIdentifier agentId = authenticateAgent(email, password);
            if (agentId != null) {
                String token = JwtUtil.generateToken(email);
                JSONObject responseJson = new JSONObject();
                responseJson.put("token", token);
                responseJson.put("agentId", agentId.toString());
                responseJson.put("message", "Login successful");
                return ResponseEntity.ok(responseJson.toString());
            } else {
                JSONObject errorJson = new JSONObject();
                errorJson.put("error", "Invalid credentials");
                errorJson.put("message", "Login failed due to invalid credentials");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorJson.toString());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            JSONObject errorJson = new JSONObject();
            errorJson.put("error", "Authentication failed");
            errorJson.put("message", "Login failed due to an internal error");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorJson.toString());
        }
    }
    private UniqueIdentifier authenticateAgent(String email, String password) {
        try {
            List<StateAndRef<AgentState>> agentStates = proxy.vaultQuery(AgentState.class).getStates();
            Optional<AgentState> agentState = agentStates.stream()
                    .map(StateAndRef::getState)
                    .map(net.corda.core.contracts.TransactionState::getData)
                    .filter(agent -> agent.getEmail().equals(email) && agent.getPassword().equals(password))
                    .findFirst();

            return agentState.map(AgentState::getLinearId).orElse(null);
        } catch (Exception e) {
            logger.error("Agent authentication failed: " + e.getMessage(), e);
            return null;
        }
    }


    @GetMapping(value = "/get-agent/{linearIdStr}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAgent(@PathVariable String linearIdStr) {
        try {
            UniqueIdentifier linearId = UniqueIdentifier.Companion.fromString(linearIdStr);

            AgentState agentState = proxy.startTrackedFlowDynamic(
                    GetAgentFlow.class, linearId
            ).getReturnValue().get();

            JSONObject responseJson = new JSONObject();
            responseJson.put("name", agentState.getName());
            responseJson.put("email", agentState.getEmail());
            // Exclude the password for security reasons
            responseJson.put("mobileNumber", agentState.getMobileNumber());
            responseJson.put("address", agentState.getAddress());
            responseJson.put("linearId", agentState.getLinearId().toString());

            return ResponseEntity.status(HttpStatus.OK).body(responseJson.toString());
        } catch (Exception e) {
            JSONObject errorJson = new JSONObject();
            errorJson.put("error", "Error retrieving agent: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorJson.toString());
        }
    }

//    ---------------------------------------- Tentant Api----------------------------------


    @PostMapping(value = "/create-tenant", produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> createTenant(@RequestBody HashMap<String, String> tenantDetails) {
        try {
            String name = tenantDetails.get("name");
            String email = tenantDetails.get("email");
            String password = tenantDetails.get("password"); // Ensure password is hashed
            String mobileNumber = tenantDetails.get("mobileNumber");
            String address = tenantDetails.get("address");

            UniqueIdentifier id = proxy.startTrackedFlowDynamic(
                    CreateTenantFlow.class, name, email, password, mobileNumber, address
            ).getReturnValue().get();

            JSONObject responseJson = new JSONObject();
            responseJson.put("id", id.toString());
            responseJson.put("message", "Tenant created successfully");

            return ResponseEntity.status(HttpStatus.CREATED).body(responseJson.toString());
        } catch (Exception e) {
            JSONObject errorJson = new JSONObject();
            errorJson.put("error", "Error creating tenant: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorJson.toString());
        }
    }
    @GetMapping(value = "/tenants", produces = "application/json")
    public ResponseEntity<List<TenantStateDTO>> getAllTenants() {
        try {
            List<TenantState> tenants = proxy.startTrackedFlowDynamic(GetAllTenantsFlow.class).getReturnValue().get();
            List<TenantStateDTO> tenantDtos = tenants.stream()
                    .map(tenant -> new TenantStateDTO(tenant.getName(), tenant.getEmail(), tenant.getMobileNumber(), tenant.getAddress(), tenant.getHost(), tenant.getLinearId()))
                    .collect(Collectors.toList());
            return new ResponseEntity<>(tenantDtos, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping(value = "/get-tenant/{linearIdStr}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getTenant(@PathVariable String linearIdStr) {
        try {
            UniqueIdentifier linearId = UniqueIdentifier.Companion.fromString(linearIdStr);

            TenantState tenantState = proxy.startTrackedFlowDynamic(
                    GetTenantFlow.class, linearId
            ).getReturnValue().get();

            JSONObject responseJson = new JSONObject();
            responseJson.put("name", tenantState.getName());
            responseJson.put("email", tenantState.getEmail());
            // Exclude the password for security reasons
            responseJson.put("mobileNumber", tenantState.getMobileNumber());
            responseJson.put("address", tenantState.getAddress());
            responseJson.put("linearId", tenantState.getLinearId().toString());

            return ResponseEntity.status(HttpStatus.OK).body(responseJson.toString());
        } catch (Exception e) {
            JSONObject errorJson = new JSONObject();
            errorJson.put("error", "Error retrieving tenant: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorJson.toString());
        }
    }

    @PostMapping(value = "/login-tentant", consumes = "application/json", produces = "application/json")
    public ResponseEntity<String> loginTentent(@RequestBody Map<String, String> params) {
        try {
            String email = params.get("email");
            String password = params.get("password");

            UniqueIdentifier tenantId = authenticateTenant(email, password);
            if (tenantId != null) {
                String token = JwtUtil.generateToken(email);
                JSONObject responseJson = new JSONObject();
                responseJson.put("token", token);
                responseJson.put("tenantId", tenantId.toString());
                responseJson.put("message", "Login successful");
                return ResponseEntity.ok(responseJson.toString());
            } else {
                JSONObject errorJson = new JSONObject();
                errorJson.put("error", "Invalid credentials");
                errorJson.put("message", "Login failed due to invalid credentials");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorJson.toString());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            JSONObject errorJson = new JSONObject();
            errorJson.put("error", "Authentication failed");
            errorJson.put("message", "Login failed due to an internal error");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorJson.toString());
        }
    }

    private UniqueIdentifier authenticateTenant(String email, String password) {
        try {
            // Query the ledger for a tenant with the given email
            List<StateAndRef<TenantState>> tenantStates = proxy.vaultQuery(TenantState.class).getStates();
            return tenantStates.stream()
                    .map(StateAndRef::getState)
                    .map(net.corda.core.contracts.TransactionState::getData)
                    .filter(tenant -> tenant.getEmail().equals(email) && tenant.getPassword().equals(password))
                    .findFirst()
                    .map(TenantState::getLinearId)
                    .orElse(null); // Return null if tenant not found or password does not match
        } catch (Exception e) {
            logger.error("Authentication failed: " + e.getMessage(), e);
            return null;
        }
    }


//    ------------------------------- Contract details api------------------------


//    @GetMapping("/by-entity")
//    public ResponseEntity<List<OwnerAgentPropertyContractState>> getContractsByEntity(@RequestParam("entityId") String entityIdString, @RequestParam("entityType") String entityType) {
//        try {
//            UniqueIdentifier entityId = UniqueIdentifier.Companion.fromString(entityIdString);
//            List<OwnerAgentPropertyContractState> contracts;
//
//            if ("owner".equalsIgnoreCase(entityType)) {
//                contracts = proxy.startTrackedFlowDynamic(GetContractsByOwnerFlow.class, entityId)
//                        .getReturnValue()
//                        .get();
//            } else if ("agent".equalsIgnoreCase(entityType)) {
//                contracts = proxy.startTrackedFlowDynamic(GetContractsByAgentFlow.class, entityId)
//                        .getReturnValue()
//                        .get();
//            } else {
//                return ResponseEntity.badRequest().body(null); // Invalid entity type
//            }
//
//            return ResponseEntity.ok(contracts);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(null); // Return an appropriate error response
//        }
//    }


    @GetMapping("/contracts/owner")
    public ResponseEntity<List<OwnerAgentPropertyContractState>> getconatractsByOwner(@RequestParam("ownerId") String ownerIdString) {
        try {
            UniqueIdentifier ownerId = UniqueIdentifier.Companion.fromString(ownerIdString);
            List<OwnerAgentPropertyContractState> contracts = proxy.startTrackedFlowDynamic(GetContractsByOwnerFlow.class, ownerId)
                    .getReturnValue()
                    .get();

            return ResponseEntity.ok(contracts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // Return an error response
        }
    }


    @GetMapping("/contracts/agent")
    public ResponseEntity<List<OwnerAgentPropertyContractState>> getContractsByAgent(@RequestParam("agentId") String agentIdString) {
        try {
            UniqueIdentifier agentId = UniqueIdentifier.Companion.fromString(agentIdString);
            List<OwnerAgentPropertyContractState> contracts = proxy.startTrackedFlowDynamic(GetContractsByAgentFlow.class, agentId)
                    .getReturnValue()
                    .get();

            return ResponseEntity.ok(contracts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // Return an error response
        }
    }

}





