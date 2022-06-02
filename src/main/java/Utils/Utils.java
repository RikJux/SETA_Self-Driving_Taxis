package Utils;

import Simulator.Measurement;
import TaxiPackage.Taxi;
import beans.TaxiBean;
import seta.smartcity.rideRequest.RideRequestOuterClass;
import taxi.communication.rechargeTokenService.RechargeTokenServiceOuterClass;

import java.util.List;

public class Utils {

    public static final String DISTRICT_1 = "district1";
    public static final String DISTRICT_2 = "district2";
    public static final String DISTRICT_3 = "district3";
    public static final String DISTRICT_4 = "district4";

    public static final String broker = "tcp://localhost:1883";
    public static final String handledTopic = "seta/smartcity/handled/";
    public static final String ridesTopic = "seta/smartcity/rides/"; // add the string related to the district
    public static final String availableTopic = "seta/smartcity/available/";

    public static int[] computeRechargeStation(String district) {
        switch (district) {
            case (DISTRICT_1):
                return new int[]{0, 0};
            case (DISTRICT_2):
                return new int[]{9, 0};
            case (DISTRICT_3):
                return new int[]{9, 9};
            case (DISTRICT_4):
                return new int[]{0, 9};
            default:
                return new int[]{-1, -1}; // watch out
        }
    }

    public static double computeDistance(int[] p1, int[] p2) {
        return Math.sqrt(Math.pow(getCoordX(p1) - getCoordX(p2), 2) +
                Math.pow(getCoordY(p1) - getCoordY(p2), 2));
    }

    public static String computeDistrict(int[] p) {
        int x = getCoordX(p);
        int y = getCoordY(p);

        if (y < 5) {
            // we are in the upper city
            if (x < 5) {
                return DISTRICT_1;
            } else {
                return DISTRICT_2;
            }
        } else {
            // we are in the lower city
            if (x < 5) {
                return DISTRICT_4;
            } else {
                return DISTRICT_3;
            }
        }
    }

    public static String computeDistrict(RideRequestOuterClass.RideRequest r){
        return computeDistrict(new int[]{r.getStartingPosition().getX(), r.getStartingPosition().getY()});
    }

    public static int getCoordX(int[] p) {
        return p[0];
    }

    public static int getCoordY(int[] p) {
        return p[1];
    }

    public static int[] fromMsgToArray(RideRequestOuterClass.RideRequest.Position pMsg) {

        int[] p = new int[2];

        p[0] = pMsg.getX();
        p[1] = pMsg.getY();
        return p;

    }

    public static double avgPollution(List<Measurement> pollution){

        double sum = 0;
        for(Measurement m: pollution){
            sum += m.getValue();
        }

        return sum/pollution.size();
    }

    public static void travel(int[] startingP, int[] destinationP, String requestId, Taxi taxi, boolean accomplished, float time) throws InterruptedException {

        Thread.sleep((long) (time * 1000));
        double distance = computeDistance(startingP, destinationP);
        taxi.setCurrentP(destinationP);
        taxi.lowerBattery(distance);
        taxi.addKilometers(distance);
        taxi.setDistrict(computeDistrict(destinationP));
        if (accomplished) {
            taxi.addRideAccomplished();
            System.out.println("[TAXI DRIVER] Taxi " + taxi.getId() + " fulfilled request " + requestId);
        }
    }

    public static TaxiBean extractNextTaxi(Taxi taxi) {

        TaxiBean resultingNext = new TaxiBean(taxi.getId(), taxi.getIp(), taxi.getPort());

        int id = Integer.parseInt(taxi.getId());

        for (TaxiBean t : taxi.getTaxiList()) {
            int thisId = Integer.parseInt(t.getId());
            int nextId = Integer.parseInt(resultingNext.getId());

            if(thisId > id){
                if(nextId <= id){
                    // taxi t is the next because it has greater id than this taxi and current next is lower
                    resultingNext = t;
                }else{ // both the two candidates (this and next) have greater id than taxi
                    if(thisId < nextId){
                        // among two taxis with greater id, pick the one with minimum id
                        resultingNext = t;
                    }
                }
            }else{
                if(nextId <= id && thisId < nextId){
                    // condition needed in case that the taxi has maximum id, so the next will be the taxi with minimum id
                    resultingNext = t;
                }
            }
        }

        return resultingNext;

    }

    public static TaxiBean updateNextOnJoin(Taxi taxi, TaxiBean joinTaxiBean){

        TaxiBean resultingNext = taxi.getNextTaxi();

        int id = Integer.parseInt(taxi.getId());
        int nextId = Integer.parseInt(resultingNext.getId());
        int thisId = Integer.parseInt(joinTaxiBean.getId());

        if(thisId < nextId){
            if(thisId > id){
                resultingNext = joinTaxiBean;
            }else{
                if(nextId <= id){
                    resultingNext = joinTaxiBean;
                }
            }
        }else{
            if(thisId > id && nextId < id){
                resultingNext = joinTaxiBean;
            }
            if(id == nextId){
                resultingNext = joinTaxiBean;
            }
        }
        return resultingNext;
    }

    public static TaxiBean updateNextOnLeave(Taxi taxi, String leaveId){

        TaxiBean resultingNext = taxi.getNextTaxi();

        if(resultingNext.getId().equals(leaveId)){
            resultingNext = extractNextTaxi(taxi);
        }

        return resultingNext;

    }

    public static RechargeTokenServiceOuterClass.RechargeToken createRechargeToken(String district){
        return RechargeTokenServiceOuterClass.RechargeToken.newBuilder().setDistrict(district).build();
    }

}
