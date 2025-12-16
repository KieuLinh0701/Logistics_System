package com.logistics.utils;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class LocationUtils {

    private static final RestTemplate restTemplate = new RestTemplate();
    private static final String BASE_URL = "https://provinces.open-api.vn/api/v2";

    public static String getCityNameByCode(int cityCode) {
        List<Map<String, Object>> cities = restTemplate.getForObject(BASE_URL + "/p/", List.class);
        if (cities != null) {
            return cities.stream()
                    .filter(c -> (int) c.get("code") == cityCode)
                    .map(c -> (String) c.get("name"))
                    .findFirst()
                    .orElse("");
        }
        return "";
    }

    public static String getWardNameByCode(int cityCode, int wardCode) {
        Map<String, Object> city = restTemplate.getForObject(BASE_URL + "/p/{code}?depth=2", Map.class, cityCode);
        if (city != null && city.containsKey("wards")) {
            List<Map<String, Object>> wards = (List<Map<String, Object>>) city.get("wards");
            return wards.stream()
                    .filter(w -> (int) w.get("code") == wardCode)
                    .map(w -> (String) w.get("name"))
                    .findFirst()
                    .orElse("");
        }
        return "";
    }
}