package TaxiPackage;

import beans.TaxiBean;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import taxi.communication.rechargeService.RechargeServiceGrpc;
import taxi.communication.rechargeService.RechargeServiceOuterClass;

public class TaxiRechargeComm extends Thread{

    private final Taxi taxi;

    public TaxiRechargeComm(Taxi taxi){
        this.taxi = taxi;
    }

    @Override
    public void run() {

        for(TaxiBean t: taxi.getTaxiList()){
            requireRecharge(taxi, t);
        }
        //TODO
        synchronized (taxi){
            while(taxi.getRechargeReqCounter() < taxi.getTaxiList().size()){
                try {
                    taxi.wait();
                    if(taxi.getRechargeReqCounter() == taxi.getTaxiList().size()) {
                        System.out.println("All other taxis tell you can go recharge!");
                        taxi.setCurrentStatus(Taxi.Status.GO_RECHARGE);
                        taxi.notifyAll();
                        return;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static void requireRecharge(Taxi taxi, TaxiBean t){
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(t.getIp()+":"+t.getPort()).usePlaintext().build();

        RechargeServiceGrpc.RechargeServiceStub stub = RechargeServiceGrpc.newStub(channel);

        RechargeServiceOuterClass.RechargeRequest rechargeReq = RechargeServiceOuterClass.RechargeRequest.newBuilder()
                .setId(taxi.getId())
                .setTimestamp(taxi.getRechargeRequestTimestamp())
                .setRechargePosition(RechargeServiceOuterClass.RechargeRequest.RechargePosition.newBuilder()
                        .setX(taxi.getX())
                        .setY(taxi.getY())
                        .build())
                .build();

        stub.recharge(rechargeReq, new StreamObserver<RechargeServiceOuterClass.RechargeOk>() {
            @Override
            public void onNext(RechargeServiceOuterClass.RechargeOk value) {
                System.out.println("Taxi " + t.getId() + " gave permission to recharge.");
                taxi.setRechargeReqCounter(taxi.getRechargeReqCounter() + 1);
                System.out.println("Counter: " + taxi.getRechargeReqCounter());
                //TODO
                synchronized (taxi){
                    taxi.notifyAll();
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
