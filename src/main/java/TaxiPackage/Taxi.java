package TaxiPackage;

import AdministratorPackage.AdministratorClient;
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
import io.grpc.Server;
import org.eclipse.paho.client.mqttv3.*;
import seta.smartcity.rideRequest.RideRequestOuterClass;
import taxi.communication.rechargeTokenService.RechargeTokenServiceOuterClass;

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

    public enum Input{
        RECHARGE,
        WORK,
        QUIT
    }

    private static String id; // default values
    private static String ip = "localhost";
    private static int port;
    private TokenQueue tokens;
    private List<TaxiBean> taxiList;
    private ElectionHandle electionHandle;
    private TaxiBean nextTaxi;
    private final String topicString = "seta/smartcity/rides/";
    private final double chargeThreshold = 30; // if battery is below this value, go recharge
    private static String district;
    private static TaxiStatistics taxiStats;
    private static int[] currentP; //= Position.newBuilder().setX(0).setY(0).build(); // these should be given by Admin
    private static Taxi instance;
    private Status currentStatus;
    private Input input;
    private int rechargeReqCounter = 0;
    private double rechargeRequestTimestamp = Double.MAX_VALUE;
    private RideRequestOuterClass.RideRequest reqToHandle = null;
    private List<RideRequestOuterClass.RideRequest> reqElected = new ArrayList<RideRequestOuterClass.RideRequest>();
    private Server server;
    private static Object statusLock = new Object();
    private static Object inputLock = new Object();
    private static Object rechargeLock = new Object();
    private static Object electedLock = new Object();
    private static Object rechargeTimestampLock = new Object();
    private static Object nextLock = new Object();
    private static Object leavingLock = new Object();
    private static List<Thread> taxiThreads;
    private boolean receivedManualInput = false;
    private TaxiMQTT taxiMQTT;
    private int waitingThreads = 0;
    private int waitingLeave = 0;
    private boolean initialized = false;

    public Input getInput() {
        return input;
    }

    public void setInput(Input input) {
        this.input = input;
        System.out.println(this.input);
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
        int idOffset = 3;
        port = 1884 + idOffset;
        id = String.valueOf(port);
        Taxi thisTaxi = getInstance();
        thisTaxi.setTaxiStats(new TaxiStatistics(id));
        thisTaxi.setBattery(100.0);
        thisTaxi.setTokens(new TokenQueue(new ArrayList<RechargeTokenServiceOuterClass.RechargeToken>()));
        thisTaxi.setElectionHandle(new ElectionHandle(thisTaxi));
        try {
            thisTaxi.setTaxiMQTT(new TaxiMQTT(thisTaxi));
        } catch (MqttException e) {
            e.printStackTrace();
        }

        Client client = Client.create();

        PM10Simulator pm10 = new PM10Simulator(new SimulatorData());

        taxiThreads = new ArrayList<Thread>(){
            {
                add(new Joining(thisTaxi, Status.JOINING, statusLock));
                add(new Leaving(thisTaxi, Status.LEAVING, statusLock));
                add(new Idle(thisTaxi, Status.IDLE, statusLock));
                add(new RequestRecharge(thisTaxi, Status.REQUEST_RECHARGE, statusLock));
                add(new GoRecharge(thisTaxi, Status.GO_RECHARGE, statusLock));
                add(new Working(thisTaxi, Status.WORKING, statusLock));
                add(new TaxiCommunicationServer(thisTaxi));
                //add(new TaxiRechargeTokenComm(thisTaxi));
                add(pm10);
                add(new Sensor(thisTaxi, pm10, client));
            }
        };

        for(Thread t: taxiThreads){
            t.start();
        }

        synchronized (statusLock) {
            while(thisTaxi.getWaitingThreads() < 5){
                System.out.println("Waiting for all threads to wait");
                    statusLock.wait();
                }
            thisTaxi.setInitialized();
            thisTaxi.setCurrentStatus(Status.JOINING);
            statusLock.notifyAll();
        }

        String userInput = null;
        Scanner in = new Scanner(System.in);

        while (userInput == null) {
            System.out.println("Type [quit] to exit the system or [recharge] to go to recharge");
            userInput = in.nextLine();
            synchronized (inputLock){
                System.out.println("Input lock in taxi");
                //thisTaxi.setReceivedManualInput(true);
                if (userInput.equals("quit")) {
                    while(thisTaxi.getInput() != null){
                        inputLock.wait();
                    }
                    thisTaxi.setInput(Input.QUIT);
                    return;
                }
                if(userInput.equals("recharge")){
                    while(thisTaxi.getInput() != null){
                        inputLock.wait();
                    }
                    thisTaxi.setInput(Input.RECHARGE);
                }
                //thisTaxi.setReceivedManualInput(false);
                inputLock.notifyAll();
            }
            userInput = null;
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
        System.out.println(this.taxiList);
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

    public static Object getInputLock() {
        return inputLock;
    }

    public static void setInputLock(Object inputLock) {
        Taxi.inputLock = inputLock;
    }

    public static List<Thread> getTaxiThreads() {
        return taxiThreads;
    }

    public static void setTaxiThreads(List<Thread> taxiThreads) {
        Taxi.taxiThreads = taxiThreads;
    }

    public static Object getRechargeLock() {
        return rechargeLock;
    }

    public static void setRechargeLock(Object rechargeLock) {
        Taxi.rechargeLock = rechargeLock;
    }

    public static Object getRechargeTimestampLock() {
        return rechargeTimestampLock;
    }

    public static void setRechargeTimestampLock(Object rechargeTimestampLock) {
        Taxi.rechargeTimestampLock = rechargeTimestampLock;
    }

    public TaxiBean getNextTaxi() {
        return nextTaxi;
    }

    public void setNextTaxi(TaxiBean nextTaxi) {
        this.nextTaxi = nextTaxi;
        System.out.println(printInformation("NEXT", this.nextTaxi.getId()));
    }

    public static Object getNextLock() {
        return nextLock;
    }

    public static void setNextLock(Object nextLock) {
        Taxi.nextLock = nextLock;
    }

    public TokenQueue getTokens() {
        return tokens;
    }

    public void setTokens(TokenQueue tokens) {
        this.tokens = tokens;
    }

    public static Object getStatusLock() {
        return statusLock;
    }

    public ElectionHandle getElectionHandle() {
        return electionHandle;
    }

    public void setElectionHandle(ElectionHandle electionHandle) {
        this.electionHandle = electionHandle;
    }

    public boolean isReceivedManualInput() {
        return receivedManualInput;
    }

    public void setReceivedManualInput(boolean receivedManualInput) {
        this.receivedManualInput = receivedManualInput;
    }

    public TaxiMQTT getTaxiMQTT() {
        return taxiMQTT;
    }

    public void setTaxiMQTT(TaxiMQTT taxiMQTT) {
        this.taxiMQTT = taxiMQTT;
    }

    public int getWaitingThreads() {
        return waitingThreads;
    }

    public void setWaitingThreads() {
        this.waitingThreads++;
    }

    public int getWaitingLeave() {
        return waitingThreads;
    }

    public void setWaitingLeave() {
        this.waitingLeave++;
    }

    public void initialize(){ // called within threads with the lock acquired
        if(getWaitingThreads() < 5){
            setWaitingThreads();
            try {
                statusLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else{
            statusLock.notifyAll();
            waitingThreads = 0;
        }
    }

    public void setInitialized() {
        this.initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public List<RideRequestOuterClass.RideRequest> getReqElected() {
        return reqElected;
    }

    public void setReqElected(List<RideRequestOuterClass.RideRequest> reqElected) {
        this.reqElected = reqElected;
    }

    public void addReqElected(RideRequestOuterClass.RideRequest rideRequest){
        this.reqElected.add(rideRequest);
    }

    public static Object getElectedLock() {
        return electedLock;
    }

    public static void setElectedLock(Object electedLock) {
        Taxi.electedLock = electedLock;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public static Object getLeavingLock() {
        return leavingLock;
    }
}


