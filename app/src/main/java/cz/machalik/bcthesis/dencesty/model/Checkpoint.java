package cz.machalik.bcthesis.dencesty.model;

/**
 * Lukáš Machalík
 */
public class Checkpoint {

    public final int id;
    public final int meters;
    public final double latitude;
    public final double longitude;

    public Checkpoint(int id, int meters, double latitude, double longitude) {
        this.id = id;
        this.meters = meters;
        this.latitude = latitude;
        this.longitude = longitude;
    }

}
