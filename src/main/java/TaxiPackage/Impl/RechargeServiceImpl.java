package TaxiPackage.Impl;

import TaxiPackage.Taxi;
import io.grpc.stub.StreamObserver;
import seta.smartcity.rideRequest.RideRequestOuterClass;
import taxi.communication.joinService.JoinServiceOuterClass;
import taxi.communication.rechargeService.RechargeServiceGrpc;
import taxi.communication.rechargeService.RechargeServiceOuterClass;
import static Utils.Utils.*;

public class RechargeServiceImpl extends RechargeServiceGrpc.RechargeServiceImplBase {

    private Taxi thisTaxi;

    public RechargeServiceImpl(Taxi thisTaxi) {
        this.thisTaxi = thisTaxi;
    }

    @Override
    public void recharge(RechargeServiceOuterClass.RechargeRequest request, StreamObserver<RechargeServiceOuterClass.RechargeOk> responseObserver) {

        int[] destinationRecharge = new int[] {request.getRechargePosition().getX(), request.getRechargePosition().getY()};
        //fromMsgToArray(request.getRechargePosition().getX());
        String district = computeDistrict(destinationRecharge);

        System.out.println("[RECHARGE SRV] Taxi " + request.getId() + " requested recharge service at station " +
                getCoordX(destinationRecharge) + ", " + getCoordY(destinationRecharge));

        System.out.println("[RECHARGE SRV] Taxi " + request.getId() + " is waiting for recharge in district " + district);

        if(!thisTaxi.getId().equals(request.getId())){
            synchronized (thisTaxi){
                while(notOkCondition(thisTaxi, district, request.getTimestamp())){
                    try {
                        thisTaxi.wait();
                        if(!notOkCondition(thisTaxi, district, request.getTimestamp())){
                            notifyAll(); // now can acknowledge the request
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }


        responseObserver.onNext(RechargeServiceOuterClass.RechargeOk.newBuilder().build());
        System.out.println("[RECHARGE SRV] Acknowledged recharge request of taxi " + request.getId() + " in district " + district);

    }

    private static boolean notOkCondition(Taxi thisTaxi, String district, double timestamp){
        return (district.equals(thisTaxi.getDistrict()) &&
                (thisTaxi.getCurrentStatus() == Taxi.Status.REQUEST_RECHARGE && timestamp >=
                        thisTaxi.getRechargeRequestTimestamp())) ||
                thisTaxi.getCurrentStatus() == Taxi.Status.GO_RECHARGE;
    }

}
