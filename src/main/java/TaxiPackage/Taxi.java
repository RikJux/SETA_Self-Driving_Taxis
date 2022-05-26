package TaxiPackage;

import Simulator.PM10Simulator;
import Simulator.SimulatorData;
import beans.TaxiBean;
import beans.TaxiStatistics;
import beans.Taxis;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import javax.ws.rs.core.Response;
import java.util.*;
import TaxiPackage.Threads.*;
import org.eclipse.paho.client.mqttv3.*;
import seta.smartcity.rideRequest.RideRequestOuterClass;

import static Utils.Utils.*;

public class Taxi {

    public enum Status{
        JOINING,
        IDLE,
        REQUEST_RECHARGE,
        GO_RECHARGE,
        ELECTED,
        WORKING,
        LEAVING
    }

    public enum ManualInput{
        RECHARGE,
        QUIT
    }

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
    private Status currentStatus;
    private ManualInput manualInput;
    private int rechargeReqCounter = 0;
    private double rechargeRequestTimestamp = Double.MAX_VALUE;
    private Stack<Object> electionId = new Stack<Object>();
    private RideRequestOuterClass.RideRequest reqToHandle = null;
    private static Object statusLock = new Object();

    public ManualInput getManualInput() {
        return manualInput;
    }

    public void setManualInput(ManualInput manualInput) {
        this.manualInput = manualInput;
    }

    public Taxi(String id, String ip, int port){

        this.id = id;
        this.ip = ip;
        this.port = port;

    }

    static final String serverAddress = "http://localhost:1337";
    private static final String joinPath = serverAddress+"/taxi/join";
    private static final String leavePath = serverAddress+"/taxi/leave/";

    public static void main(String args[]) throws InterruptedException {
        // insert id manually ?
        int idOffset = 0;
        port = 1338 + idOffset;
        id = String.valueOf(port);
        Taxi thisTaxi = getInstance();
        thisTaxi.setTaxiStats(new TaxiStatistics(id));
        thisTaxi.setBattery(100.0);
        MqttClient mqttClient;
        String broker = "tcp://localhost:1883";

        ArrayList<Status> jS = new ArrayList<>();
        jS.add(Status.IDLE);
        Joining j = new Joining(thisTaxi, Status.JOINING, jS, statusLock);
        j.start();
        Leaving l = new Leaving(thisTaxi, Status.LEAVING, new ArrayList<>(), statusLock);
        l.start();

        ArrayList<Status> iS = new ArrayList<>();
        iS.add(Status.REQUEST_RECHARGE);
        iS.add(Status.WORKING);
        Idle idle = new Idle(thisTaxi, Status.IDLE, iS, statusLock, null);
        idle.start();

        ArrayList<Status> rS = new ArrayList<>();
        rS.add(Status.GO_RECHARGE);
        RequestRecharge requestRecharge = new RequestRecharge(thisTaxi, Status.REQUEST_RECHARGE, rS, statusLock);
        requestRecharge.start();

        ArrayList<Status> gS = new ArrayList<>();
        gS.add(Status.IDLE);
        GoRecharge goRecharge = new GoRecharge(thisTaxi, Status.GO_RECHARGE, gS, statusLock);
        goRecharge.start();

        ArrayList<Status> wS = new ArrayList<>();
        wS.add(Status.IDLE);
        wS.add(Status.REQUEST_RECHARGE);
        Working working = new Working(thisTaxi, Status.WORKING, wS, statusLock);
        working.start();


        synchronized (statusLock){
            Thread.sleep(1000);
            thisTaxi.setCurrentStatus(Status.JOINING);
            statusLock.notifyAll();

            }

        Client client = Client.create();
        // initialize all threads
        TaxiCommunicationServer communicationServer = new TaxiCommunicationServer(thisTaxi);
        PM10Simulator pm10 = new PM10Simulator(new SimulatorData());
        TaxiDriver drive = new TaxiDriver(thisTaxi);
        Sensor sensor = new Sensor(thisTaxi, pm10, client);

        // start all threads
        communicationServer.start();
        pm10.start();
        sensor.start();


        String userInput = null;
        Scanner in = new Scanner(System.in);

        while(userInput == null){
            System.out.println("[TAXI MAIN] Type [quit] to exit the system or [recharge] to go to recharge");
            userInput = in.nextLine();
            if(userInput.equals("quit")){ // leaving procedure
                thisTaxi.setManualInput(ManualInput.QUIT);
                synchronized (statusLock){ // has to wait for full recharge, so never enters the while, for now
                    while(thisTaxi.getCurrentStatus() != Status.IDLE){
                        System.out.println("[TAXI MAIN] Waiting to get idle to quit");
                        statusLock.wait();
                        if(thisTaxi.getCurrentStatus() == Status.IDLE){
                            System.out.println("[TAXI MAIN] Taxi ready to leave");
                            thisTaxi.setCurrentStatus(Status.LEAVING);
                        }
                    }
                }
                synchronized (thisTaxi){
                    thisTaxi.setCurrentStatus(Status.LEAVING);
                    thisTaxi.notifyAll();
                }
                System.out.println(thisTaxi.getCurrentStatus());
                /*
                leaveRequest(client);
                TaxiCommunicationClient announceLeaveThread = new TaxiCommunicationClient(thisTaxi, false);
                announceLeaveThread.start();
                announceLeaveThread.join();

                 */
                communicationServer.interrupt();
                pm10.interrupt();
                drive.interrupt();
                sensor.interrupt();
                return;
            }

            if(userInput.equals("recharge")){
                thisTaxi.setManualInput(ManualInput.RECHARGE);
                // tell the driver to go recharge
                thisTaxi.setCurrentStatus(Status.REQUEST_RECHARGE);
                thisTaxi.setRechargeRequestTimestamp(System.currentTimeMillis());
                TaxiRechargeComm r = new TaxiRechargeComm(thisTaxi);
                r.start();
                //r.join(); // wait for all responses
                synchronized (thisTaxi){
                    while(thisTaxi.getCurrentStatus() == Status.GO_RECHARGE){
                        thisTaxi.wait();
                        if(thisTaxi.getCurrentStatus() == Status.IDLE){
                            System.out.println("[TAXI MAIN] Recharge done.");
                            thisTaxi.notifyAll();
                        }
                    }
                }
            }

            userInput = null;
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
            System.out.println("[TAXI MAIN] Join impossible: taxi" + id + "can't reach the server");
            return null;
        }

        if(clientResponse.getStatus() == Response.Status.NOT_ACCEPTABLE.getStatusCode()){
            System.out.println("[TAXI MAIN] Join impossible: duplicated id " + id);
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
            System.out.println("[TAXI MAIN] Impossible to leave: taxi " + id + " can't reach the server");
            return;
        }

        if(clientResponse.getStatus() == Response.Status.NOT_FOUND.getStatusCode()){
            System.out.println("[TAXI MAIN] Impossible to leave: can't find id " + id);
            return;
        }else{
            System.out.println("[TAXI MAIN] Taxi " + id + " left the system");
        }
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

    public Status getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(Status currentStatus) {
        this.currentStatus = currentStatus;
        System.out.println("Taxi " + id + " is in status " + this.currentStatus);
    }

    public double getRechargeRequestTimestamp() {
        return rechargeRequestTimestamp;
    }

    public void setRechargeRequestTimestamp(double rechargeRequestTimestamp) {
        this.rechargeRequestTimestamp = rechargeRequestTimestamp;
    }

    public void incrementRechargeReqCounter(){
        this.rechargeReqCounter = rechargeReqCounter + 1;
    }

    public void zeroRechargeReqCounter(){
        this.rechargeReqCounter = 0;
    }

    public int getRechargeReqCounter() {
        return rechargeReqCounter;
    }

    public void setRechargeReqCounter(int rechargeReqCounter) {
        this.rechargeReqCounter = rechargeReqCounter;
    }

    public Stack<Double> fillElectionId(double distance) {

        Stack<Double> electionId = new Stack<Double>();

        electionId.push(Double.parseDouble(this.getId()));
        electionId.push(this.getBattery());
        electionId.push(-1*distance);
        electionId.push(this.getCurrentStatus()==Status.IDLE ? 1d : 0d);

        return electionId;
    }

    public RideRequestOuterClass.RideRequest getReqToHandle() {
        return reqToHandle;
    }

    public void setReqToHandle(RideRequestOuterClass.RideRequest reqToHandle) {
        this.reqToHandle = reqToHandle;
    }

}
