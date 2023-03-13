package TaxiPackage.Impl;

import TaxiPackage.ElectionHandle;
import TaxiPackage.Taxi;
import beans.TaxiBean;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import seta.smartcity.rideRequest.RideRequestOuterClass;
import taxi.communication.handleRideService.HandleRideServiceGrpc;
import taxi.communication.handleRideService.HandleRideServiceOuterClass;

import static Utils.Utils.*;

public class HandleRideServiceImpl extends HandleRideServiceGrpc.HandleRideServiceImplBase {

    private ElectionHandle electionHandle;
    private Object inputLock;

    public HandleRideServiceImpl(ElectionHandle electionHandle){
        this.electionHandle = electionHandle;
        this.inputLock = this.electionHandle.getThisTaxi().getInputLock();
    }

    @Override
    public void election(HandleRideServiceOuterClass.ElectionMsg request, StreamObserver<HandleRideServiceOuterClass.ElectionOk> responseObserver) {
        System.out.println("Received"+ printInformation("ELECTION", request.getRequest().getId()));

        HandleRideServiceOuterClass.ElectionMsg myElectionMsg = electionHandle.receiveElectionMsg(request);

        if(myElectionMsg != null){
            forwardMessage(electionHandle.getThisTaxi(), myElectionMsg);
        }else{
            if(request.getCandidateMsg().getId().equals(electionHandle.getThisTaxi().getId())){
                sendElected(electionHandle.getThisTaxi(), request.getRequest());
            }
        }

        responseObserver.onNext(HandleRideServiceOuterClass.ElectionOk.newBuilder().build());
    }

    @Override
    public void elected(HandleRideServiceOuterClass.ElectedMsg request, StreamObserver<HandleRideServiceOuterClass.ElectedOK> responseObserver) {
        System.out.println("Received"+ printInformation("ELECTED", request.getRequest().getId()));

        HandleRideServiceOuterClass.ElectedMsg electedMsg = electionHandle.receiveElectedMsg(request);

        if(electedMsg == null){
            synchronized (electionHandle.getThisTaxi().getElectedLock()){
                if(electionHandle.getElectedSize() == 0){
                    try {
                        electionHandle.getThisTaxi().getElectedLock().wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                synchronized (inputLock){
                        if(electionHandle.getThisTaxi().getInput() == null &&
                        electionHandle.getThisTaxi().getReqToHandle() == null &&
                                electionHandle.getElectedSize() > 0){
                            electionHandle.getThisTaxi().setReqToHandle(electionHandle.getFirst());
                            electionHandle.getThisTaxi().getElectionHandle().addHandled(electionHandle.getThisTaxi().getReqToHandle());
                            electionHandle.getThisTaxi().setInput(Taxi.Input.WORK);
                        }else{
                            for(RideRequestOuterClass.RideRequest rideRequest: electionHandle.getElected()){
                                HandleRideServiceOuterClass.ElectionMsg electionMsg = electionHandle.receiveRideRequest(rideRequest);
                                if(electionMsg != null){
                                    try {
                                        Thread.sleep(2500);
                                        System.out.println("Sent again election for"+printInformation("REQUEST", rideRequest.getId()));
                                        forwardMessage(electionHandle.getThisTaxi(), electionMsg);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                        inputLock.notifyAll();
                    }
                electionHandle.getThisTaxi().getElectedLock().notifyAll();
                }
        }else{
            sendElected(electionHandle.getThisTaxi(), electedMsg);
        }

        responseObserver.onNext(HandleRideServiceOuterClass.ElectedOK.newBuilder().build());
    }

    private static ManagedChannel createChannel(Taxi thisTaxi){

            while(true){
                try{
                    synchronized (thisTaxi.getNextLock()) {
                        TaxiBean t = thisTaxi.getNextTaxi();
                        ManagedChannel channel = ManagedChannelBuilder.forTarget(t.getIp() + ":" + t.getPort()).usePlaintext().build();
                        thisTaxi.getNextLock().notifyAll();
                        return channel;
                    }
                }catch(Exception e){
                    continue;
                }
            }
    }

    private static void sendElected(Taxi thisTaxi, HandleRideServiceOuterClass.ElectedMsg electedMsg){

        ManagedChannel creatingChannel;

        while(true){
            try{
                creatingChannel = createChannel(thisTaxi);
                break;
            }catch(Exception e){
                continue;
            }

        }

        final ManagedChannel channel = creatingChannel;

        HandleRideServiceGrpc.HandleRideServiceStub stub = HandleRideServiceGrpc.newStub(channel);

        stub.elected(electedMsg, new StreamObserver<HandleRideServiceOuterClass.ElectedOK>() {
            @Override
            public void onNext(HandleRideServiceOuterClass.ElectedOK value) {
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

    private static void sendElected(Taxi thisTaxi, HandleRideServiceOuterClass.RideRequest rideRequest){

        HandleRideServiceOuterClass.ElectedMsg myElectedMsg = HandleRideServiceOuterClass.ElectedMsg.newBuilder()
                .setRequest(rideRequest)
                .setTaxiId(thisTaxi.getId())
                .build();

        sendElected(thisTaxi, myElectedMsg);

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
