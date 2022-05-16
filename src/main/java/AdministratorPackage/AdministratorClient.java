package AdministratorPackage;

import beans.Hello;
import beans.TaxiStatistics;
import com.google.gson.Gson;
import com.sun.jersey.api.client.*;

import javax.ws.rs.core.MediaType;

public class AdministratorClient {

    private static String serverAddress = "http://localhost:1337";
    private static String statsNPath = "/statistics/get/%s/%d";
    private static String statsWindowPath = "/statistics/window/%f/%f";
    private static Client client;

    public static void main(String args[]){
        client = Client.create();
    }

    public static TaxiStatistics statsOfTaxi(String id, int n){
        String url = String.format(serverAddress+statsNPath, id, n);
        return getTaxiStats(client, url);
    }

    public static TaxiStatistics statsInTempWindow(double from, double to){
        String url = String.format(serverAddress+statsWindowPath, from, to);
        return getTaxiStats(client, url);
    }

    public static TaxiStatistics getTaxiStats(Client client, String url){
        ClientResponse clientResponse = getRequest(client, url);
        return new Gson().fromJson(clientResponse.getEntity(String.class), TaxiStatistics.class);
    }

    public static ClientResponse getRequest(Client client, String url){
        WebResource webResource = client.resource(url);
        try {
            return webResource.type(MediaType.TEXT_PLAIN).get(ClientResponse.class);
        } catch (ClientHandlerException e) {
            System.out.println("Error in getting statistics");
            return null;
        }
    }

}
