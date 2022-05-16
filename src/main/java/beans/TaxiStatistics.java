package beans;

import javafx.util.Pair;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class TaxiStatistics {
    private String id;
    private double timestamp;
    private double kilometersTravelled;
    private double batteryLevel;
    private double ridesAccomplished;

    //@XmlElement(name="pollution")
    //private List<Double> pollutionLevel;

    public TaxiStatistics(){};

    public TaxiStatistics(String id, double timestamp, double kilometersTravelled, double batteryLevel, double ridesAccomplished) {
        this.id = id;
        this.timestamp = timestamp;
        this.kilometersTravelled = kilometersTravelled;
        this.batteryLevel = batteryLevel;
        this.ridesAccomplished = ridesAccomplished;
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
}
