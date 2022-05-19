package TaxiPackage;

import beans.TaxiBean;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import taxi.communication.joinService.JoinServiceGrpc;
import taxi.communication.joinService.JoinServiceGrpc.*;
import taxi.communication.joinService.JoinServiceOuterClass;
import taxi.communication.leaveService.LeaveServiceGrpc;
import taxi.communication.leaveService.LeaveServiceOuterClass;

public class TaxiCommunicationClient extends Thread{

    private final Taxi taxi;

    public TaxiCommunicationClient(Taxi taxi){
        this.taxi = taxi;
    }

    public void run(){

        for(TaxiBean t: taxi.getTaxiList()){ // announce joining
            System.out.println(t.toString());
            announceJoin(taxi.getId(), taxi.getIp(), taxi.getPort(), t);
        }

    }

    public static void announceJoin(String myId, String myIp, int myPort, TaxiBean t){
        // synchronously contact all the other taxis
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(t.getIp()+":"+t.getPort()).usePlaintext().build();

        JoinServiceBlockingStub stub = JoinServiceGrpc.newBlockingStub(channel);

        JoinServiceOuterClass.JoinMsg joinMsg = JoinServiceOuterClass.JoinMsg.newBuilder()
                .setId(myId)
                .setIp(myIp)
                .setPort(myPort) // my port
                .build();

        JoinServiceOuterClass.JoinOk response = stub.join(joinMsg);
        System.out.println("Taxi " + t.getId() + " is now aware of my presence.");
    }

    public static void announceLeaving(String myId, String myIp, int myPort, TaxiBean t){
        // synchronously contact all the other taxis
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(t.getIp()+":"+t.getPort()).usePlaintext().build();

        LeaveServiceGrpc.LeaveServiceBlockingStub stub = LeaveServiceGrpc.newBlockingStub(channel);

        LeaveServiceOuterClass.LeaveMsg leaveMsg = LeaveServiceOuterClass.LeaveMsg.newBuilder()
                .setId(myId)
                .build();

        LeaveServiceOuterClass.LeaveOk response = stub.leave(leaveMsg);
        System.out.println("Taxi " + t.getId() + " is now aware of my departure.");
    }


}
