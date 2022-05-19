package TaxiPackage.Impl;

import io.grpc.stub.StreamObserver;
import taxi.communication.joinService.JoinServiceGrpc;
import taxi.communication.joinService.JoinServiceOuterClass;

public class JoinServiceImpl extends JoinServiceGrpc.JoinServiceImplBase {

    @Override
    public void join(JoinServiceOuterClass.JoinMsg request, StreamObserver<JoinServiceOuterClass.JoinOk> responseObserver) {
        super.join(request, responseObserver);
    }
}
