package com.logistics.controller.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/public/maps")
public class MapsController {

    @Value("${google.maps.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/autocomplete")
    public ResponseEntity<?> autocomplete(@RequestParam String input) {
        String url = UriComponentsBuilder
                .fromUriString("https://maps.googleapis.com/maps/api/place/autocomplete/json")
                .queryParam("input", input)
                .queryParam("language", "vi")
                .queryParam("region", "vn")
                .queryParam("components", "country:vn")
                .queryParam("types", "geocode")
                .queryParam("key", apiKey)
                .toUriString();

        return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
    }

    @GetMapping("/geocode")
    public ResponseEntity<?> geocode(@RequestParam String address) {
        String url = UriComponentsBuilder
                .fromUriString("https://maps.googleapis.com/maps/api/geocode/json")
                .queryParam("address", address)
                .queryParam("language", "vi")
                .queryParam("region", "vn")
                .queryParam("components", "country:VN")
                .queryParam("key", apiKey)
                .toUriString();

        return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
    }

    @GetMapping("/directions")
    public ResponseEntity<?> directions(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam(defaultValue = "driving") String mode
    ) {
        String url = UriComponentsBuilder
                .fromUriString("https://maps.googleapis.com/maps/api/directions/json")
                .queryParam("origin", origin)
                .queryParam("destination", destination)
                .queryParam("mode", mode)
                .queryParam("language", "vi")
                .queryParam("region", "vn")
                .queryParam("key", apiKey)
                .toUriString();

        return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
    }

    @GetMapping("/distance-matrix")
    public ResponseEntity<?> distanceMatrix(
            @RequestParam String origins,
            @RequestParam String destinations
    ) {
        String url = UriComponentsBuilder
                .fromUriString("https://maps.googleapis.com/maps/api/distancematrix/json")
                .queryParam("origins", origins)
                .queryParam("destinations", destinations)
                .queryParam("mode", "driving")
                .queryParam("language", "vi")
                .queryParam("region", "vn")
                .queryParam("key", apiKey)
                .toUriString();

        return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
    }

    @GetMapping("/place-details")
    public ResponseEntity<?> placeDetails(@RequestParam String placeId) {
        String url = UriComponentsBuilder.fromUriString("https://maps.googleapis.com/maps/api/place/details/json")
                .queryParam("place_id", placeId)
                .queryParam("language", "vi")
                .queryParam("region", "vn")
                .queryParam("fields", "formatted_address,geometry,name,address_components") // ← thêm address_components
                .queryParam("key", apiKey)
                .toUriString();

        return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
    }
}