package dhbw.trasima.trasima_aufgabe_10.client;

import java.util.SplittableRandom;

/**
 * One simulated vehicle that moves from a random spawn position towards a fixed destination.
 *
 * <p>Movement model: every tick, move {@code speedMps * dtSeconds} meters along the bearing to the destination.</p>
 */
final class VehicleSimulator {

    private final int id;
    private final SimulationConfig config;
    private final IPublishPosition publisher;
    private final SplittableRandom rng;

    private double lat;
    private double lon;
    private double speedMps;

    VehicleSimulator(int id, SimulationConfig config, IPublishPosition publisher, long seed) {
        this.id = id;
        this.config = config;
        this.publisher = publisher;
        this.rng = new SplittableRandom(seed);
        // Initialize with a fresh spawn position and speed.
        respawn();
    }

    int id() {
        return id;
    }

    void publishInitial() {
        // Initial publish so the marker exists immediately before the first tick.
        double direction = GeoUtil.bearingDegrees(lat, lon, config.destination.lat, config.destination.lon);
        publisher.publishPosition(id, lat, lon, speedMps, direction);
    }

    void tick(double dtSeconds) {
        double destLat = config.destination.lat;
        double destLon = config.destination.lon;

        // Stop/respawn if we are close enough to the destination.
        double distanceToDest = GeoUtil.distanceMeters(lat, lon, destLat, destLon);
        if (distanceToDest <= config.arrivalRadiusMeters) {
            handleArrival();
            return;
        }

        // Heading to the destination (0..360 degrees).
        double direction = GeoUtil.bearingDegrees(lat, lon, destLat, destLon);

        // Move in meters per tick; do not overshoot the destination.
        double stepMeters = speedMps * dtSeconds;
        double moveMeters = Math.min(stepMeters, distanceToDest);

        // Convert "move X meters in direction Y" into a new lat/lon.
        GeoUtil.LatLon next = GeoUtil.destinationPoint(lat, lon, direction, moveMeters);
        lat = next.lat();
        lon = next.lon();

        publisher.publishPosition(id, lat, lon, speedMps, direction);
    }

    private void handleArrival() {
        String mode = config.onArrival == null ? "respawn" : config.onArrival.trim().toLowerCase();
        switch (mode) {
            case "stop" -> {
                // Publish a final update at the destination, then do nothing on subsequent ticks.
                lat = config.destination.lat;
                lon = config.destination.lon;
                publisher.publishPosition(id, lat, lon, 0.0, 0.0);
                speedMps = 0.0;
            }
            case "respawn" -> {
                // Pick a new spawn position and speed, then start a new trip to the same destination.
                respawn();
                publishInitial();
            }
            default -> {
                respawn();
                publishInitial();
            }
        }
    }

    private void respawn() {
        SimulationConfig.Spawn spawn = config.spawn == null ? new SimulationConfig.Spawn() : config.spawn;
        SimulationConfig.Speed speedCfg = config.speed == null ? new SimulationConfig.Speed() : config.speed;

        // Uniformly sample a point within a circle by using sqrt(random) for the radius.
        double bearing = rng.nextDouble(0.0, 360.0);
        double radius = Math.sqrt(rng.nextDouble()) * Math.max(0.0, spawn.radiusMeters);
        GeoUtil.LatLon start = GeoUtil.destinationPoint(spawn.centerLat, spawn.centerLon, bearing, radius);
        lat = start.lat();
        lon = start.lon();

        // Pick a random speed (m/s) within [min, max].
        double min = speedCfg.minMps;
        double max = speedCfg.maxMps;
        if (!(max >= min)) {
            max = min;
        }
        speedMps = rng.nextDouble(min, max == min ? min + 0.000001 : max);
    }
}
