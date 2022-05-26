package TaxiPackage.Threads;

import TaxiPackage.Taxi;
import TaxiPackage.TaxiCommunicationClient;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import javax.ws.rs.core.Response;
import java.util.List;

public class Leaving extends TaxiThread{

    static final String serverAddress = "http://localhost:1337";
    private static final String leavePath = serverAddress+"/taxi/leave/";

    public Leaving(Taxi thisTaxi, Taxi.Status thisStatus, Object syncObj) {
        super(thisTaxi, thisStatus, syncObj);
    }

    @Override
    public void doStuff() throws InterruptedException {
        Client client = Client.create();
        leaveRequest(client);
        TaxiCommunicationClient announceJoinThread = new TaxiCommunicationClient(thisTaxi, false);
        announceJoinThread.start();
        announceJoinThread.join();
        for(Thread t: thisTaxi.getTaxiThreads()){
            t.interrupt();
        }

    }

    private static void leaveRequest(Client client){
        ClientResponse clientResponse = null;

        WebResource webResource = client.resource(leavePath+thisTaxi.getId());

        try {
            clientResponse = webResource.type("application/json").delete(ClientResponse.class);
        } catch (ClientHandlerException e) {
            System.out.println("[TAXI MAIN] Impossible to leave: taxi " + thisTaxi.getId() + " can't reach the server");
            return;
        }

        if(clientResponse.getStatus() == Response.Status.NOT_FOUND.getStatusCode()){
            System.out.println("[TAXI MAIN] Impossible to leave: can't find id " + thisTaxi.getId());
            return;
        }else{
            System.out.println("[TAXI MAIN] Taxi " + thisTaxi.getId() + " left the system");
        }
    }

}
