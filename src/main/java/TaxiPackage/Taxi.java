package TaxiPackage;

import seta.smartcity.rideRequest.RideRequestOuterClass.RideRequest.Position;
import java.net.Socket;
import java.util.List;

public class Taxi {

    private final String id; // should be given by the admin
    private final String ip;
    private final int port;
    private final String topicString = "seta/smartcity/rides/";
    private final double chargeThreshold = 30; // if battery is below this value, go recharge
    private double battery = 100;
    private Position currentP = Position.newBuilder().setX(0).setY(0).build(); // these should be given by Admin

    public String getId() {
        return id;
    }

    public String getIp() { return ip; }

    public int getPort() { return port; }

    public String getTopicString() {
        return topicString;
    }

    public double getChargeThreshold() {
        return chargeThreshold;
    }

    public double getBattery() {
        return battery;
    }

    public void setBattery(double battery) {
        this.battery = battery;
    }

    public Position getCurrentP() {
        return currentP;
    }

    public void setCurrentP(Position currentP) {
        this.currentP = currentP;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    private String district = "district1";

    public Taxi(String id, String ip, int port){

        this.id = id;
        this.ip = ip;
        this.port = port;

    }

    public static void main(String args[]) {
        Taxi taxi = new Taxi("0", "localhost", 5678);
        // subscribe to the topic(s)
        TaxiComAdmin toAdmin = new TaxiComAdmin(taxi);
        TaxiDriver driver = new TaxiDriver(taxi);
        //toAdmin.start();
        driver.start();
    }

}
