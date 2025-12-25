package smartcitymanagemnet;

public class Emergency implements Displayable {

    private int id;
    private String type;
    private double lat;
    private double lon;
    private String status;
    private String time;
    private String addressString;
    private String responder;

    // Updated Constructor: Now accepts 'addressString'
    public Emergency(int id, String type, double lat, double lon, String status, String time, String addressString, String responder) {
        this.id = id;
        this.type = type;
        this.lat = lat;
        this.lon = lon;
        this.status = status;
        this.time = time;
        this.addressString = addressString;
        this.responder = responder;
    }

    public int getId() { return id; }
    public String getType() { return type; }

    public double getLat() {
        return lat;
    }
    public double getLon() {
        return lon;
    }

    // --- INTERFACE IMPLEMENTATION ---

    @Override
    public String getDisplayName() {
        String emoji = "⚠";
        if (type.contains("Fire")) emoji = "🔥";
        else if (type.contains("Medical")) emoji = "🚑";
        else if (type.contains("Accident")) emoji = "🚗";
        else if (type.contains("Police")) emoji = "🚓";

        return emoji + " " + type;
    }

    @Override
    public String getDisplayAddress() {
        // Return the stored string.
        // If it's empty/null, fall back to coordinates.
        if (addressString == null || addressString.isEmpty()) {
            return String.format("Lat: %.4f, Lon: %.4f", lat, lon);
        }
        return addressString;
    }

    @Override
    public String getDisplayDetails() {
        return "Status: " + status + " (" + time + ")";
    }

    @Override
    public String getDisplayResponder() {
        // If null (old database rows), show "Waiting..."
        return (responder == null) ? "Waiting..." : responder;
    }
}