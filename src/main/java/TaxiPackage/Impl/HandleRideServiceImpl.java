package TaxiPackage.Impl;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import taxi.communication.handleRideService.HandleRideServiceGrpc;
import taxi.communication.handleRideService.HandleRideServiceOuterClass;

public class HandleRideServiceImpl extends HandleRideServiceGrpc.HandleRideServiceImplBase {
    @Override
    public void election(HandleRideServiceOuterClass.ElectionMsg request, StreamObserver<HandleRideServiceOuterClass.HandleRideOk> responseObserver) {
        super.election(request, responseObserver);
    }

    @Override
    public void takeCharge(HandleRideServiceOuterClass.TakeChargeRide request, StreamObserver<Empty> responseObserver) {
        super.takeCharge(request, responseObserver);
    }
}
