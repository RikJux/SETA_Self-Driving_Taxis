package TaxiPackage;
import Simulator.Buffer;
import Simulator.Measurement;
import Simulator.PM10Simulator;
import Simulator.SimulatorData;
import beans.TaxiStatistics;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import java.util.List;

public class Sensor extends Thread{

    private final Taxi thisTaxi;
    private final PM10Simulator pm10;
    private final Client client;

    public Sensor(Taxi taxi, PM10Simulator pm10, Client client){
        this.thisTaxi = taxi;
        this.pm10 = pm10;
        this.client = client;
    }

    public void run() {

        while(true){
            try {
                Thread.sleep(15000);
                handleStatistics(client, thisTaxi, pm10.getBuffer());
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private static Taxi handleStatistics(Client client, Taxi thisTaxi, Buffer pm10Buffer){
        List<Measurement> pollution = pm10Buffer.readAllAndClean();
        thisTaxi.getTaxiStats().setPollution(avgPollution(pollution));
        thisTaxi.getTaxiStats().setTimestamp(System.currentTimeMillis());
        sendStatistics(client, thisTaxi.getTaxiStats());
        double battLeft = thisTaxi.getTaxiStats().getBatteryLevel();
        thisTaxi.setTaxiStats(new TaxiStatistics(thisTaxi.getId()));
        thisTaxi.setBattery(battLeft);

        return thisTaxi;
    }

    private static void sendStatistics(Client client, TaxiStatistics taxiStats){
        ClientResponse clientResponse = null;

        WebResource webResource = client.resource(Taxi.serverAddress+"/statistics/post/"+taxiStats.getId());
        String input = new Gson().toJson(taxiStats);
        System.out.println(input);

        try {
            clientResponse = webResource.type("application/json").post(ClientResponse.class, input);
        } catch (ClientHandlerException e) {
            System.out.println("Error");
            return;
        }

    }

    private static double avgPollution(List<Measurement> pollution){

        double sum = 0;
        for(Measurement m: pollution){
            sum += m.getValue();
        }

        return sum/pollution.size();
    }
}
