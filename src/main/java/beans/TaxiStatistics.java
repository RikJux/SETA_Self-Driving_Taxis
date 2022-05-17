package beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TaxiStatistics {
    private String id;
    private double timestamp;
    private double kilometersTravelled;
    private double batteryLevel;
    private double ridesAccomplished;
    private double pollution;

    public TaxiStatistics(){};

    public TaxiStatistics(String id) {
        this.id = id;
        this.timestamp = 0;
        this.kilometersTravelled = 0;
        this.batteryLevel = 0;
        this.ridesAccomplished = 0;
        this.pollution = 0;
    }

    @Override
    public String toString() {
        return "TaxiStatistics{" +
                "id='" + id + '\'' +
                ", timestamp=" + timestamp +
                ", kilometersTravelled=" + kilometersTravelled +
                ", batteryLevel=" + batteryLevel +
                ", ridesAccomplished=" + ridesAccomplished +
                ", pollution=" + pollution +
                '}';
    }

    public TaxiStatistics(String id, double timestamp, double kilometersTravelled, double batteryLevel, double ridesAccomplished, double pollution) {
        this.id = id;
        this.timestamp = timestamp;
        this.kilometersTravelled = kilometersTravelled;
        this.batteryLevel = batteryLevel;
        this.ridesAccomplished = ridesAccomplished;
        this.pollution = pollution;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(double timestamp) {
        this.timestamp = timestamp;
    }

    public double getKilometersTravelled() {
        return kilometersTravelled;
    }

    public void setKilometersTravelled(double kilometersTravelled) {
        this.kilometersTravelled = kilometersTravelled;
    }

    public double getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(double batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public double getRidesAccomplished() {
        return ridesAccomplished;
    }

    public void setRidesAccomplished(double ridesAccomplished) {
        this.ridesAccomplished = ridesAccomplished;
    }

    public double getPollution() {
        return pollution;
    }

    public void setPollution(double pollution) {
        this.pollution = pollution;
    }
}
