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
        synchronized (syncObj){
            try {
                while(thisTaxi.getCurrentStatus() != thisStatus){
                    syncObj.wait();
                    if (thisTaxi.getCurrentStatus() == thisStatus) {
                        System.out.println(thisStatus + " acquired the lock.");
                        doStuff();
                        System.out.println(thisStatus + " released the lock.");
                        syncObj.notifyAll();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

    public abstract void doStuff() throws InterruptedException;

    public void makeTransition(Taxi.Status s){
        if(nextStatus.contains(s)){ // check if transaction is correct
            thisTaxi.setCurrentStatus(s);
        }
    }

}

