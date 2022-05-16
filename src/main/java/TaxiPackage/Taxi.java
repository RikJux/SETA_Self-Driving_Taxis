package TaxiPackage;

import beans.TaxiBean;
import beans.TaxiStatistics;
import beans.Taxis;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import javafx.util.Pair;
import seta.smartcity.rideRequest.RideRequestOuterClass;
import seta.smartcity.rideRequest.RideRequestOuterClass.RideRequest.Position;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import java.io.*;

public class Taxi {

    private static String id = "0"; // default values
    private static String ip = "localhost";
    private static int port = 9999;
    private List<TaxiBean> taxiList;
    private final String topicString = "seta/smartcity/rides/";
    private final double chargeThreshold = 30; // if battery is below this value, go recharge
    private double battery = 100;
    private static int[] currentP; //= Position.newBuilder().setX(0).setY(0).build(); // these should be given by Admin

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

    public int[] getCurrentP() {
        return currentP;
    }

    public void setCurrentP(int[] currentP) {
        this.currentP = currentP;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    private static String district;

    public Taxi(String id, String ip, int port){

        this.id = id;
        this.ip = ip;
        this.port = port;

    }

    private static final String serverAddress = "http://localhost:1337";
    private static final String joinPath = serverAddress+"/taxi/join";
    private static final String leavePath = serverAddress+"/taxi/leave/"+id;

    public static void main(String args[]) {
        // insert id manually ?
        Client client = Client.create();
        Taxis taxis = joinRequest(client);
        if(taxis == null){
            return;
        }
        List<TaxiBean> taxiList= taxis.getTaxiList();
        currentP = taxis.randomCoord();
        district = computeDistrict(currentP);
        System.out.println("Taxi " + id + " joined in " + district);
        for(TaxiBean t: taxiList){
            System.out.println(t.getId() + " " + t.getIp() + " " + t.getPort());
        }
        sendStatistics(client);
        /*
        BufferedReader obj = new BufferedReader(new InputStreamReader(System.in));
        String command = null;
        System.out.println("Type \'quit\' to quit: ");
        try {
            command = obj.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(command.equals("quit")){leaveRequest(client);}*/
    }

    private static void sendStatistics(Client client){
        ClientResponse clientResponse = null;

        TaxiStatistics taxiStats= new TaxiStatistics(id, 0.0, 100.0 , 100.0, 0);

        WebResource webResource = client.resource(serverAddress+"/statistics/post/"+id);
        String input = new Gson().toJson(taxiStats);

        try {
            clientResponse = webResource.type("application/json").post(ClientResponse.class, input);
        } catch (ClientHandlerException e) {
            System.out.println("Error");
            return;
        }

        System.out.println(clientResponse.toString());

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

        return clientResponse.getEntity(Taxis.class); // taxi successfully joined
    }

    private static void leaveRequest(Client client){
        ClientResponse clientResponse = null;

        WebResource webResource = client.resource(leavePath);

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
