package Building;

import smartcitymanagemnet.Displayable; // Import the interface

public abstract class Building implements Displayable {
    protected int id;
    protected String name;
    protected String type;
    protected String address;
    protected String phone;


    public Building(int id, String type, String name, String address, String phone) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.address = address;
        this.phone =  phone;
    }

    public abstract String getServiceDetails();

    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }

    @Override
    public String getDisplayName() {
        return this.name;
    }

    @Override
    public String getDisplayAddress() {
        return this.address;
    }

    @Override
    public String getDisplayDetails() {
        return this.getServiceDetails();
    }

    @Override
    public String getDisplayResponder() {
        return "-";
    }
}