package TaxiPackage;

import beans.TaxiBean;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import taxi.communication.handleRideService.HandleRideServiceGrpc;
import taxi.communication.handleRideService.HandleRideServiceOuterClass;

import java.util.PriorityQueue;

public class TaxiReqComm extends Thread{

    private final Taxi thisTaxi;
    private final ElectionIdentifier elId;
    private final String requestId;
    private static int voteCount = 0;

    public TaxiReqComm(Taxi thisTaxi, ElectionIdentifier elId, String requestId){
        this.thisTaxi = thisTaxi;
        this.elId = elId;
        this.requestId = requestId;
    }

    @Override
    public void run() {

        for(TaxiBean t: thisTaxi.getTaxiList()){
            initiateElection(elId, t, requestId, thisTaxi);
        }

        synchronized (thisTaxi){
            while(voteCount < thisTaxi.getTaxiList().size() && thisTaxi.getCurrentStatus() == Taxi.Status.IDLE){
                try {
                    System.out.println("waiting for votes");
                    thisTaxi.wait();
                    if(voteCount == thisTaxi.getTaxiList().size() && thisTaxi.getCurrentStatus() != Taxi.Status.IDLE){ // change here
                        thisTaxi.setCurrentStatus(Taxi.Status.WORKING); // not yet elected, TODO change here
                        System.out.println("[REQUEST COMM] Election for request " + requestId + " won!");
                        thisTaxi.notifyAll();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static void initiateElection(ElectionIdentifier elId, TaxiBean t, String requestId, Taxi thisTaxi){

        final ManagedChannel channel = ManagedChannelBuilder.forTarget(t.getIp()+":"+t.getPort()).usePlaintext().build();

        HandleRideServiceGrpc.HandleRideServiceStub stub = HandleRideServiceGrpc.newStub(channel);

        HandleRideServiceOuterClass.ElectionMsg electionReq = HandleRideServiceOuterClass.ElectionMsg.newBuilder()
                .setRideRequest(requestId)
                .setCandidateMsg(elId.toMsg())
                .build();

        stub.election(electionReq, new StreamObserver<HandleRideServiceOuterClass.HandleRideOk>() {
            @Override
            public void onNext(HandleRideServiceOuterClass.HandleRideOk value) {
                // found a better taxi to handle the request, or self
                synchronized (thisTaxi){
                    System.out.println("[REQUEST COMM] " + t.getId() + " is a candidate for request " + requestId);
                    voteCount++;
                    thisTaxi.notifyAll();
                }
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
