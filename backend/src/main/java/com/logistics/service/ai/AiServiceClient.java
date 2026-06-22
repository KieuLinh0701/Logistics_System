package com.logistics.service.ai;

import com.logistics.config.properties.AiServiceProperties;
import com.logistics.dto.ai.client.AiRouteOptimizationRequestDto;
import com.logistics.dto.ai.client.AiRouteOptimizationResponseDto;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AiServiceClient {

    private final RestClient restClient;
    private final AiServiceProperties properties;

    public AiServiceClient(AiServiceProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    public AiRouteOptimizationResponseDto optimizeRoutes(AiRouteOptimizationRequestDto request) {
        try {
            AiRouteOptimizationResponseDto response = restClient.post()
                    .uri("/api/v1/optimization/route")
                    .body(request)
                    .retrieve()
                    .body(AiRouteOptimizationResponseDto.class);
            if (response == null) {
                throw new IllegalStateException("AI service returned empty response");
            }
            if (Boolean.FALSE.equals(response.getSuccess())) {
                throw new IllegalStateException(
                        response.getMessage() != null ? response.getMessage() : "AI optimization failed");
            }
            return response;
        } catch (RestClientException ex) {
            throw new IllegalStateException("Cannot reach AI service at " + properties.getBaseUrl() + ": " + ex.getMessage(), ex);
        }
    }

    public boolean isHealthy() {
        try {
            restClient.get().uri("/api/v1/optimization/health").retrieve().toBodilessEntity();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
