package dhbw.trasima.trasima_aufgabe_10.client;

/**
 * Small geo helper methods (WGS84-ish) for short-distance simulations.
 *
 * <p>This is intentionally lightweight and good enough for a city-scale exercise (Mannheim etc.). It uses
 * spherical formulas (no ellipsoid corrections).</p>
 *
 * <p>All angles are in degrees; distances in meters.</p>
 */
final class GeoUtil {

    // Mean earth radius. Good enough for short-distance movement.
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private GeoUtil() {
    }

    static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        // Haversine distance on a sphere.
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dPhi = Math.toRadians(lat2 - lat1);
        double dLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dPhi / 2.0) * Math.sin(dPhi / 2.0)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLambda / 2.0) * Math.sin(dLambda / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_METERS * c;
    }

    static double bearingDegrees(double fromLat, double fromLon, double toLat, double toLon) {
        // Initial bearing (forward azimuth) from start -> target.
        double phi1 = Math.toRadians(fromLat);
        double phi2 = Math.toRadians(toLat);
        double lambda1 = Math.toRadians(fromLon);
        double lambda2 = Math.toRadians(toLon);
        double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
        double theta = Math.atan2(y, x);
        double degrees = Math.toDegrees(theta);
        return degrees < 0.0 ? degrees + 360.0 : degrees;
    }

    static LatLon destinationPoint(double fromLat, double fromLon, double bearingDegrees, double distanceMeters) {
        // Move along a great-circle for the given distance and bearing.
        double delta = distanceMeters / EARTH_RADIUS_METERS;
        double theta = Math.toRadians(bearingDegrees);
        double phi1 = Math.toRadians(fromLat);
        double lambda1 = Math.toRadians(fromLon);

        double sinPhi2 = Math.sin(phi1) * Math.cos(delta) + Math.cos(phi1) * Math.sin(delta) * Math.cos(theta);
        double phi2 = Math.asin(sinPhi2);

        double y = Math.sin(theta) * Math.sin(delta) * Math.cos(phi1);
        double x = Math.cos(delta) - Math.sin(phi1) * Math.sin(phi2);
        double lambda2 = lambda1 + Math.atan2(y, x);

        double lat = Math.toDegrees(phi2);
        double lon = Math.toDegrees(lambda2);
        lon = normalizeLonDegrees(lon);
        return new LatLon(lat, lon);
    }

    static double normalizeLonDegrees(double lon) {
        // Keep longitude in [-180, 180] so Leaflet/OSM doesn't get confused.
        double result = lon;
        while (result > 180.0) {
            result -= 360.0;
        }
        while (result < -180.0) {
            result += 360.0;
        }
        return result;
    }

    record LatLon(double lat, double lon) {
    }
}
