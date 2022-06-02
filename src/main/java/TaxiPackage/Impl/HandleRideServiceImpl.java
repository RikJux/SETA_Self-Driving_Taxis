package TaxiPackage.Impl;

import TaxiPackage.ElectionData;
import TaxiPackage.Taxi;
import beans.TaxiBean;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import taxi.communication.handleRideService.HandleRideServiceGrpc;
import taxi.communication.handleRideService.HandleRideServiceOuterClass;
import taxi.communication.rechargeTokenService.RechargeTokenServiceGrpc;
import taxi.communication.rechargeTokenService.RechargeTokenServiceOuterClass;

import static Utils.Utils.printInformation;

public class HandleRideServiceImpl extends HandleRideServiceGrpc.HandleRideServiceImplBase {

    private ElectionData electionData;

    public HandleRideServiceImpl(ElectionData electionData){
        this.electionData = electionData;
    }

    @Override
    public void election(HandleRideServiceOuterClass.ElectionMsg request, StreamObserver<HandleRideServiceOuterClass.ElectionOk> responseObserver) {
        System.out.println("Received"+ printInformation("ELECTION", request.getRequest().getId()));
        HandleRideServiceOuterClass.ElectionMsg myElectionMsg = electionData.computeElectionMsg(request);
        if(myElectionMsg != null){
            forwardMessage(electionData.getThisTaxi(), myElectionMsg);
        }else{
            sendElected(electionData.getThisTaxi(), request.getRequest().getId());
        }
        responseObserver.onNext(HandleRideServiceOuterClass.ElectionOk.newBuilder().build());
    }

    @Override
    public void elected(HandleRideServiceOuterClass.ElectedMsg request, StreamObserver<HandleRideServiceOuterClass.ElectedOK> responseObserver) {
        System.out.println("Received"+ printInformation("ELECTED", request.getRideRequestId()));
        electionData.markNonParticipant(request);
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

    private static void sendElected(Taxi thisTaxi, String requestId){
        final ManagedChannel channel = createChannel(thisTaxi);

        HandleRideServiceGrpc.HandleRideServiceStub stub = HandleRideServiceGrpc.newStub(channel);

        HandleRideServiceOuterClass.ElectedMsg myElectedMsg = HandleRideServiceOuterClass.ElectedMsg.newBuilder()
                .setRideRequestId(requestId)
                .setTaxiId(thisTaxi.getId())
                .build();

        stub.elected(myElectedMsg, new StreamObserver<HandleRideServiceOuterClass.ElectedOK>() {
            @Override
            public void onNext(HandleRideServiceOuterClass.ElectedOK value) {
                System.out.println("Sent"+ printInformation("ELECTED", myElectedMsg.getRideRequestId()));
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
                    System.out.println("Sent"+ printInformation("ELECTION", electionMsg.getRequest().getId()));
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
