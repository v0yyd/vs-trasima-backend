package dhbw.trasima.trasima_aufgabe_10.client;

import dhbw.trasima.trasima_bis_5.IPositionPublisher;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class RestPositionPublisher implements IPositionPublisher, IPublishPosition {

    private final HttpClient httpClient;
    private final String baseUrl;
    private final Set<Integer> createdIds = ConcurrentHashMap.newKeySet();
    private final Map<Integer, LastPos> lastPositions = new ConcurrentHashMap<>();

    public RestPositionPublisher(String baseUrl) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @Override
    public void publishPosition(int id, double x, double y, double speed) {
        double direction = computeDirectionDegrees(id, x, y);
        publishPosition(id, x, y, speed, direction);
    }

    @Override
    public void publishPosition(int id, double lat, double lon, double speed, double direction) {
        String json = toJson(id, lat, lon, speed, direction);

        if (createdIds.add(id)) {
            int status = post(id, json);
            if (status == 201) {
                return;
            }
            if (status != 409) {
                createdIds.remove(id);
                return;
            }
        }

        int status = put(id, json);
        if (status == 404) {
            createdIds.remove(id);
        }
    }

    @Override
    public void deleteVehicle(int id) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/trasima/vehicles/" + id))
                .timeout(Duration.ofSeconds(2))
                .DELETE()
                .build();
        send(request);
        createdIds.remove(id);
        lastPositions.remove(id);
    }

    private int post(int id, String json) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/trasima/vehicles/" + id))
                .timeout(Duration.ofSeconds(2))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        return send(request);
    }

    private int put(int id, String json) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/trasima/vehicles/" + id))
                .timeout(Duration.ofSeconds(2))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        return send(request);
    }

    private int send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode();
        } catch (Exception e) {
            System.out.println("REST Fehler: " + e.getMessage());
            return -1;
        }
    }

    private double computeDirectionDegrees(int id, double x, double y) {
        LastPos previous = lastPositions.put(id, new LastPos(x, y));
        if (previous == null) {
            return 0.0;
        }
        double dx = x - previous.x;
        double dy = y - previous.y;
        if (dx == 0.0 && dy == 0.0) {
            return 0.0;
        }
        double radians = Math.atan2(dy, dx);
        double degrees = Math.toDegrees(radians);
        return degrees < 0.0 ? degrees + 360.0 : degrees;
    }

    private static String toJson(int id, double lat, double lon, double speed, double direction) {
        return "{"
                + "\"id\":" + id + ","
                + "\"lat\":" + lat + ","
                + "\"lon\":" + lon + ","
                + "\"speed\":" + speed + ","
                + "\"direction\":" + direction
                + "}";
    }

    private static String trimTrailingSlash(String s) {
        if (s == null || s.isBlank()) {
            return "http://localhost:8080";
        }
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private record LastPos(double x, double y) {
    }
}

