package com.riequation.property.webserver.Dto;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import net.corda.core.contracts.UniqueIdentifier;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PropertyIdFetcher {

    private final RestTemplate restTemplate;

    public PropertyIdFetcher(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<UniqueIdentifier> fetchPropertyIds() throws Exception {
        String propertiesUrl = "http://localhost:8080/properties";
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                propertiesUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody().stream()
                    .map(propertyMap -> (Map<String, Object>) propertyMap.get("linearId"))
                    .map(linearIdMap -> (String) linearIdMap.get("id"))
                    .map(UniqueIdentifier.Companion::fromString)
                    .collect(Collectors.toList());
        } else {
            throw new Exception("Failed to fetch property IDs. Response: " + response.getStatusCode());
        }
    }
}
