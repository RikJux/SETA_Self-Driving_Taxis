package TaxiPackage;

import beans.TaxiBean;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import taxi.communication.rechargeTokenService.RechargeTokenServiceGrpc;
import taxi.communication.rechargeTokenService.RechargeTokenServiceOuterClass;

import static Utils.Utils.printInformation;

public class TaxiRechargeTokenComm extends Thread{

    private final Taxi thisTaxi;

    public TaxiRechargeTokenComm(Taxi thisTaxi){
        this.thisTaxi = thisTaxi;
    }

    @Override
    public void run() {

        while(thisTaxi.getInput() != Taxi.Input.QUIT){
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sendToken(thisTaxi, thisTaxi.getTokens().extract());
        }

        while(thisTaxi.getTokens().size() != 0 && thisTaxi.getNextTaxi() !=
                new TaxiBean(thisTaxi.getId(), thisTaxi.getIp(), thisTaxi.getPort())){
            sendToken(thisTaxi, thisTaxi.getTokens().extract());
        }

        System.out.println("Send away all the recharge tokens");

    }

    private static ManagedChannel createChannel(Taxi thisTaxi){

        synchronized (thisTaxi.getNextLock()) {
            TaxiBean t = thisTaxi.getNextTaxi();
            ManagedChannel channel = ManagedChannelBuilder.forTarget(t.getIp() + ":" + t.getPort()).usePlaintext().build();
            thisTaxi.getNextLock().notifyAll();
            return channel;
        }

    }

    private static void sendToken(Taxi thisTaxi, RechargeTokenServiceOuterClass.RechargeToken token){

        final ManagedChannel channel = createChannel(thisTaxi);

        RechargeTokenServiceGrpc.RechargeTokenServiceStub stub = RechargeTokenServiceGrpc.newStub(channel);

        stub.rechargeToken(token, new StreamObserver<RechargeTokenServiceOuterClass.RechargeOk>() {
            @Override
            public void onNext(RechargeTokenServiceOuterClass.RechargeOk value) {
                System.out.println("Sent" + printInformation("RECHARGE TOKEN", token.getDistrict()));
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
