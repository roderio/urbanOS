package Hospital;
import Building.Building;
import smartcitymanagemnet.EmergencyService;

public class Hospital extends Building implements EmergencyService {
    private int numberOfBeds;
    private int numberOfDoctors;

    public Hospital(int id, String name, String address, String phone, int beds, int doctors) {
        super(id, "HOSPITAL", name, address, phone);
        this.numberOfBeds = beds;
        this.numberOfDoctors = doctors;
    }

    // Getters
    public int getBeds() { return numberOfBeds; }
    public int getDoctors() { return numberOfDoctors; }

    @Override
    public void triggerAlarm() {
        System.out.println("🚑 Hospital Lockdown Initiated at " + this.getName());
    }

    @Override
    public String getServiceDetails() {
        return "Emergency Beds: " + numberOfBeds + " | Doctors: " + numberOfDoctors;
    }

    @Override
    public String getEmergencyNumber() {
        return "122";
    }
}