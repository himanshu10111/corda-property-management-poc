package com.riequation.property.webserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riequation.property.flows.*;
import com.riequation.property.states.OwnerState;
import com.riequation.property.states.PropertyState;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;

import java.math.BigDecimal;
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







}



