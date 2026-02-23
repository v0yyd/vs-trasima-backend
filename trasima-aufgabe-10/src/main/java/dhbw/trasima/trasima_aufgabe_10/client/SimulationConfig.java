package dhbw.trasima.trasima_aufgabe_10.client;

/**
 * JSON configuration for {@link RestSimulationClientMain}.
 *
 * <p>Fields are public so Jackson can bind without extra boilerplate.</p>
 */
public final class SimulationConfig {

    /**
     * Base URL of the REST server (without trailing path).
     *
     * <p>Example: {@code http://localhost:8080}</p>
     */
    public String baseUrl = "http://localhost:8080";

    /** How many vehicles to simulate (ids will be 1..N). */
    public int vehicles = 10;

    /**
     * How often positions are published to the REST server.
     *
     * <p>Smaller values = smoother marker movement on the web map, but more HTTP requests.</p>
     */
    public long updateIntervalMillis = 1000;

    /**
     * How long the simulation should run.
     *
     * <p>0 = run forever until you stop it (Ctrl+C).</p>
     */
    public long runDurationSeconds = 0;

    /**
     * Whether the client should delete all created vehicles on exit.
     *
     * <p>If you want the markers to remain on the map after stopping the client, set this to {@code false}.</p>
     */
    public boolean deleteOnExit = true;

    /** Vehicles spawn within this radius around the center. */
    public Spawn spawn = new Spawn();

    /** Vehicles drive towards this fixed destination. */
    public Destination destination = new Destination();

    /** Vehicle speed in meters/second. */
    public Speed speed = new Speed();

    /** Consider destination reached when within this radius (meters). */
    public double arrivalRadiusMeters = 25.0;

    /** What to do when a vehicle arrives: "respawn" (default) or "stop". */
    public String onArrival = "respawn";

    public static final class Spawn {
        /** Mannheim-ish defaults. */
        public double centerLat = 49.4875;
        public double centerLon = 8.4660;
        public double radiusMeters = 4000.0;
    }

    public static final class Destination {
        /** Example: SAP Arena Mannheim (adjust as needed). */
        public double lat = 49.4636;
        public double lon = 8.5147;
    }

    public static final class Speed {
        public double minMps = 5.0;
        public double maxMps = 15.0;
    }
}
