package TaxiPackage.Impl;

import io.grpc.stub.StreamObserver;
import taxi.communication.rechargeService.RechargeServiceGrpc;
import taxi.communication.rechargeService.RechargeServiceOuterClass;

public class RechargeService extends RechargeServiceGrpc.RechargeServiceImplBase {
    @Override
    public void recharge(RechargeServiceOuterClass.RechargeRequest request, StreamObserver<RechargeServiceOuterClass.RechargeOk> responseObserver) {
        super.recharge(request, responseObserver);
    }
}
