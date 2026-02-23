package dhbw.trasima.trasima_aufgabe_10.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Starts a local simulation and publishes vehicle states to the Aufgabe 10 REST server.
 *
 * <p>CLI args:</p>
 * <ul>
 *   <li>{@code --config <path>} JSON config file (default: {@code trasima-aufgabe-10/sim-config.json}, then
 *   {@code sim-config.json}, otherwise built-in defaults)</li>
 *   <li>{@code --vehicles <n>} overrides config</li>
 *   <li>{@code --baseUrl <url>} overrides config</li>
 * </ul>
 */
public final class RestSimulationClientMain {

    public static void main(String[] args) {
        // Load configuration from JSON (or fall back to defaults).
        SimulationConfig config = loadConfig(args);

        // Optional CLI overrides (useful for quick experiments without touching the JSON file).
        int vehiclesOverride = intArg(args, "--vehicles", -1);
        if (vehiclesOverride > 0) {
            config.vehicles = vehiclesOverride;
        }
        String baseUrlOverride = stringArg(args, "--baseUrl", null);
        if (baseUrlOverride != null && !baseUrlOverride.isBlank()) {
            config.baseUrl = baseUrlOverride;
        }

        RestPositionPublisher publisher = new RestPositionPublisher(config.baseUrl);
        List<VehicleSimulator> simulators = new ArrayList<>(Math.max(0, config.vehicles));
        AtomicBoolean cleanupDone = new AtomicBoolean(false);

        System.out.println("Starte REST Simulation mit " + config.vehicles + " Fahrzeugen.");
        System.out.println("REST Target: " + config.baseUrl + "/api/trasima/vehicles/{id}");
        System.out.println("Spawn: center=(" + config.spawn.centerLat + "," + config.spawn.centerLon + ") radius="
                + config.spawn.radiusMeters + "m");
        System.out.println("Destination: (" + config.destination.lat + "," + config.destination.lon + ")");
        System.out.println("Speed: " + config.speed.minMps + ".." + config.speed.maxMps + " m/s; interval="
                + config.updateIntervalMillis + "ms");

        // Create N vehicles with ids 1..N.
        for (int i = 0; i < config.vehicles; i++) {
            int id = i + 1;
            simulators.add(new VehicleSimulator(id, config, publisher, System.nanoTime() + id));
        }

        // Publish an initial state for each vehicle so markers appear immediately.
        for (VehicleSimulator simulator : simulators) {
            simulator.publishInitial();
        }

        // Single scheduler thread that ticks all vehicles at a fixed interval.
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rest-sim-tick");
            t.setDaemon(false);
            return t;
        });

        // Ensure we cleanup on Ctrl+C / JVM shutdown.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdownNow();
            cleanup(config, publisher, simulators, cleanupDone);
        }, "rest-sim-shutdown"));

        // Interval is also used as our simulation timestep (dt).
        long intervalMs = Math.max(100, config.updateIntervalMillis);
        double dtSeconds = intervalMs / 1000.0;
        Instant start = Instant.now();
        Duration duration = config.runDurationSeconds > 0 ? Duration.ofSeconds(config.runDurationSeconds) : null;

        scheduler.scheduleAtFixedRate(() -> {
            try {
                // If a finite runDurationSeconds is configured, stop after the time has elapsed.
                if (duration != null && Duration.between(start, Instant.now()).compareTo(duration) >= 0) {
                    scheduler.shutdown();
                    return;
                }
                // Advance all vehicles one tick and publish their updated state.
                for (VehicleSimulator simulator : simulators) {
                    simulator.tick(dtSeconds);
                }
            } catch (Exception e) {
                System.out.println("Simulation Fehler: " + e.getMessage());
            }
        }, 0, intervalMs, TimeUnit.MILLISECONDS);

        // Block main thread until finished.
        try {
            if (duration == null) {
                // "Forever" mode: wait essentially indefinitely (until shutdown hook triggers).
                scheduler.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            } else {
                // Finite mode: wait a bit longer than the configured duration to allow the scheduler to stop.
                scheduler.awaitTermination(duration.toMillis() + intervalMs * 2, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            scheduler.shutdownNow();
            cleanup(config, publisher, simulators, cleanupDone);
        }
    }

    private static void cleanup(
            SimulationConfig config,
            RestPositionPublisher publisher,
            List<VehicleSimulator> simulators,
            AtomicBoolean cleanupDone
    ) {
        // cleanup() can be called from both the shutdown hook and the finally block; ensure it runs only once.
        if (!cleanupDone.compareAndSet(false, true)) {
            return;
        }
        if (!config.deleteOnExit) {
            return;
        }
        // Delete resources so the server/map doesn't keep old vehicles between runs.
        for (VehicleSimulator simulator : simulators) {
            publisher.deleteVehicle(simulator.id());
        }
    }

    private static SimulationConfig loadConfig(String[] args) {
        // Highest priority: explicit --config <path>.
        String configPath = stringArg(args, "--config", null);
        if (configPath != null && !configPath.isBlank()) {
            return readConfig(Path.of(configPath));
        }

        // Next: module-local config file.
        Path preferred = Path.of("trasima-aufgabe-10", "sim-config.json");
        if (Files.exists(preferred)) {
            return readConfig(preferred);
        }

        // Next: config in current working directory.
        Path local = Path.of("sim-config.json");
        if (Files.exists(local)) {
            return readConfig(local);
        }

        // Fallback: hardcoded defaults.
        return new SimulationConfig();
    }

    private static SimulationConfig readConfig(Path path) {
        // Ignore unknown JSON fields so the config can evolve without breaking older binaries.
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            String json = Files.readString(path);
            SimulationConfig config = mapper.readValue(json, SimulationConfig.class);
            return sanitize(config == null ? new SimulationConfig() : config);
        } catch (IOException e) {
            System.out.println("Konnte Config nicht lesen (" + path + "): " + e.getMessage());
            return new SimulationConfig();
        }
    }

    private static SimulationConfig sanitize(SimulationConfig config) {
        // Defensive defaults if JSON omitted nested objects.
        if (config.spawn == null) {
            config.spawn = new SimulationConfig.Spawn();
        }
        if (config.destination == null) {
            config.destination = new SimulationConfig.Destination();
        }
        if (config.speed == null) {
            config.speed = new SimulationConfig.Speed();
        }
        // Sanity checks so we don't start with invalid values.
        if (config.vehicles < 1) {
            config.vehicles = 1;
        }
        if (config.updateIntervalMillis < 100) {
            config.updateIntervalMillis = 100;
        }
        if (config.arrivalRadiusMeters < 0.0) {
            config.arrivalRadiusMeters = 0.0;
        }
        if (config.onArrival == null || config.onArrival.isBlank()) {
            config.onArrival = "respawn";
        }
        if (config.baseUrl == null || config.baseUrl.isBlank()) {
            config.baseUrl = "http://localhost:8080";
        }
        return config;
    }

    private static int intArg(String[] args, String key, int defaultValue) {
        String value = stringArg(args, key, null);
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    private static String stringArg(String[] args, String key, String defaultValue) {
        // Looks for "<key> <value>" pairs in the raw args array.
        for (int i = 0; i < args.length - 1; i++) {
            if (key.equals(args[i])) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }
}
