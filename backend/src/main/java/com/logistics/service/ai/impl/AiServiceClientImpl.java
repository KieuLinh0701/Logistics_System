package com.logistics.service.ai.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.config.properties.AiServiceProperties;
import com.logistics.dto.ai.AiRecommendationRequestDto;
import com.logistics.dto.ai.AiRecommendationResponseDto;
import com.logistics.dto.ai.AiRouteOptimizationRequestDto;
import com.logistics.dto.ai.AiRouteOptimizationResponseDto;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.AiErrorCode;
import com.logistics.service.ai.AiServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.ParameterizedTypeReference;
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
            var rawEntity = restClient.post()
                    .uri("/api/v1/optimization/route")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<String>() {});

            String rawResponse = (rawEntity != null && rawEntity.getBody() != null)
                    ? rawEntity.getBody() : "null";

            AiRouteOptimizationResponseDto response;
            try {
                response = objectMapper.readValue(rawResponse, AiRouteOptimizationResponseDto.class);
            } catch (Exception parseEx) {
                log.error("[AI_PARSE_ERROR] {}", parseEx.getMessage(), parseEx);
                throw new AppException(AiErrorCode.AI_RESPONSE_INVALID);
            }

            if (response == null) {
                log.error("[AI_EMPTY_RESPONSE] AI service returned empty response");
                throw new AppException(AiErrorCode.AI_RESPONSE_INVALID);
            }
            if (Boolean.FALSE.equals(response.getSuccess())) {
                log.error("[AI_OPTIMIZATION_FAILED] message={}", response.getMessage());
                throw new AppException(AiErrorCode.AI_OPTIMIZATION_FAILED);
            }
            return response;
        } catch (AppException ex) {
            throw ex;
        } catch (HttpClientErrorException ex) {
            throw new AppException(AiErrorCode.AI_REQUEST_REJECTED);
        } catch (ResourceAccessException ex) {
            throw new AppException(AiErrorCode.AI_SERVICE_TIMEOUT);
        } catch (RestClientException ex) {
            throw new AppException(AiErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public AiRecommendationResponseDto recommendUnassignedOrders(AiRecommendationRequestDto request) {
        if (request == null) {
            return AiRecommendationResponseDto.builder()
                    .success(false)
                    .message(AiErrorCode.AI_REQUEST_REJECTED.getMessage())
                    .recommendations(Collections.emptyList())
                    .build();
        }
        long start = System.currentTimeMillis();
        String recommendationUrl = "/api/v1/recommendations/orders";
        try {
            String rawResponse = recommendClient.post()
                    .uri(recommendationUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);

            AiRecommendationResponseDto response;
            try {
                response = objectMapper.readValue(rawResponse, AiRecommendationResponseDto.class);
            } catch (Exception parseEx) {
                log.error("[BE][AiServiceClient] AI response parse error rawBody={} error={}",
                        rawResponse, parseEx.getMessage(), parseEx);
                return AiRecommendationResponseDto.builder()
                        .success(false)
                        .message(AiErrorCode.AI_RESPONSE_INVALID.getMessage())
                        .recommendations(Collections.emptyList())
                        .build();
            }

            return response;
        } catch (ResourceAccessException ex) {
            log.warn("[BE][AiServiceClient] Recommendation timeout connectTimeoutMs={}, readTimeoutMs={}, elapsedMs={}, error={}",
                    properties.getRecommendConnectTimeoutMs(),
                    properties.getRecommendReadTimeoutMs(),
                    System.currentTimeMillis() - start,
                    ex.getMessage(), ex);
            return AiRecommendationResponseDto.builder()
                    .success(false)
                    .message(AiErrorCode.AI_SERVICE_TIMEOUT.getMessage())
                    .recommendations(Collections.emptyList())
                    .build();
        } catch (HttpClientErrorException ex) {
            return AiRecommendationResponseDto.builder()
                    .success(false)
                    .message(AiErrorCode.AI_REQUEST_REJECTED.getMessage())
                    .recommendations(Collections.emptyList())
                    .build();
        } catch (RestClientException ex) {
            return AiRecommendationResponseDto.builder()
                    .success(false)
                    .message(AiErrorCode.AI_SERVICE_UNAVAILABLE.getMessage())
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
