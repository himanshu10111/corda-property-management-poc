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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

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
    public ResponseEntity<String> addProperty(@RequestBody HashMap<String, String> propertyDetails) {
        try {
            String address = propertyDetails.get("address");
            String propertyType = propertyDetails.get("propertyType");
            String ownerAccountId = propertyDetails.get("ownerAccountId"); // UUID as String

            UUID ownerAccountUUID = UUID.fromString(ownerAccountId);
            UniqueIdentifier id = proxy.startTrackedFlowDynamic(
                    AddPropertyFlow.class, address, propertyType, ownerAccountUUID
            ).getReturnValue().get();

            return ResponseEntity.status(HttpStatus.CREATED).body("Property added with ID: " + id.toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error adding property: " + e.getMessage());
        }
    }


    @GetMapping(value = "/properties/{ownerAccountId}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPropertyListByOwner(@PathVariable String ownerAccountId) {
        try {
            UUID ownerAccountUUID = UUID.fromString(ownerAccountId);
            List<PropertyState> properties = proxy.startTrackedFlowDynamic(
                    GetPropertiesOfOwnerFlow.class, ownerAccountUUID
            ).getReturnValue().get();

            if (properties.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No properties found for the given owner account ID.");
            }

            return new ResponseEntity<>(properties, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error retrieving properties: " + e.getMessage());
        }
    }


    @GetMapping(value = "/properties", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllProperties() {
        try {
            List<PropertyState> properties = proxy.startTrackedFlowDynamic(GetAllPropertiesFlow.class).getReturnValue().get();

            if (properties.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No properties found.");
            }

            return new ResponseEntity<>(properties, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error retrieving properties: " + e.getMessage());
        }
    }



}


