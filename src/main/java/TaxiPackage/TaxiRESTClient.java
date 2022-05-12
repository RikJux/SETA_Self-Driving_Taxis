package TaxiPackage;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import beans.*;

public class TaxiRESTClient {

    public static void main(String[] args){

        Client client = Client.create();
        String serverAddress = "http://localhost:1337";
        ClientResponse clientResponse = null;

        String postPath = "/taxi/join";
        TaxiBean t = new TaxiBean("2", "localhost", 1235);
        WebResource webResource = client.resource(serverAddress+postPath);
        String input = new Gson().toJson(t);
        clientResponse = webResource.type("application/json").post(ClientResponse.class, input);
        System.out.println(clientResponse.toString());

        TaxiBean t2 = new TaxiBean("0", "localhost", 1233);
        webResource = client.resource(serverAddress+postPath);
        input = new Gson().toJson(t2);
        clientResponse = webResource.type("application/json").post(ClientResponse.class, input);

        System.out.println(clientResponse.toString());

        String getPath = "/taxi/taxiList";
        webResource = client.resource(serverAddress+getPath);
        clientResponse = webResource.type("application/json").get(ClientResponse.class);
        System.out.println(clientResponse.toString());
        Taxis taxis = clientResponse.getEntity(beans.Taxis.class);
        for(TaxiBean ta: taxis.getTaxiList()){
            System.out.println(ta.getId() +  " " + ta.getIp() + " " + ta.getPort());
        }

        String removePath = "/taxi/leaveRequest/0";
        webResource = client.resource(serverAddress+getPath);
        clientResponse = webResource.type("application/json").delete(ClientResponse.class);
        System.out.println(clientResponse.toString());
        // taxis = clientResponse.getEntity(beans.Taxis.class);
        for(TaxiBean ta: taxis.getTaxiList()){
            System.out.println(ta.getId() +  " " + ta.getIp() + " " + ta.getPort());
        }

    }

}
