package Utils;

import Simulator.Measurement;
import seta.smartcity.rideRequest.RideRequestOuterClass;

import java.util.List;

public class Utils {

    public static final String DISTRICT_1 = "district1";
    public static final String DISTRICT_2 = "district2";
    public static final String DISTRICT_3 = "district3";
    public static final String DISTRICT_4 = "district4";

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

}
