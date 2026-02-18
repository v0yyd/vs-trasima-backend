package dhbw.trasima.trasima_aufgabe_10.model;

public class V2State {

    public int id;
    public double lat;
    public double lon;
    public double speed;
    public double direction;

    public V2State() {
    }

    public V2State(int id, double lat, double lon, double speed, double direction) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
        this.speed = speed;
        this.direction = direction;
    }
}

