package dhbw.trasima.trasima_aufgabe07.client;

import dhbw.trasima.trasima_bis_5.VirtualVehicle;
import java.util.ArrayList;
import java.util.List;

/**
 * Der Simulations-Client startet die eigentliche Simulation.
 * 
 * - Erstellt eine Anzahl an virtuellen Fahrzeugen (VirtualVehicle).
 * - Jedes Fahrzeug bekommt einen PositionPublisher (gRPC-Client).
 * - Startet jedes Fahrzeug in einem eigenen Thread.
 * - Wartet, bis alle Fahrzeuge ihre Fahrt beendet haben.
 */
public class SimulationClient {

    public static void main(String[] args) {
        // Standardmäßig 3 Fahrzeuge, falls kein Argument übergeben wurde
        int numberOfVehicles = 3;
        if (args.length > 0) {
            numberOfVehicles = Integer.parseInt(args[0]);
        }

        System.out.println("Starte gRPC Simulation mit " + numberOfVehicles + " Virtual Vehicles.");

        List<Thread> threads = new ArrayList<>();
        List<PositionPublisher> publishers = new ArrayList<>();

        try {
            for (int i = 0; i < numberOfVehicles; i++) {
                // Ein gRPC-Publisher für jedes Fahrzeug
                PositionPublisher publisher = new PositionPublisher("192.168.110.7", 50051);
                publishers.add(publisher);

                // Das eigentliche Fahrzeug-Objekt aus trasima_bis_5
                VirtualVehicle v2 = new VirtualVehicle(
                        i + 1,
                        Math.random() * 10,
                        Math.random() * 10,
                        0.5 + Math.random(),
                        (id, x, y, speed) -> {
                            // Lambda-Funktion: Sendet Daten via gRPC und gibt Status aus
                            publisher.publishPosition(id, x, y, speed);
                            System.out.println("V2-" + id + " Position gesendet.");
                        }
                );

                // Fahrzeug in einem neuen Thread starten
                Thread t = new Thread(v2);
                threads.add(t);
                t.start();
            }

            // Warten, bis alle Threads fertig sind
            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } finally {
            // Alle Verbindungen am Ende schließen
            for (PositionPublisher p : publishers) {
                p.close();
            }
        }

        System.out.println("Simulation abgeschlossen.");
    }
}
