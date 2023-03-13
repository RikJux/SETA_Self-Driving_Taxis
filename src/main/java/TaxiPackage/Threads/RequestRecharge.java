package TaxiPackage.Threads;

import TaxiPackage.Taxi;

import static Utils.Utils.*;

public class RequestRecharge extends TaxiThread{
    public RequestRecharge(Taxi thisTaxi, Taxi.Status thisStatus, Object syncObj) {
        super(thisTaxi, thisStatus, syncObj);
        this.nextStatus.add(Taxi.Status.GO_RECHARGE);
    }

    @Override
    public void doStuff() throws InterruptedException {

        System.out.println("Waiting for" + printInformation("RECHARGE TOKEN", thisTaxi.getDistrict()));
        thisTaxi.getTokens().setInUse(createRechargeToken(thisTaxi.getDistrict()));
        makeTransition(Taxi.Status.GO_RECHARGE);

    }

}
