package FireStation;
import Building.Building;
import smartcitymanagemnet.EmergencyService;

public class FireStation extends Building implements EmergencyService {
    private int numberOfTrucks;
    private int numberOfFighters;

    public FireStation(int id, String name, String address, String phone, int trucks, int fighters) {
        super(id, "FIRE", name, address, phone);
        this.numberOfTrucks = trucks;
        this.numberOfFighters = fighters;
    }

    @Override
    public String getServiceDetails() {
        return "Fire Trucks: " + numberOfTrucks + " | Firefighters: " + numberOfFighters;
    }

    @Override
    public void triggerAlarm() {
        System.out.println("🚒 Fire Alarm! Trucks Deploying from " + this.getName());
    }

    @Override
    public String getEmergencyNumber() {
        return "115";
    }

    // Getters
    public int getTrucks() { return numberOfTrucks; }
    public int getFighters() { return numberOfFighters; }
}