package TaxiPackage;

import TaxiPackage.Impl.HandleRideServiceImpl;
import TaxiPackage.Impl.JoinServiceImpl;
import TaxiPackage.Impl.LeaveServiceImpl;
import TaxiPackage.Impl.RechargeServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class TaxiCommunicationServer extends Thread{

    private final Taxi taxi;

    public TaxiCommunicationServer(Taxi taxi){
        this.taxi = taxi;
    }

    public void run()
    {

        try {

            Server server = ServerBuilder.forPort(taxi.getPort())
                    .addService(new JoinServiceImpl())
                    .addService(new LeaveServiceImpl())
                    .addService(new HandleRideServiceImpl())
                    .addService(new RechargeServiceImpl())
                    .build();

            server.start();

            System.out.println("Communication thread for taxi " + taxi.getId() + " started");

            server.awaitTermination();

        } catch (IOException e) {

            e.printStackTrace();

        } catch (InterruptedException e) {

            e.printStackTrace();

        }


    }


}
