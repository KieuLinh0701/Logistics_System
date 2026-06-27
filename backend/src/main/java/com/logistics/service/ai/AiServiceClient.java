package com.logistics.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.config.properties.AiServiceProperties;
import com.logistics.dto.ai.AiRouteOptimizationRequestDto;
import com.logistics.dto.ai.AiRouteOptimizationResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
public class AiServiceClient {

    private final RestClient restClient;
    private final AiServiceProperties properties;
    private final ObjectMapper objectMapper;

    public AiServiceClient(AiServiceProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
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
            log.debug("Requesting route optimization from {}: {}", properties.getBaseUrl(), objectMapper.writeValueAsString(request));
        } catch (Exception e) {
            log.warn("Failed to serialize AI request for logging", e);
        }
        try {
            // DEBUG: lấy raw JSON response trước
            var rawEntity = restClient.post()
                    .uri("/api/v1/optimization/route")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        String body4xx = res.getBody() != null ? new String(res.getBody().readAllBytes()) : "empty";
                        log.error("AI service 4xx: status={} body={}", res.getStatusCode(), body4xx);
                    })
                    .toEntity(new ParameterizedTypeReference<String>() {});

            String rawResponse = (rawEntity != null && rawEntity.getBody() != null)
                    ? rawEntity.getBody() : "null";

            AiRouteOptimizationResponseDto response;
            try {
                response = objectMapper.readValue(rawResponse, AiRouteOptimizationResponseDto.class);
            } catch (Exception parseEx) {
                log.error("[AI_PARSE_ERROR] {}", parseEx.getMessage());
                throw new IllegalStateException("Cannot parse AI response: " + parseEx.getMessage(), parseEx);
            }

            if (response.getRoutes() != null) {
                for (int ri = 0; ri < response.getRoutes().size(); ri++) {
                    var route = response.getRoutes().get(ri);
                    if (route.getStops() != null) {
                        for (int si = 0; si < route.getStops().size(); si++) {
                            var stop = route.getStops().get(si);
                            log.info(
                                "[AI_PARSED_STOP] routeIdx={} stopIdx={} orderId={} tracking={} stopType={}",
                                ri, si, stop.getOrderId(), stop.getTrackingNumber(), stop.getStopType()
                            );
                        }
                    }
                    var retStop = route.getReturnToOfficeStop();
                    if (retStop != null) {
                        log.info(
                            "[AI_PARSED_STOP_RETURN] orderId={} tracking={} stopType={}",
                            retStop.getOrderId(), retStop.getTrackingNumber(), retStop.getStopType()
                        );
                    }
                }
            }

            if (response == null) {
                throw new IllegalStateException("AI service returned empty response");
            }
            if (Boolean.FALSE.equals(response.getSuccess())) {
                throw new IllegalStateException(
                        response.getMessage() != null ? response.getMessage() : "AI optimization failed");
            }
            log.info("AI returned {} optimized routes", response.getRoutes() != null ? response.getRoutes().size() : 0);
            return response;
        } catch (HttpClientErrorException ex) {
            log.error("AI service client error {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new IllegalStateException("AI service rejected request (" + ex.getStatusCode() + "): " + ex.getResponseBodyAsString(), ex);
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
