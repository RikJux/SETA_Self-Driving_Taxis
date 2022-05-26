package TaxiPackage.Threads;

import TaxiPackage.Taxi;

import java.util.List;

public class Elected extends TaxiThread{
    public Elected(Taxi thisTaxi, Taxi.Status thisStatus, List<Taxi.Status> nextStatus, Object syncObj) {
        super(thisTaxi, thisStatus, nextStatus, syncObj);
    }

    @Override
    public void doStuff() throws InterruptedException {
        // discard if elected many times
    }

}
