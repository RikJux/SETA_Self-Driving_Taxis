package TaxiPackage.Impl;

import TaxiPackage.Taxi;
import io.grpc.stub.StreamObserver;
import seta.smartcity.rideRequest.RideRequestOuterClass;
import taxi.communication.joinService.JoinServiceOuterClass;
import taxi.communication.rechargeService.RechargeServiceGrpc;
import taxi.communication.rechargeService.RechargeServiceOuterClass;

public class RechargeServiceImpl extends RechargeServiceGrpc.RechargeServiceImplBase {

    private Taxi thisTaxi;

    public RechargeServiceImpl(Taxi thisTaxi) {
        this.thisTaxi = thisTaxi;
    }

    @Override
    public void recharge(RechargeServiceOuterClass.RechargeRequest request, StreamObserver<RechargeServiceOuterClass.RechargeOk> responseObserver) {

        int[] destinationRecharge = fromMsgToArray(request.getRechargePosition());
        String district = computeDistrict(destinationRecharge);

        System.out.println("Taxi " + request.getId() + " requested recharge service at station " +
                getCoordX(destinationRecharge) + ", " + getCoordY(destinationRecharge));

        System.out.println("Taxi " + request.getId() + " is waiting for recharge in district " + district);

        while(notOkCondition(thisTaxi, district, request.getTimestamp())){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        responseObserver.onNext(RechargeServiceOuterClass.RechargeOk.newBuilder().build());
        System.out.println("Acknowledged recharge request of taxi " + request.getId() + " in district " + district);

    }

    private static boolean notOkCondition(Taxi thisTaxi, String district, double timestamp){
        return (district.equals(thisTaxi.getDistrict()) &&
                (thisTaxi.getCurrentStatus() == Taxi.Status.REQUEST_RECHARGE && timestamp >=
                        thisTaxi.getRechargeRequestTimestamp())) ||
                thisTaxi.getCurrentStatus() == Taxi.Status.GO_RECHARGE;
    }

    private static String computeDistrict(int[] p){
        int x = getCoordX(p);
        int y = getCoordY(p);
        String distN;

        if(y < 5){
            // we are in the upper city
            if(x < 5){
                distN = "1";
            }else{
                distN = "2";
            }
        }else{
            // we are in the lower city
            if(x < 5){
                distN = "4";
            }else{
                distN = "3";
            }
        }

        return "district_" + distN;
    }

    private static int getCoordX(int[] p){ return p[0];}

    private static int getCoordY(int[] p){ return p[1];}

    private static int[] fromMsgToArray(RechargeServiceOuterClass.RechargeRequest.RechargePosition pMsg){

        int[] p = new int[2];

        p[0] = pMsg.getX();
        p[1] = pMsg.getY();
        return p;

    }
}
