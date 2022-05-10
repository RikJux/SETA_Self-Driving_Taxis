package AdministratorPackage;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;
import java.io.IOException;
import java.net.Socket;

import seta.smartcity.requestToJoin.RequestToJoinOuterClass.RequestToJoin;
import seta.smartcity.requestToJoinAccept.RequestToJoinAcceptOuterClass.RequestToJoinAccept;

public class AdminTaxiThread extends Thread{

    private InputStream inFromClient;
    private OutputStream outToClient;
    private AdministratorServer admin;
    private Socket s;

    public AdminTaxiThread(AdministratorServer admin, Socket s){
        this.admin = admin;
        this.s = s;

        try {
            inFromClient = s.getInputStream();
            outToClient = s.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void run() {
        System.out.println("Thread started!");
        try {
            RequestToJoin receivedJoinRequest = RequestToJoin.parseFrom(inFromClient);
            System.out.println("Join request received");

            boolean accepted = true;

            for(RequestToJoinAccept.Taxi t: admin.getTaxis()){
                if(t.getId().equals(receivedJoinRequest.getId())){
                    accepted = false;
                    break;
                }
            }

            if(accepted){
                RequestToJoinAccept.Taxi thisTaxi = RequestToJoinAccept.Taxi.newBuilder()
                        .setId(receivedJoinRequest.getId())
                        .setIp(receivedJoinRequest.getIp())
                        .setPort(receivedJoinRequest.getPort())
                        .build();

                Random rand = new Random();

                RequestToJoinAccept.Position startingP = RequestToJoinAccept.Position.newBuilder()
                        .setX(genCoord(rand))
                        .setY(genCoord(rand))
                        .build();

                RequestToJoinAccept acceptJoinRequest = RequestToJoinAccept.newBuilder()
                        .setStartingP(startingP)
                        .addAllTaxi(admin.getTaxis())
                        .build();

                admin.addTaxi(thisTaxi);
                // acceptJoinRequest.writeTo(outToClient);
            }else{
                outToClient.write(-1); // should be a message
            }

            System.out.println(accepted);
            System.out.println(admin.getTaxis());

            s.close();
            System.out.println("Thread closed");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int genCoord(Random r){

        int c = 0;

        if(r.nextBoolean()){
            c = 9;
        }

        return c;
    }

}
