package com.riequation.property.webserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riequation.property.flows.*;
import com.riequation.property.states.AgentState;
import com.riequation.property.states.OwnerState;
import com.riequation.property.states.PropertyState;
import com.riequation.property.states.TenantState;
import net.corda.client.jackson.JacksonSupport;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
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
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
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

            return ResponseEntity.status(HttpStatus.CREATED).body("Owner created with ID: " + id.toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error creating owner: " + e.getMessage());
        }
    }

    @GetMapping(value = "/owners", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<OwnerState>> getAllOwners() {
        try {
            List<OwnerState> owners = proxy.startTrackedFlowDynamic(GetAllOwnersFlow.class).getReturnValue().get();
            return new ResponseEntity<>(owners, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
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

            // Cast to BigDecimal and then convert to Double
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

            return ResponseEntity.status(HttpStatus.CREATED).body("Agent created with ID: " + id.toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error creating agent: " + e.getMessage());
        }
    }

    @GetMapping(value = "/agents", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AgentState>> getAllAgents() {
        try {
            List<AgentState> agents = proxy.startTrackedFlowDynamic(GetAllAgentsFlow.class).getReturnValue().get();
            return new ResponseEntity<>(agents, HttpStatus.OK);
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



//    ----------------------------- Owner Login Api----------------------------------------


    @PostMapping(value = "/login", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> params) {
        try {
            String email = params.get("email");
            String password = params.get("password");

            UniqueIdentifier ownerId = authenticateOwner(email, password);
            if (ownerId != null) {
                String token = JwtUtil.generateToken(email);
                Map<String, String> response = new HashMap<>();
                response.put("token", token);
                response.put("ownerId", ownerId.toString()); // Include the owner's linear ID in the response
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed");
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

    @PostMapping(value = "/agent/login", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> loginAgent(@RequestBody Map<String, String> params) {
        try {
            String email = params.get("email");
            String password = params.get("password");

            UniqueIdentifier agentId = authenticateAgent(email, password);
            if (agentId != null) {
                String token = JwtUtil.generateToken(email);
                Map<String, String> response = new HashMap<>();
                response.put("token", token);
                response.put("agentId", agentId.toString());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed");
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

            return ResponseEntity.status(HttpStatus.CREATED).body("Tenant created with ID: " + id.toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error creating tenant: " + e.getMessage());
        }
    }

    @GetMapping(value = "/tenants", produces = "application/json")
    public ResponseEntity<List<TenantState>> getAllTenants() {
        try {
            List<TenantState> tenants = proxy.startTrackedFlowDynamic(GetAllTenantsFlow.class).getReturnValue().get();
            return new ResponseEntity<>(tenants, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/login/tentant", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> loginTentent (@RequestBody Map<String, String> params) {
        try {
            String email = params.get("email");
            String password = params.get("password");

            UniqueIdentifier tenantId = authenticateTenant(email, password);
            if (tenantId != null) {
                String token = JwtUtil.generateToken(email);
                Map<String, String> response = new HashMap<>();
                response.put("token", token);
                response.put("tenantId", tenantId.toString()); // Include the tenant's linear ID in the response
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed");
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

}





