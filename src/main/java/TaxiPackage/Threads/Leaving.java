package TaxiPackage.Threads;

import TaxiPackage.Taxi;
import TaxiPackage.TaxiCommunicationClient;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import javax.ws.rs.core.Response;
import java.util.List;

import static Utils.Utils.printInformation;

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
        TaxiCommunicationClient announceLeaveThread = new TaxiCommunicationClient(thisTaxi, false);
        announceLeaveThread.start();
        announceLeaveThread.join();
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
            System.out.println("Impossible to leave:" + printInformation("TAXI", thisTaxi.getId()) + "can't reach the server");
            return;
        }

        if(clientResponse.getStatus() == Response.Status.NOT_FOUND.getStatusCode()){
            System.out.println("Impossible to leave: can't find id" + printInformation("TAXI", thisTaxi.getId()) );
            return;
        }else{
            System.out.println(printInformation("TAXI", thisTaxi.getId()) + " left the system");
        }
    }

}
