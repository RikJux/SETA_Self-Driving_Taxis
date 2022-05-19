package TaxiPackage.Impl;

import io.grpc.stub.StreamObserver;
import taxi.communication.joinService.JoinServiceGrpc;
import taxi.communication.joinService.JoinServiceOuterClass;

public class JoinServiceImpl extends JoinServiceGrpc.JoinServiceImplBase {

    @Override
    public void join(JoinServiceOuterClass.JoinMsg request, StreamObserver<JoinServiceOuterClass.JoinOk> responseObserver) {
        System.out.println(request);
        // qui la logica su request
        responseObserver.onNext(JoinServiceOuterClass.JoinOk.newBuilder().build());
        responseObserver.onCompleted();
    }
}
