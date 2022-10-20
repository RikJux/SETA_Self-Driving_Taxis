package TaxiPackage.Threads;

import TaxiPackage.Taxi;
import TaxiPackage.TaxiCommunicationClient;
import TaxiPackage.TaxiRechargeTokenComm;
import TaxiPackage.TokenQueue;
import beans.TaxiBean;
import beans.Taxis;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Stack;

import static Utils.Utils.*;


public class Joining extends TaxiThread {

    static final String serverAddress = "http://localhost:1337";
    private static final String joinPath = serverAddress+"/taxi/join";

    public Joining(Taxi thisTaxi, Taxi.Status thisStatus, Object syncObj) {
        super(thisTaxi, thisStatus, syncObj);
        this.nextStatus.add(Taxi.Status.IDLE);
    }

    @Override
    public void doStuff() throws InterruptedException {
        Client client = Client.create();
        Taxis taxis = joinRequest(client);
        if(taxis == null){
            System.out.println("Cannot enter the system");
            return;
        }

        thisTaxi.setTaxiList(taxis.getTaxiList());
        thisTaxi.setCurrentP(taxis.randomCoord());
        thisTaxi.setDistrict(computeDistrict(thisTaxi.getCurrentP()));
        System.out.println(thisStatus + printInformation("TAXI", thisTaxi.getId()) + "joined in " + thisTaxi.getDistrict());
        synchronized (thisTaxi.getNextLock()){
            thisTaxi.setNextTaxi(extractNextTaxi(thisTaxi));
            thisTaxi.getNextLock().notifyAll();
        }

        TaxiCommunicationClient announceJoinThread = new TaxiCommunicationClient(thisTaxi, true);
        announceJoinThread.start();
        announceJoinThread.join();

        new TaxiRechargeTokenComm(thisTaxi).start();

        /*


        new Thread(() -> {
            synchronized (thisTaxi.getElectedLock()){
                while(thisTaxi.getReqElected().size()  == 0){
                    try {
                        thisTaxi.getElectedLock().wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(thisTaxi.getReqElected().size() > 0){

                    }
                }
            }
        });

         */

        thisTaxi.setTokens(new TokenQueue(taxis.getTokens()));
        System.out.println(thisTaxi.getTokens().toString());

        makeTransition(Taxi.Status.IDLE);

    }

    private static Taxis joinRequest(Client client){
        ClientResponse clientResponse = null;

        TaxiBean t = new TaxiBean(thisTaxi.getId(), thisTaxi.getIp(), thisTaxi.getPort());
        WebResource webResource = client.resource(joinPath);
        String input = new Gson().toJson(t);

        try {
            clientResponse = webResource.type("application/json").post(ClientResponse.class, input);
        } catch (ClientHandlerException e) {
            System.out.println("Join impossible: taxi" + thisTaxi.getId() + "can't reach the server");
            return null;
        }

        if(clientResponse.getStatus() == Response.Status.NOT_ACCEPTABLE.getStatusCode()){
            System.out.println("Join impossible: duplicated id " + thisTaxi.getId());
            return null; // duplicated id
        }
        return new Gson().fromJson(clientResponse.getEntity(String.class), Taxis.class);
    }

}
