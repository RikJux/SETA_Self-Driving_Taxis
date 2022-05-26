package TaxiPackage.Threads;

import TaxiPackage.Taxi;

import java.util.List;

import static Utils.Utils.*;

public class Working extends TaxiThread{
    public Working(Taxi thisTaxi, Taxi.Status thisStatus, List<Taxi.Status> nextStatus, Object syncObj) {
        super(thisTaxi, thisStatus, nextStatus, syncObj);
    }

    @Override
    public void doStuff() throws InterruptedException {

        String requestId = thisTaxi.getReqToHandle().getId();
        int[] startingP = fromMsgToArray(thisTaxi.getReqToHandle().getStartingPosition());
        int[] destinationP = fromMsgToArray(thisTaxi.getReqToHandle().getDestinationPosition());

        System.out.println("[TAXI DRIVER] Taxi " + thisTaxi.getId() + " located at " + thisTaxi.getX() + ", " + thisTaxi.getY() +
                " accepted request " + requestId + " from " + getCoordX(startingP) + ", " + getCoordY(startingP)
                + " to " + getCoordX(destinationP) + ", " + getCoordY(destinationP));

        travel(thisTaxi.getCurrentP(), startingP, requestId, thisTaxi, false, 2.5f); // reach the user
        travel(startingP, destinationP, requestId, thisTaxi, true, 2.5f); // reach the final destination

        if(thisTaxi.getBattery() <= 30){ // after driving
            System.out.println("[TAXI DRIVER] Taxi " + thisTaxi.getId() + " needs recharge.");
            thisTaxi.setRechargeRequestTimestamp(System.currentTimeMillis());
            makeTransition(Taxi.Status.REQUEST_RECHARGE);
        } else {
            makeTransition(Taxi.Status.IDLE);
        }

    }
}