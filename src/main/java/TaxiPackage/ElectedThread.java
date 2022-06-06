package TaxiPackage;

import beans.TaxiBean;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import seta.smartcity.rideRequest.RideRequestOuterClass;
import taxi.communication.handleRideService.HandleRideServiceGrpc;
import taxi.communication.handleRideService.HandleRideServiceOuterClass;

public class ElectedThread extends Thread{

    private Taxi thisTaxi;

    public ElectedThread(Taxi thisTaxi){
        this.thisTaxi = thisTaxi;
    }

    @Override
    public void run() {

        synchronized (thisTaxi.getInputLock()) {
            while (thisTaxi.getInput() != null) {
                try {
                    thisTaxi.getInputLock().wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (thisTaxi.getInput() == null) {
                    thisTaxi.setInput(Taxi.Input.WORK);
                    thisTaxi.setReqToHandle(thisTaxi.getElectionData().getRequestToHandle());
                    System.out.println("To handle " + thisTaxi.getReqToHandle());
                    thisTaxi.getInputLock().notifyAll();
                }
            }
        }
    }


    private static ManagedChannel createChannel(Taxi thisTaxi){

        synchronized (thisTaxi.getNextLock()) {
            TaxiBean t = thisTaxi.getNextTaxi();
            ManagedChannel channel = ManagedChannelBuilder.forTarget(t.getIp() + ":" + t.getPort()).usePlaintext().build();
            thisTaxi.getNextLock().notifyAll();
            return channel;
        }

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
