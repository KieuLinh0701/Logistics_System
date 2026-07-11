package com.logistics.service.ai.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.config.properties.AiServiceProperties;
import com.logistics.dto.ai.AiRecommendationRequestDto;
import com.logistics.dto.ai.AiRecommendationResponseDto;
import com.logistics.dto.ai.AiRouteOptimizationRequestDto;
import com.logistics.dto.ai.AiRouteOptimizationResponseDto;
import com.logistics.service.ai.AiServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class AiServiceClientImpl implements AiServiceClient, DisposableBean {

    private final RestClient restClient;
    private final RestClient recommendClient;
    private final AiServiceProperties properties;
    private final ObjectMapper objectMapper;
    private final AtomicLong lastRecommendCallDurationMs = new AtomicLong(0);

    public AiServiceClientImpl(AiServiceProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;

        // Jackson message converter dùng chung ObjectMapper của Spring (đã đăng ký @JsonProperty/@JsonAlias).
        MappingJackson2HttpMessageConverter jacksonConverter =
                new MappingJackson2HttpMessageConverter(objectMapper);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(factory)
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(jacksonConverter);
                })
                .build();

        // Client riêng cho recommendation: timeout ngắn, không ảnh hưởng OR-Tools.
        SimpleClientHttpRequestFactory recFactory = new SimpleClientHttpRequestFactory();
        recFactory.setConnectTimeout(properties.getRecommendConnectTimeoutMs());
        recFactory.setReadTimeout(properties.getRecommendReadTimeoutMs());
        this.recommendClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(recFactory)
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(jacksonConverter);
                })
                .build();
    }

    public long getLastRecommendCallDurationMs() {
        return lastRecommendCallDurationMs.get();
    }

    @Override
    public AiRouteOptimizationResponseDto optimizeRoutes(AiRouteOptimizationRequestDto request) {
        try {
            log.debug("Requesting route optimization from {}: {}", properties.getBaseUrl(), objectMapper.writeValueAsString(request));
        } catch (Exception e) {
            log.warn("Failed to serialize AI request for logging", e);
        }
        try {
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

    @Override
    public AiRecommendationResponseDto recommendUnassignedOrders(AiRecommendationRequestDto request) {
        if (request == null) {
            return AiRecommendationResponseDto.builder()
                    .success(false)
                    .message("Empty request")
                    .recommendations(Collections.emptyList())
                    .build();
        }
        long start = System.currentTimeMillis();
        String recommendationUrl = "/api/v1/recommendations/orders";
        try {
            log.info("[BE][AiServiceClient] POST {} candidateCount={}",
                    recommendationUrl,
                    request.getCandidateOrders() != null ? request.getCandidateOrders().size() : 0);
            try {
                log.info("[BE][AiServiceClient] Request body={}", objectMapper.writeValueAsString(request));
            } catch (Exception ignored) {}

            String rawResponse = recommendClient.post()
                    .uri(recommendationUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        String body4xx = res.getBody() != null ? new String(res.getBody().readAllBytes()) : "empty";
                        log.error("AI recommend 4xx: status={} body={}", res.getStatusCode(), body4xx);
                    })
                    .body(String.class);

            long elapsed = System.currentTimeMillis() - start;
            log.info("[BE][AiServiceClient] AI response elapsedMs={}, rawBodyLength={}",
                    elapsed, rawResponse != null ? rawResponse.length() : 0);

            AiRecommendationResponseDto response;
            try {
                response = objectMapper.readValue(rawResponse, AiRecommendationResponseDto.class);
            } catch (Exception parseEx) {
                log.error("[BE][AiServiceClient] AI response parse error rawBody={} error={}",
                        rawResponse, parseEx.getMessage());
                throw new IllegalStateException("Cannot parse AI response: " + parseEx.getMessage(), parseEx);
            }

            log.info("[BE][AiServiceClient] AI response success={}, recommendationCount={}, elapsedMs={}",
                    Boolean.TRUE.equals(response != null ? response.getSuccess() : null),
                    response != null && response.getRecommendations() != null
                            ? response.getRecommendations().size()
                            : 0,
                    elapsed);

            return response;
        } catch (ResourceAccessException ex) {
            // Timeout / connection refused
            long elapsed = System.currentTimeMillis() - start;
            log.warn("[BE][AiServiceClient] Recommendation timeout connectTimeoutMs={}, readTimeoutMs={}, elapsedMs={}, error={}",
                    properties.getRecommendConnectTimeoutMs(),
                    properties.getRecommendReadTimeoutMs(),
                    elapsed,
                    ex.getMessage());
            return AiRecommendationResponseDto.builder()
                    .success(false)
                    .message("AI service timeout/connection issue: " + ex.getMessage())
                    .recommendations(Collections.emptyList())
                    .build();
        } catch (HttpClientErrorException ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[BE][AiServiceClient] Recommendation call failed: type={}, status={}, message={}, elapsedMs={}",
                    ex.getClass().getSimpleName(),
                    ex.getStatusCode(),
                    ex.getMessage(),
                    elapsed,
                    ex);
            return AiRecommendationResponseDto.builder()
                    .success(false)
                    .message("AI service rejected request: " + ex.getStatusCode())
                    .recommendations(Collections.emptyList())
                    .build();
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[BE][AiServiceClient] Recommendation call failed: type={}, message={}, elapsedMs={}",
                    ex.getClass().getSimpleName(),
                    ex.getMessage(),
                    elapsed,
                    ex);
            return AiRecommendationResponseDto.builder()
                    .success(false)
                    .message("AI service error: " + ex.getMessage())
                    .recommendations(Collections.emptyList())
                    .build();
        } finally {
            lastRecommendCallDurationMs.set(System.currentTimeMillis() - start);
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            restClient.get().uri("/api/v1/optimization/health").retrieve().toBodilessEntity();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public void destroy() {
        // SimpleClientHttpRequestFactory không giữ connection pool nên không cần đóng.
    }
}
