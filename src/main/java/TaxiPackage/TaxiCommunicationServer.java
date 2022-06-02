package TaxiPackage;

import TaxiPackage.Impl.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

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
                    .addService(new HandleRideServiceImpl(thisTaxi))
                    .addService(new RechargeTokenServiceImpl(thisTaxi))
                    .build();

            server.start();

            System.out.println("Communication thread for taxi " + thisTaxi.getId() + " started");

            server.awaitTermination();

        } catch (IOException e) {

            e.printStackTrace();

        } catch (InterruptedException e) {

            e.printStackTrace();

        }


    }


}
