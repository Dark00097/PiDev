package com.nexora.bank.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeoLocationService {
    private static final String GEO_ENDPOINT_IPAPI = "https://ipapi.co/json/";
    private static final String GEO_ENDPOINT_IPWHO = "https://ipwho.is/";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(4);
    private static final Pattern STRING_FIELD = Pattern.compile("\"%s\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern NUMBER_FIELD = Pattern.compile("\"%s\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");

    private final HttpClient httpClient;
    private volatile LocationSnapshot cache;
    private volatile Instant cacheExpiresAt;

    public GeoLocationService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
    }

    public LocationSnapshot resolveCurrentLocation() {
        Instant now = Instant.now();
        if (cache != null && cacheExpiresAt != null && now.isBefore(cacheExpiresAt)) {
            return cache;
        }

        LocationSnapshot snapshot = tryEndpoint(GEO_ENDPOINT_IPAPI, "country_name", "region", "latitude", "longitude", "ip");
        if (!isUsable(snapshot)) {
            snapshot = tryEndpoint(GEO_ENDPOINT_IPWHO, "country", "region", "latitude", "longitude", "ip");
        }

        if (!isUsable(snapshot)) {
            snapshot = unknown();
        }

        cache = snapshot;
        cacheExpiresAt = now.plusSeconds(600);
        return snapshot;
    }

    private LocationSnapshot tryEndpoint(
        String endpoint,
        String countryField,
        String regionField,
        String latField,
        String lngField,
        String ipField
    ) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", "NEXORA-Bank/1.0")
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return unknown();
            }
            return parseLocation(response.body(), countryField, regionField, latField, lngField, ipField);
        } catch (Exception ex) {
            return unknown();
        }
    }

    private LocationSnapshot parseLocation(
        String payload,
        String countryField,
        String regionField,
        String latField,
        String lngField,
        String ipField
    ) {
        if (payload == null || payload.isBlank()) {
            return unknown();
        }

        String city = extractString(payload, "city");
        String region = extractString(payload, regionField);
        String country = extractString(payload, countryField);
        String ip = extractString(payload, ipField);
        Double lat = extractDouble(payload, latField);
        Double lng = extractDouble(payload, lngField);

        String display = buildDisplay(city, region, country);
        return new LocationSnapshot(display, lat, lng, ip);
    }

    private boolean isUsable(LocationSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        if (snapshot.latitude() != null && snapshot.longitude() != null) {
            return true;
        }
        return snapshot.displayName() != null && !"Unknown location".equalsIgnoreCase(snapshot.displayName().trim());
    }

    private String buildDisplay(String city, String region, String country) {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, city);
        appendPart(sb, region);
        appendPart(sb, country);
        return sb.length() == 0 ? "Unknown location" : sb.toString();
    }

    private void appendPart(StringBuilder sb, String value) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(cleaned);
    }

    private String extractString(String payload, String field) {
        Matcher matcher = Pattern.compile(String.format(Locale.ROOT, STRING_FIELD.pattern(), Pattern.quote(field))).matcher(payload);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private Double extractDouble(String payload, String field) {
        Matcher matcher = Pattern.compile(String.format(Locale.ROOT, NUMBER_FIELD.pattern(), Pattern.quote(field))).matcher(payload);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Double.parseDouble(matcher.group(1));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static LocationSnapshot unknown() {
        return new LocationSnapshot("Unknown location", null, null, "");
    }

    public record LocationSnapshot(String displayName, Double latitude, Double longitude, String publicIp) {
    }
}
