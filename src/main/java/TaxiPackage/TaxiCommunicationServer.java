package TaxiPackage;

import TaxiPackage.Impl.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

import static Utils.Utils.printInformation;

public class TaxiCommunicationServer extends Thread{

    private final Taxi thisTaxi;

    public TaxiCommunicationServer(Taxi thisTaxi){
        this.thisTaxi = thisTaxi;
    }

    public void run()
    {

        try {

            Server server = ServerBuilder.forPort(thisTaxi.getPort())
                    .addService(new JoinServiceImpl(thisTaxi))
                    .addService(new LeaveServiceImpl(thisTaxi))
                    .addService(new RechargeTokenServiceImpl(thisTaxi))
                    .addService(new HandleRideServiceImpl(thisTaxi.getElectionHandle()))
                    .build();

            server.start();
            thisTaxi.setServer(server);

            System.out.println("Communication thread for" + printInformation("TAXI", thisTaxi.getId()) + "started");

            thisTaxi.getServer().awaitTermination();

        } catch (IOException e) {

            e.printStackTrace();

        } catch (InterruptedException e) {

            e.printStackTrace();

        }


    }


}
