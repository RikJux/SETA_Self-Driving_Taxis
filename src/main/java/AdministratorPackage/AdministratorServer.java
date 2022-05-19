package AdministratorPackage;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;

public class AdministratorServer {

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
