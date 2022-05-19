package TaxiPackage;

import Simulator.Buffer;
import Simulator.Measurement;
import Simulator.PM10Simulator;
import Simulator.SimulatorData;
import beans.Statistics;
import beans.TaxiBean;
import beans.TaxiStatistics;
import beans.Taxis;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.eclipse.paho.client.mqttv3.MqttException;

import javax.ws.rs.core.Response;
import java.util.List;

import java.util.Random;
import java.util.Scanner;

public class Taxi {

    private static String id; // default values
    private static String ip = "localhost";
    private static int port;
    private List<TaxiBean> taxiList;
    private final String topicString = "seta/smartcity/rides/";
    private final double chargeThreshold = 30; // if battery is below this value, go recharge
    private static String district;
    private static TaxiStatistics taxiStats;
    private static int[] currentP; //= Position.newBuilder().setX(0).setY(0).build(); // these should be given by Admin
    private static Taxi instance;
    private boolean isIdle;

    public boolean isIdle() {
        return isIdle;
    }

    public void setIdle(boolean idle) {
        isIdle = idle;
    }

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

    public int[] getCurrentP() {
        return currentP;
    }

    public void setCurrentP(int[] currentP) {
        this.currentP = currentP;
    }

    public int getX() {return getCurrentP()[0];}

    public int getY() {return getCurrentP()[1];}

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public List<TaxiBean> getTaxiList() {
        return taxiList;
    }

    public void setTaxiList(List<TaxiBean> taxiList) {
        this.taxiList = taxiList;
    }

    public synchronized TaxiStatistics getTaxiStats() {
        return taxiStats;
    } // cannot be sync!

    public void setTaxiStats(TaxiStatistics taxiStats) {
        this.taxiStats = taxiStats;
    }

    public double getBattery(){return this.getTaxiStats().getBatteryLevel();}

    public void setBattery(double b){this.getTaxiStats().setBatteryLevel(b);}

    public void lowerBattery(double b){this.getTaxiStats().setBatteryLevel(this.getTaxiStats().getBatteryLevel() - b);}

    public double getKilometers(){return this.getTaxiStats().getKilometersTravelled();}

    public void setKilometers(double b){this.getTaxiStats().setKilometersTravelled(b);}

    public void addKilometers(double b){this.getTaxiStats().setKilometersTravelled(b + this.getTaxiStats().getKilometersTravelled());}

    public double getRidesAccomplished(){return this.getTaxiStats().getRidesAccomplished();}

    public void addRideAccomplished(){this.getTaxiStats().setRidesAccomplished(this.getTaxiStats().getRidesAccomplished() + 1);}

    public synchronized static Taxi getInstance(){
        if(instance==null)
            instance = new Taxi(id, ip, port);
        return instance;
    }

    public Taxi(String id, String ip, int port){

        this.id = id;
        this.ip = ip;
        this.port = port;

    }

    static final String serverAddress = "http://localhost:1337";
    private static final String joinPath = serverAddress+"/taxi/join";
    private static final String leavePath = serverAddress+"/taxi/leave/";

    public static void main(String args[]) {
        // insert id manually ?
        id = "10";
        port = 1338 + Integer.parseInt(id);
        Taxi thisTaxi = getInstance();
        thisTaxi.setTaxiStats(new TaxiStatistics(id));
        thisTaxi.setBattery(100.0);
        Client client = Client.create();
        Taxis taxis = joinRequest(client);
        if(taxis == null){
            System.out.println("Cannot enter the system");
            return;
        }
        thisTaxi.setTaxiList(taxis.getTaxiList());
        thisTaxi.setCurrentP(taxis.randomCoord());
        thisTaxi.setDistrict(computeDistrict(currentP));
        System.out.println("Taxi " + id + " joined in " + district);
        thisTaxi.setIdle(true); // ready to accept requests

        // initialize all threads
        TaxiCommunicationServer communicationServer = new TaxiCommunicationServer(thisTaxi);
        PM10Simulator pm10 = new PM10Simulator(new SimulatorData());
        TaxiDriver drive = new TaxiDriver(thisTaxi);
        Sensor sensor = new Sensor(thisTaxi, pm10, client);

        // start all threads
        communicationServer.start();
        new TaxiCommunicationClient(thisTaxi, true).start();
        pm10.start();
        drive.start();
        sensor.start();

        String quit = null;
        Scanner in = new Scanner(System.in);

        while(quit == null){
            System.out.println("Type [quit] to exit the system or [recharge] to go to recharge");
            quit = in.nextLine();
            if(quit.equals("quit")){ // leaving procedure
                System.out.println(thisTaxi.getTaxiList());
                leaveRequest(client);
                new TaxiCommunicationClient(thisTaxi, false).start();
                communicationServer.interrupt();
                pm10.interrupt();
                drive.interrupt();
                sensor.interrupt();
                return;
            }
            quit = null;
        }

    }

    private static double avgPollution(List<Measurement> pollution){

        double sum = 0;
        for(Measurement m: pollution){
            sum += m.getValue();
        }

        return sum/pollution.size();
    }

    private static Taxi handleStatistics2(Client client, Taxi thisTaxi, Buffer pm10Buffer){
        List<Measurement> pollution = pm10Buffer.readAllAndClean();
        thisTaxi.getTaxiStats().setPollution(avgPollution(pollution));
        thisTaxi.getTaxiStats().setTimestamp(System.currentTimeMillis());
        sendStatistics(client, thisTaxi.getTaxiStats());
        double battLeft = thisTaxi.getTaxiStats().getBatteryLevel();
        thisTaxi.setTaxiStats(new TaxiStatistics(id));
        thisTaxi.setBattery(battLeft);

        return thisTaxi;
    }

    private static void sendStatistics(Client client, TaxiStatistics taxiStats){
        ClientResponse clientResponse = null;

        WebResource webResource = client.resource(serverAddress+"/statistics/post/"+id);
        String input = new Gson().toJson(taxiStats);
        System.out.println(input);

        try {
            clientResponse = webResource.type("application/json").post(ClientResponse.class, input);
        } catch (ClientHandlerException e) {
            System.out.println("Error");
            return;
        }

    }

    private static Taxis joinRequest(Client client){
        ClientResponse clientResponse = null;

        TaxiBean t = new TaxiBean(id, ip, port);
        WebResource webResource = client.resource(joinPath);
        String input = new Gson().toJson(t);

        try {
            clientResponse = webResource.type("application/json").post(ClientResponse.class, input);
        } catch (ClientHandlerException e) {
            System.out.println("Join impossible: taxi" + id + "can't reach the server");
            return null;
        }

        if(clientResponse.getStatus() == Response.Status.NOT_ACCEPTABLE.getStatusCode()){
            System.out.println("Join impossible: duplicated id " + id);
            return null; // duplicated id
        }
        return new Gson().fromJson(clientResponse.getEntity(String.class), Taxis.class);
    }

    private static void leaveRequest(Client client){
        ClientResponse clientResponse = null;

        WebResource webResource = client.resource(leavePath+id);

        try {
            clientResponse = webResource.type("application/json").delete(ClientResponse.class);
        } catch (ClientHandlerException e) {
            System.out.println("Impossible to leave: taxi " + id + " can't reach the server");
            return;
        }

        if(clientResponse.getStatus() == Response.Status.NOT_FOUND.getStatusCode()){
            System.out.println("Impossible to leave: can't find id " + id);
            return;
        }else{
            System.out.println("Taxi " + id + " left the system");
        }
    }

    private static String computeDistrict(int[] coord){
        int x = coord[0];
        int y = coord[1];
        String distN;

        if(y < 5){
            // we are in the upper city
            if(x < 5){
                distN = "1";
            }else{
                distN = "2";
            }
        }else{
            // we are in the lower city
            if(x < 5){
                distN = "4";
            }else{
                distN = "3";
            }
        }

        return "district_" + distN;
    }

}
