package com.rnpc.inventory.service;

import com.rnpc.inventory.dto.XenditSessionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class XenditService {

    private final RestClient restClient;

    public XenditService(RestClient.Builder builder,
                          @Value("${xendit.base-url}") String baseUrl,
                          @Value("${xendit.api.key}") String apiKey) {
        // Xendit uses HTTP Basic Auth with the secret key as the username and an empty password.
        String basicAuth = Base64.getEncoder().encodeToString((apiKey + ":").getBytes(StandardCharsets.UTF_8));
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                .build();
    }

    public XenditSessionResponse createPaymentSession(String referenceId, double amount, String successReturnUrl) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reference_id", referenceId);
        body.put("currency", "PHP");
        body.put("country", "PH");
        body.put("amount", amount);
        body.put("session_type", "PAY");
        body.put("mode", "PAYMENT_LINK");
        if (successReturnUrl != null) {
            body.put("success_return_url", successReturnUrl);
        }

        return restClient.post()
                .uri("/sessions")
                .body(body)
                .retrieve()
                .body(XenditSessionResponse.class);
    }

    public String getSessionStatus(String sessionId) {
        XenditSessionResponse response = restClient.get()
                .uri("/sessions/{id}", sessionId)
                .retrieve()
                .body(XenditSessionResponse.class);
        return response != null ? response.getStatus() : null;
    }
}
