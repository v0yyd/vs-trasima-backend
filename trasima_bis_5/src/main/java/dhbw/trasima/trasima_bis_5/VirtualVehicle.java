package dhbw.trasima.trasima_bis_5;

public class VirtualVehicle implements Runnable {

    private final int id;
    private double x;
    private double y;
    private final double speed;

    private final IPositionPublisher publisher;

    public VirtualVehicle(int id, double startX, double startY, double speed, IPositionPublisher publisher) {
        this.id = id;
        this.x = startX;
        this.y = startY;
        this.speed = speed;
        this.publisher = publisher;
    }

    @Override
    public void run() {
        System.out.println("V2-" + id + " gestartet.");

        long startTime = System.currentTimeMillis();
        long runtime = 30_000;

        while (System.currentTimeMillis() - startTime < runtime) {
            move();
            publisher.publishPosition(id, x, y, speed);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("V2-" + id + " beendet Fahrt.");
    }

    private void move() {
        x += speed;
        y += speed;
    }
}
