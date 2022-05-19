package TaxiPackage.Impl;

import io.grpc.stub.StreamObserver;
import taxi.communication.leaveService.LeaveServiceGrpc;
import taxi.communication.leaveService.LeaveServiceOuterClass;

public class LeaveServiceImpl extends LeaveServiceGrpc.LeaveServiceImplBase {
    @Override
    public void leave(LeaveServiceOuterClass.LeaveMsg request, StreamObserver<LeaveServiceOuterClass.LeaveOk> responseObserver) {
        System.out.println(request);
        // qui la logica su request
        responseObserver.onNext(LeaveServiceOuterClass.LeaveOk.newBuilder().build());
        responseObserver.onCompleted();
    }
}
