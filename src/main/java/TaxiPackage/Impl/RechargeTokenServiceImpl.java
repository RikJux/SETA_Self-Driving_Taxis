package TaxiPackage.Impl;

import TaxiPackage.Taxi;
import beans.TaxiBean;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import taxi.communication.rechargeTokenService.RechargeTokenServiceGrpc;
import taxi.communication.rechargeTokenService.RechargeTokenServiceOuterClass;

public class RechargeTokenServiceImpl extends RechargeTokenServiceGrpc.RechargeTokenServiceImplBase{

    private Taxi thisTaxi;

    public RechargeTokenServiceImpl(Taxi thisTaxi) {
        this.thisTaxi = thisTaxi;
    }

    @Override
    public void rechargeToken(RechargeTokenServiceOuterClass.RechargeToken request, StreamObserver<RechargeTokenServiceOuterClass.RechargeOk> responseObserver) {

        System.out.println("Received [RECHARGE TOKEN " + request.getDistrict() + "]");
        thisTaxi.getTokens().add(request);
        responseObserver.onNext(RechargeTokenServiceOuterClass.RechargeOk.newBuilder().build());

    }

}
