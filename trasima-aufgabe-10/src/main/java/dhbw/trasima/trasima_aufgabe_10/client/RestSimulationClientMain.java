package dhbw.trasima.trasima_aufgabe_10.client;

import dhbw.trasima.trasima_bis_5.VirtualVehicle;

public final class RestSimulationClientMain {

    public static void main(String[] args) {
        int numberOfVehicles = intArg(args, "--vehicles", 10);
        String baseUrl = stringArg(args, "--baseUrl", "http://localhost:8080");

        RestPositionPublisher publisher = new RestPositionPublisher(baseUrl);
        Thread[] threads = new Thread[numberOfVehicles];

        System.out.println("Starte REST Simulation mit " + numberOfVehicles + " Virtual Vehicles.");
        System.out.println("REST Target: " + baseUrl + "/api/trasima/vehicles/{id}");

        for (int i = 0; i < numberOfVehicles; i++) {
            int id = i + 1;
            VirtualVehicle v2 = new VirtualVehicle(
                    id,
                    Math.random() * 10,
                    Math.random() * 10,
                    0.5 + Math.random(),
                    publisher
            );
            threads[i] = new Thread(v2);
            threads[i].start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        for (int i = 0; i < numberOfVehicles; i++) {
            publisher.deleteVehicle(i + 1);
        }

        System.out.println("Simulation abgeschlossen (Fahrzeuge gelöscht).");
    }

    private static int intArg(String[] args, String key, int defaultValue) {
        String value = stringArg(args, key, null);
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    private static String stringArg(String[] args, String key, String defaultValue) {
        for (int i = 0; i < args.length - 1; i++) {
            if (key.equals(args[i])) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }
}

