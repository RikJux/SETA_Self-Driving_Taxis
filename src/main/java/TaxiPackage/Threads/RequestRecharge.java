package TaxiPackage.Threads;

import TaxiPackage.Taxi;
import TaxiPackage.TaxiRechargeComm;
import taxi.communication.rechargeTokenService.RechargeTokenServiceOuterClass;

import java.util.List;

public class RequestRecharge extends TaxiThread{
    public RequestRecharge(Taxi thisTaxi, Taxi.Status thisStatus, Object syncObj) {
        super(thisTaxi, thisStatus, syncObj);
        this.nextStatus.add(Taxi.Status.GO_RECHARGE);
    }

    @Override
    public void doStuff() throws InterruptedException {

        RechargeTokenServiceOuterClass.RechargeToken rechargeToken = thisTaxi.getTokens().remove();
        if(rechargeToken.getDistrict() == thisTaxi.getDistrict()){
            makeTransition(Taxi.Status.GO_RECHARGE);
        }
        thisTaxi.getTokens().add(rechargeToken);
    }

}
