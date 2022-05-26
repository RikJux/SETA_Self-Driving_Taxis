package TaxiPackage.Impl;

import TaxiPackage.ElectionIdentifier;
import TaxiPackage.Taxi;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import taxi.communication.handleRideService.HandleRideServiceGrpc;
import taxi.communication.handleRideService.HandleRideServiceOuterClass;

public class HandleRideServiceImpl extends HandleRideServiceGrpc.HandleRideServiceImplBase {

    private Taxi thisTaxi;

    public HandleRideServiceImpl(Taxi thisTaxi){
        this.thisTaxi = thisTaxi;
    }

    @Override
    public void election(HandleRideServiceOuterClass.ElectionMsg request, StreamObserver<HandleRideServiceOuterClass.HandleRideOk> responseObserver) {

        ElectionIdentifier newCandidate = new ElectionIdentifier(request.getCandidateMsg());

        System.out.println("[REQUEST SRV] Request " + request.getRideRequest() + " to handle.");
        ElectionIdentifier thisCandidate = new ElectionIdentifier(thisTaxi, Double.MAX_VALUE); // will always lose

        if(thisCandidate.compareTo(newCandidate) < 0){
            System.out.println("[REQUEST SRV] Request " + request.getRideRequest() + " has a better candidate in " +
                    newCandidate.toString());
            responseObserver.onCompleted();
        }else{ // this taxi is better or exactly the same
            responseObserver.onNext(HandleRideServiceOuterClass.HandleRideOk.newBuilder().setId(request.getRideRequest()).build());
        }

    }

    @Override
    public void takeCharge(HandleRideServiceOuterClass.TakeChargeRide request, StreamObserver<Empty> responseObserver) {
        super.takeCharge(request, responseObserver);
    }
}
