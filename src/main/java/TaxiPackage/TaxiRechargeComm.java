package TaxiPackage;

import beans.TaxiBean;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import taxi.communication.rechargeService.RechargeServiceGrpc;
import taxi.communication.rechargeService.RechargeServiceOuterClass;
import Utils.Utils;

public class TaxiRechargeComm extends Thread{

    private final Taxi thisTaxi;

    public TaxiRechargeComm(Taxi thisTaxi){
        this.thisTaxi = thisTaxi;
    }

    @Override
    public void run() {

        for(TaxiBean t: thisTaxi.getTaxiList()){
            requireRecharge(thisTaxi, t);
        }
        //TODO
        synchronized (thisTaxi){
            while(thisTaxi.getRechargeReqCounter() < thisTaxi.getTaxiList().size()){
                try {
                    thisTaxi.wait();
                    if(thisTaxi.getRechargeReqCounter() == thisTaxi.getTaxiList().size()) {
                        System.out.println("[RECHARGE COMM] All other taxis tell you can go recharge!");
                        // thisTaxi.setCurrentStatus(Taxi.Status.GO_RECHARGE);
                        thisTaxi.zeroRechargeReqCounter();
                        thisTaxi.notifyAll();
                        return;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static void requireRecharge(Taxi thisTaxi, TaxiBean t){
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(t.getIp()+":"+t.getPort()).usePlaintext().build();

        int[] destinationStation = Utils.computeRechargeStation(thisTaxi.getDistrict());

        RechargeServiceGrpc.RechargeServiceStub stub = RechargeServiceGrpc.newStub(channel);

        RechargeServiceOuterClass.RechargeRequest rechargeReq = RechargeServiceOuterClass.RechargeRequest.newBuilder()
                .setId(thisTaxi.getId())
                .setTimestamp(thisTaxi.getRechargeRequestTimestamp())
                .setRechargePosition(RechargeServiceOuterClass.RechargeRequest.RechargePosition.newBuilder()
                        .setX(Utils.getCoordX(destinationStation))
                        .setY(Utils.getCoordY(destinationStation))
                        .build())
                .build();

        stub.recharge(rechargeReq, new StreamObserver<RechargeServiceOuterClass.RechargeOk>() {
            @Override
            public void onNext(RechargeServiceOuterClass.RechargeOk value) {
                synchronized (thisTaxi){
                    System.out.println("[RECHARGE COMM] Taxi " + t.getId() + " gave permission to recharge.");
                    thisTaxi.incrementRechargeReqCounter();
                    System.out.println("[RECHARGE COMM] Counter: " + thisTaxi.getRechargeReqCounter());
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
