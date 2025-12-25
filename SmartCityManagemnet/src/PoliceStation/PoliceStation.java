package PoliceStation;
import Building.Building;
import smartcitymanagemnet.EmergencyService;

public class PoliceStation extends Building implements EmergencyService {
    private int numberOfCars;
    private int numberOfOfficers;

    public PoliceStation(int id, String name, String address, String phone, int cars, int officers) {
        super(id, "POLICE", name, address, phone);
        this.numberOfCars = cars;
        this.numberOfOfficers = officers;
    }

    @Override
    public void triggerAlarm() {
        System.out.println("🚓 Police Units Dispatched from " + this.getName());
    }

    @Override
    public String getServiceDetails() {
        return "Patrol Cars: " + numberOfCars + " | Officers: " + numberOfOfficers;
    }

    @Override
    public String getEmergencyNumber() {
        return "104";
    }

    // Getters
    public int getCars() { return numberOfCars; }
    public int getOfficers() { return numberOfOfficers; }
}