package Utils;

import Simulator.Measurement;
import TaxiPackage.Taxi;
import beans.TaxiBean;
import seta.smartcity.rideRequest.RideRequestOuterClass;
import taxi.communication.handleRideService.HandleRideServiceOuterClass;
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

    public static int[] fromMsgToArray(HandleRideServiceOuterClass.RideRequest.Position pMsg) {

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
        taxi.setDistrict(computeDistrict(destinationP));
        taxi.lowerBattery(distance);
        taxi.addKilometers(distance);
        if (accomplished) {
            taxi.addRideAccomplished();
            System.out.println("Taxi " + taxi.getId() + " fulfilled request " + requestId);
        }
    }

    public static TaxiBean extractNextTaxi(Taxi taxi){

        TaxiBean thisTaxiBean = new TaxiBean(taxi.getId(), taxi.getIp(), taxi.getPort());
        TaxiBean nextTaxiBean = thisTaxiBean;
        TaxiBean minTaxiBean = thisTaxiBean;

        for(TaxiBean taxiBean: taxi.getTaxiList()){
            int thisId = Integer.parseInt(thisTaxiBean.getId());
            int nextId = Integer.parseInt(nextTaxiBean.getId());
            int beanId = Integer.parseInt(taxiBean.getId());
            int minId = Integer.parseInt(minTaxiBean.getId());
            if(beanId > thisId && (beanId < nextId || nextId == thisId)){
                nextTaxiBean = taxiBean;
            }
            if(beanId < minId){
                minTaxiBean = taxiBean;
            }
        }

        if(nextTaxiBean.getId().equals(thisTaxiBean.getId())){
            return minTaxiBean;
        }

        return nextTaxiBean;

    }

    public static TaxiBean updateNextOnJoin(Taxi taxi, TaxiBean joinTaxiBean){

        return extractNextTaxi(taxi);
    }

    public static TaxiBean updateNextOnLeave(Taxi taxi, String leaveId){

        return extractNextTaxi(taxi);

    }

    public static RechargeTokenServiceOuterClass.RechargeToken createRechargeToken(String district){
        return RechargeTokenServiceOuterClass.RechargeToken.newBuilder().setDistrict(district).build();
    }

    public static String printInformation(String type, String content){
        return " [" + type + " " + content + "] ";
    }

    public static HandleRideServiceOuterClass.ElectionMsg.CandidateMsg createCandidateMsg(Taxi thisTaxi, RideRequestOuterClass.RideRequest request){
        double distance = computeDistance(thisTaxi.getCurrentP(), fromMsgToArray(request.getStartingPosition()));
        return HandleRideServiceOuterClass.ElectionMsg.CandidateMsg.newBuilder()
                .setIdle(thisTaxi.getCurrentStatus() == Taxi.Status.IDLE)
                .setDistance(distance)
                .setBatteryLevel(thisTaxi.getBattery())
                .setId(thisTaxi.getId())
                .build();
    }

    public static HandleRideServiceOuterClass.RideRequest translateRideRequest(RideRequestOuterClass.RideRequest request){
        return HandleRideServiceOuterClass.RideRequest.newBuilder()
                .setId(request.getId())
                .setStartingPosition(translatePosition(request.getStartingPosition()))
                .setDestinationPosition(translatePosition(request.getDestinationPosition()))
                .build();
    }

    public static RideRequestOuterClass.RideRequest translateRideRequest(HandleRideServiceOuterClass.RideRequest request){
        return RideRequestOuterClass.RideRequest .newBuilder()
                .setId(request.getId())
                .setStartingPosition(translatePosition(request.getStartingPosition()))
                .setDestinationPosition(translatePosition(request.getDestinationPosition()))
                .build();
    }

    private static HandleRideServiceOuterClass.RideRequest.Position translatePosition(RideRequestOuterClass.RideRequest.Position position){
        return HandleRideServiceOuterClass.RideRequest.Position.newBuilder()
                .setX(position.getX())
                .setY(position.getY())
                .build();
    }
    private static RideRequestOuterClass.RideRequest.Position translatePosition(HandleRideServiceOuterClass.RideRequest.Position position){
        return RideRequestOuterClass.RideRequest.Position.newBuilder()
                .setX(position.getX())
                .setY(position.getY())
                .build();
    }

}
