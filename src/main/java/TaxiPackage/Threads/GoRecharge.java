package TaxiPackage.Threads;

import TaxiPackage.Taxi;

import java.util.List;

import static Utils.Utils.*;

public class GoRecharge extends TaxiThread{
    public GoRecharge(Taxi thisTaxi, Taxi.Status thisStatus, Object syncObj) {
        super(thisTaxi, thisStatus, syncObj);
        this.nextStatus.add(Taxi.Status.IDLE);
    }

    @Override
    public void doStuff() throws InterruptedException {

        int[] destinationP = computeRechargeStation(thisTaxi.getDistrict());
        travel(thisTaxi.getCurrentP(), destinationP, null, thisTaxi, false, 5);
        System.out.println(thisStatus + " Reached the recharge station");
        Thread.sleep(10000);
        thisTaxi.setBattery(100.0);
        thisTaxi.setCurrentStatus(Taxi.Status.IDLE);
        System.out.println(thisStatus + " Taxi " + thisTaxi.getId() + " is now fully recharged.");

        thisTaxi.setRechargeRequestTimestamp(Double.MAX_VALUE);
        makeTransition(Taxi.Status.IDLE);

    }

}
