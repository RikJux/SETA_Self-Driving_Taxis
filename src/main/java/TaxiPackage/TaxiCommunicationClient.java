package TaxiPackage;

import beans.TaxiBean;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import taxi.communication.joinService.JoinServiceGrpc;
import taxi.communication.joinService.JoinServiceGrpc.*;
import taxi.communication.joinService.JoinServiceOuterClass;
import taxi.communication.leaveService.LeaveServiceGrpc;
import taxi.communication.leaveService.LeaveServiceOuterClass;

import static Utils.Utils.printInformation;

public class TaxiCommunicationClient extends Thread{

    private final Taxi thisTaxi;
    private final boolean join;

    public TaxiCommunicationClient(Taxi thisTaxi, boolean join){
        this.thisTaxi = thisTaxi;
        this.join = join;
    }

    public void run(){

        if(join){
            for(TaxiBean t: thisTaxi.getTaxiList()){
                if(!thisTaxi.getId().equals(t.getId())){
                    announceJoinAsync(thisTaxi, t);
                }
            }
        }else{
            for(TaxiBean t: thisTaxi.getTaxiList()){
                if(!thisTaxi.getId().equals(t.getId())){
                    announceLeaveAsync(thisTaxi, t);
                }
            }
        }

    }

    public static void announceJoinAsync(Taxi thisTaxi, TaxiBean t){
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(t.getIp()+":"+t.getPort()).usePlaintext().build();

        JoinServiceStub stub = JoinServiceGrpc.newStub(channel);

        JoinServiceOuterClass.JoinMsg joinMsg = JoinServiceOuterClass.JoinMsg.newBuilder()
                .setId(thisTaxi.getId())
                .setIp(thisTaxi.getIp())
                .setPort(thisTaxi.getPort())
                .setPosition(JoinServiceOuterClass.JoinMsg.Position.newBuilder().setX(thisTaxi.getX()).setY(thisTaxi.getY()).build())
                .build();

        stub.join(joinMsg, new StreamObserver<JoinServiceOuterClass.JoinOk>() {
            @Override
            public void onNext(JoinServiceOuterClass.JoinOk value) {
                System.out.println(printInformation("TAXI", t.getId()) + "is now aware of my presence.");
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                channel.shutdownNow();
            }
        });

    }

    public static void announceLeaveAsync(Taxi thisTaxi, TaxiBean t){
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(t.getIp()+":"+t.getPort()).usePlaintext().build();

        LeaveServiceGrpc.LeaveServiceStub stub = LeaveServiceGrpc.newStub(channel);

        LeaveServiceOuterClass.LeaveMsg leaveMsg = LeaveServiceOuterClass.LeaveMsg.newBuilder()
                .setId(thisTaxi.getId())
                .build();

        stub.leave(leaveMsg, new StreamObserver<LeaveServiceOuterClass.LeaveOk>() {
            @Override
            public void onNext(LeaveServiceOuterClass.LeaveOk value) {
                System.out.println(printInformation("TAXI", t.getId()) + "is now aware of my departure.");
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                channel.shutdownNow();
            }
        });
    }

}
