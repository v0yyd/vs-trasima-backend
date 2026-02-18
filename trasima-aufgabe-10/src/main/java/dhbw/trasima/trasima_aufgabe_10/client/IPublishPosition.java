package dhbw.trasima.trasima_aufgabe_10.client;

public interface IPublishPosition {

    void publishPosition(int id, double lat, double lon, double speed, double direction);

    void deleteVehicle(int id);
}

