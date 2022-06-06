package TaxiPackage.Impl;

import TaxiPackage.ElectionData;
import TaxiPackage.ElectionDataStructure;
import TaxiPackage.Taxi;
import beans.TaxiBean;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import seta.smartcity.rideRequest.RideRequestOuterClass;
import taxi.communication.handleRideService.HandleRideServiceGrpc;
import taxi.communication.handleRideService.HandleRideServiceOuterClass;
import taxi.communication.rechargeTokenService.RechargeTokenServiceGrpc;
import taxi.communication.rechargeTokenService.RechargeTokenServiceOuterClass;

import java.util.Random;

import static Utils.Utils.*;

public class HandleRideServiceImpl extends HandleRideServiceGrpc.HandleRideServiceImplBase {

    private ElectionDataStructure electionData;
    private Object inputLock;

    public HandleRideServiceImpl(ElectionDataStructure electionData){
        this.electionData = electionData;
        this.inputLock = this.electionData.getThisTaxi().getInputLock();
    }

    @Override
    public void election(HandleRideServiceOuterClass.ElectionMsg request, StreamObserver<HandleRideServiceOuterClass.ElectionOk> responseObserver) {
        System.out.println("Received"+ printInformation("ELECTION", request.getRequest().getId()));
        switch (electionData.decideWhatToSend(request)){
            case NOTHING:
                break;
            case ELECTED:
                sendElected(electionData.getThisTaxi(), request.getRequest());
                break;
            case SELF_ELECTION:
                HandleRideServiceOuterClass.ElectionMsg myElectionMsg = electionData.computeElectionMsg(request.getRequest().getId());
                forwardMessage(electionData.getThisTaxi(), myElectionMsg);
                break;
            case OTHER_ELECTION:
                forwardMessage(electionData.getThisTaxi(), request);
                break;
        }
        responseObserver.onNext(HandleRideServiceOuterClass.ElectionOk.newBuilder().build());
    }

    @Override
    public void elected(HandleRideServiceOuterClass.ElectedMsg request, StreamObserver<HandleRideServiceOuterClass.ElectedOK> responseObserver) {
        System.out.println("Received"+ printInformation("ELECTED", request.getRequest().getId()));
        if (request.getTaxiId().equals(electionData.getThisTaxi().getId())){
            electionData.putInElected(request.getRequest().getId());
            synchronized (electionData.getThisTaxi().getInputLock()){
                electionData.getThisTaxi().getInputLock().notifyAll();
            }
        }else{
            electionData.markNonParticipant(request.getRequest().getId());
        }
        responseObserver.onNext(HandleRideServiceOuterClass.ElectedOK.newBuilder().build());
    }

    private static ManagedChannel createChannel(Taxi thisTaxi){

        synchronized (thisTaxi.getNextLock()) {
            TaxiBean t = thisTaxi.getNextTaxi();
            ManagedChannel channel = ManagedChannelBuilder.forTarget(t.getIp() + ":" + t.getPort()).usePlaintext().build();
            thisTaxi.getNextLock().notifyAll();
            return channel;
        }

    }

    private static void sendElected(Taxi thisTaxi, HandleRideServiceOuterClass.RideRequest rideRequest){
        final ManagedChannel channel = createChannel(thisTaxi);

        HandleRideServiceGrpc.HandleRideServiceStub stub = HandleRideServiceGrpc.newStub(channel);

        HandleRideServiceOuterClass.ElectedMsg myElectedMsg = HandleRideServiceOuterClass.ElectedMsg.newBuilder()
                .setRequest(rideRequest)
                .setTaxiId(thisTaxi.getId())
                .build();

        stub.elected(myElectedMsg, new StreamObserver<HandleRideServiceOuterClass.ElectedOK>() {
            @Override
            public void onNext(HandleRideServiceOuterClass.ElectedOK value) {
                System.out.println("Sent"+ printInformation("ELECTED", myElectedMsg.getRequest().getId()));
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


    private static void forwardMessage(Taxi thisTaxi, HandleRideServiceOuterClass.ElectionMsg electionMsg){

        final ManagedChannel channel = createChannel(thisTaxi);

        HandleRideServiceGrpc.HandleRideServiceStub stub = HandleRideServiceGrpc.newStub(channel);

        stub.election(electionMsg, new StreamObserver<HandleRideServiceOuterClass.ElectionOk>() {
                @Override
                public void onNext(HandleRideServiceOuterClass.ElectionOk value) {
                    //System.out.println("Sent"+ printInformation("ELECTION", electionMsg.getRequest().getId()));
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
