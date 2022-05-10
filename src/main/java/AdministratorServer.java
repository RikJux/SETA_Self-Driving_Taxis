import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import seta.smartcity.requestToJoin.RequestToJoinOuterClass;
import seta.smartcity.requestToJoinAccept.RequestToJoinAcceptOuterClass.RequestToJoinAccept.Taxi;

public class AdministratorServer {

    private ArrayList<Taxi> taxis = new ArrayList<Taxi>();

    public synchronized ArrayList<Taxi> getTaxis() {
        return taxis;
    }

    public void addTaxi(Taxi t){
        taxis.add(t);
    }

    // recieve requests (through socket?)
    // check identifier is no already in use
    // assign random district
    // maybe a dispatcher
    public static void main(String[] args) throws Exception{
        AdministratorServer admin = new AdministratorServer();
        ServerSocket adminAcceptTaxi = new ServerSocket(9999);
        System.out.println("Administrator Server is now accepting join requests");
        System.out.println("at port " + adminAcceptTaxi.getLocalPort());

        while(true){ // change this!
            Socket incomingRequest = adminAcceptTaxi.accept();
            AdminTaxiThread t = new AdminTaxiThread(admin, incomingRequest);
            t.start();
        }
    }
}
