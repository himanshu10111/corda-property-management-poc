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
public class ContractIdFetcher {

    private final RestTemplate restTemplate;

    public ContractIdFetcher(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<UniqueIdentifier> fetchContractIds() throws Exception {
        String contractNodeUrl = "http://localhost:8080/agent-owner-contracts";
        ResponseEntity<List<Map<String, String>>> response = restTemplate.exchange(
                contractNodeUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, String>>>() {}
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody().stream()
                    .map(contract -> contract.get("linearId"))
                    .map(UniqueIdentifier.Companion::fromString)
                    .collect(Collectors.toList());
        } else {
            throw new Exception("Failed to fetch contract IDs. Response: " + response.getStatusCode());
        }
    }
}
