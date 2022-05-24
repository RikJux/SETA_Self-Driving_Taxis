package TaxiPackage.Impl;

import TaxiPackage.Taxi;
import beans.TaxiBean;
import io.grpc.stub.StreamObserver;
import taxi.communication.leaveService.LeaveServiceGrpc;
import taxi.communication.leaveService.LeaveServiceOuterClass;

import java.util.List;

public class LeaveServiceImpl extends LeaveServiceGrpc.LeaveServiceImplBase {

    private Taxi thisTaxi;

    public LeaveServiceImpl(Taxi thisTaxi) {
        this.thisTaxi = thisTaxi;
    }
    @Override
    public void leave(LeaveServiceOuterClass.LeaveMsg request, StreamObserver<LeaveServiceOuterClass.LeaveOk> responseObserver) {
        System.out.println("[LEAVE SRV] Taxi " + request.getId() + " is leaving.");

        if(!thisTaxi.getId().equals(request.getId())){
            for(TaxiBean t: thisTaxi.getTaxiList()){
                if(t.getId().equals(request.getId())){
                    thisTaxi.getTaxiList().remove(t);
                    System.out.println("[LEAVE SRV] Taxi " + request.getId() + " was removed from taxi list.");
                    break;
                }
            }
        }

        responseObserver.onNext(LeaveServiceOuterClass.LeaveOk.newBuilder().build());
        responseObserver.onCompleted();
    }
}
