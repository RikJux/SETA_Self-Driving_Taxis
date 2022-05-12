package AdministratorPackage;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;
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

    private static final String HOST = "localhost";
    private static final int PORT = 1337;

    // recieve requests (through socket?)
    // check identifier is no already in use
    // assign random district
    // maybe a dispatcher
    public static void main(String[] args) throws Exception{

        HttpServer server = HttpServerFactory.create("http://"+HOST+":"+PORT+"/");
        server.start();

        System.out.println("Server started on: http://"+HOST+":"+PORT);

    }
}
