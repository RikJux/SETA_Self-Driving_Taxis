package TaxiPackage;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import beans.*;

import java.util.ArrayList;
import java.util.List;

public class TaxiRESTClient {

    private static final String serverAddress = "http://localhost:1337";
    private static final String joinPath = serverAddress+"/taxi/join";
    private static final String removePath = serverAddress+"/taxi/leave/"; // + id

    public static void main(String[] args){

        Client client = Client.create();
        ClientResponse clientResponse = null;

        String postPath = "/taxi/join";
        TaxiBean t = new TaxiBean("2", "localhost", 1235);
        WebResource webResource = client.resource(joinPath);
        String input = new Gson().toJson(t);
        clientResponse = webResource.type("application/json").post(ClientResponse.class, input);
        System.out.println(clientResponse.toString());
        Taxis taxis = clientResponse.getEntity(Taxis.class);
        List<TaxiBean> taxiList= taxis.getTaxiList();
        int coord[] = taxis.randomCoord();

        for(TaxiBean ta: taxiList) {
            System.out.println(ta.getId() + " " + ta.getIp() + " " + ta.getPort());
        }
        for(int i: coord){
            System.out.println(i);
        }


        // List<TaxiBean> l = clientResponse.readEntity()

        /*
        TaxiBean t2 = new TaxiBean("0", "localhost", 1233);
        webResource = client.resource(serverAddress+postPath);
        input = new Gson().toJson(t2);
        clientResponse = webResource.type("application/json").post(ClientResponse.class, input);

        System.out.println(clientResponse.toString());

        String getPath = "/taxi/taxiList";
        webResource = client.resource(serverAddress+getPath);
        clientResponse = webResource.type("application/json").get(ClientResponse.class);
        System.out.println(clientResponse.toString());
        Taxis taxis = clientResponse.getEntity(Taxis.class);
        for(TaxiBean ta: taxis.getTaxiList()){
            System.out.println(ta.getId() +  " " + ta.getIp() + " " + ta.getPort());
        }

        String removePath = "/taxi/leave/";
        webResource = client.resource(serverAddress+removePath);
        clientResponse = webResource.type("application/json").delete(ClientResponse.class);
        System.out.println(clientResponse.toString());
        taxis = clientResponse.getEntity(Taxis.class);
        for(TaxiBean ta: taxis.getTaxiList()){
            System.out.println(ta.getId() +  " " + ta.getIp() + " " + ta.getPort());
        }
        */
    }

    public static void joinRequest(Client client, String url, Taxi t){

    }

}
