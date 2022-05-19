package TaxiPackage.Impl;

import io.grpc.stub.StreamObserver;
import taxi.communication.leaveService.LeaveServiceGrpc;
import taxi.communication.leaveService.LeaveServiceOuterClass;

public class LeaveServiceImpl extends LeaveServiceGrpc.LeaveServiceImplBase {
    @Override
    public void leave(LeaveServiceOuterClass.LeaveMsg request, StreamObserver<LeaveServiceOuterClass.LeaveOk> responseObserver) {
        super.leave(request, responseObserver);
    }
}
