package cz.machalik.bcthesis.dencesty.model;

/**
 * Represents a point on a race track.
 *
 * @author Lukáš Machalík
 */
public class Checkpoint {

    /**
     * Check ID.
     */
    public final int id;
    /**
     * Real distance in meters from race start.
     */
    public final int meters;
    /**
     * Latitude coordinate.
     */
    public final double latitude;
    /**
     * Longitude coordinate.
     */
    public final double longitude;

    /**
     * Creates new point on a race track with given parameters.
     * @param id check ID
     * @param meters real distance in meters from race start
     * @param latitude latitude coordinate
     * @param longitude longitude coordinate
     */
    public Checkpoint(int id, int meters, double latitude, double longitude) {
        this.id = id;
        this.meters = meters;
        this.latitude = latitude;
        this.longitude = longitude;
    }

}
