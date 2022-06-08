package TaxiPackage.Threads;

import TaxiPackage.Taxi;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import seta.smartcity.rideRequest.RideRequestOuterClass;

import static Utils.Utils.*;

public class Working extends TaxiThread {
    public Working(Taxi thisTaxi, Taxi.Status thisStatus, Object syncObj) {
        super(thisTaxi, thisStatus, syncObj);
        this.nextStatus.add(Taxi.Status.IDLE);
        this.nextStatus.add(Taxi.Status.REQUEST_RECHARGE);
    }

    @Override
    public void doStuff() throws InterruptedException {

        RideRequestOuterClass.RideRequest requestToHandle = thisTaxi.getReqToHandle();
        String requestId = requestToHandle.getId();
        thisTaxi.getElectionHandle().addHandled(requestToHandle);

        try {
            thisTaxi.getTaxiMQTT().publishToHandleRequest(requestToHandle);
        } catch (MqttException e) {
            e.printStackTrace();
        }

        int[] startingP = fromMsgToArray(requestToHandle.getStartingPosition());
        int[] destinationP = fromMsgToArray(requestToHandle.getDestinationPosition());

        System.out.println(thisStatus + printInformation("TAXI", thisTaxi.getId())
                + " located at " + thisTaxi.getX() + ", " + thisTaxi.getY() + " accepted" + printInformation("REQUEST", requestId)
                + " from " + getCoordX(startingP) + ", " + getCoordY(startingP)
                + " to " + getCoordX(destinationP) + ", " + getCoordY(destinationP));

        travel(thisTaxi.getCurrentP(), startingP, requestId, thisTaxi, false, 2.5f); // reach the user
        travel(startingP, destinationP, requestId, thisTaxi, true, 2.5f); // reach the final destination

        if (thisTaxi.getBattery() <= 30) { // after driving
            System.out.println(thisStatus + printInformation("TAXI", thisTaxi.getId()) + "needs recharge.");
            makeTransition(Taxi.Status.REQUEST_RECHARGE);
        } else {
            synchronized (thisTaxi.getInputLock()){
                thisTaxi.setReqToHandle(null);
                thisTaxi.getInputLock().notifyAll();
            }
            makeTransition(Taxi.Status.IDLE);
        }


    }
}
