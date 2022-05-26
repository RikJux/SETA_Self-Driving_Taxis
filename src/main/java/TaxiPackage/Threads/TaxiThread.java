package TaxiPackage.Threads;

import TaxiPackage.Taxi;

import java.util.List;

public abstract class TaxiThread extends Thread {

    public static Taxi thisTaxi;
    public final Taxi.Status thisStatus;
    public final List<Taxi.Status> nextStatus;
    public final Object syncObj;

    public TaxiThread(Taxi thisTaxi, Taxi.Status thisStatus, List<Taxi.Status> nextStatus, Object syncObj) {
        this.thisTaxi = thisTaxi;
        this.thisStatus = thisStatus;
        this.nextStatus = nextStatus;
        this.syncObj = syncObj;
    }
    @Override
    public void run() {
        synchronized (thisTaxi){
            try {
                while(thisTaxi.getCurrentStatus() != thisStatus){
                    System.out.println(thisStatus + " waiting for the lock");
                    thisTaxi.wait();
                    if (thisTaxi.getCurrentStatus() == thisStatus) {
                        doStuff();
                        makeTransition();
                        System.out.println(thisStatus + " releasing the lock");
                        thisTaxi.notifyAll();
                        return;
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

    public abstract void doStuff() throws InterruptedException;
    public abstract void makeTransition();

}

