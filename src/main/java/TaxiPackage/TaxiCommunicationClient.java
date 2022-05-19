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
    private final boolean join;
    private final TaxiBean otherTaxiBean;

    public TaxiCommunicationClient(Taxi taxi, boolean join, TaxiBean otherTaxiBean){
        this.taxi = taxi;
        this.join = join;
        this.otherTaxiBean = otherTaxiBean;
    }

    public void run(){

        if(join){
            announceJoin(taxi.getId(), taxi.getIp(), taxi.getPort(), otherTaxiBean);
        }else{
            announceLeave(taxi.getId(), taxi.getIp(), taxi.getPort(), otherTaxiBean);
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

    public static void announceLeave(String myId, String myIp, int myPort, TaxiBean t){
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
