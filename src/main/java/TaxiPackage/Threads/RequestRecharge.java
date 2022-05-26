package TaxiPackage.Threads;

import TaxiPackage.Taxi;
import TaxiPackage.TaxiRechargeComm;

import java.util.List;

public class RequestRecharge extends TaxiThread{
    public RequestRecharge(Taxi thisTaxi, Taxi.Status thisStatus, Object syncObj) {
        super(thisTaxi, thisStatus, syncObj);
        this.nextStatus.add(Taxi.Status.GO_RECHARGE);
    }

    @Override
    public void doStuff() throws InterruptedException {

        TaxiRechargeComm r = new TaxiRechargeComm(thisTaxi);
        r.start();
        r.join();
        makeTransition(Taxi.Status.GO_RECHARGE);

    }

}
