package TaxiPackage.Threads;

import TaxiPackage.Taxi;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import seta.smartcity.rideRequest.RideRequestOuterClass;

import java.util.List;

import static Utils.Utils.*;
import static Utils.Utils.handledTopic;

public class Working extends TaxiThread {
    public Working(Taxi thisTaxi, Taxi.Status thisStatus, Object syncObj) {
        super(thisTaxi, thisStatus, syncObj);
        this.nextStatus.add(Taxi.Status.IDLE);
        this.nextStatus.add(Taxi.Status.REQUEST_RECHARGE);
        this.client = null;
    }

    private boolean isConnected = false;
    private MqttClient client;
    private final String broker = "tcp://localhost:1883";
    private final String handledTopic = "seta/smartcity/handled/";
    private final String availableTaxiTopic = "seta/smartcity/available/";

    @Override
    public void doStuff() throws InterruptedException {

        if (!isConnected) {
            try {
                client = initiateMqttClient();
                client.connect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
            isConnected = true;
        }

        RideRequestOuterClass.RideRequest requestToHandle = thisTaxi.getReqToHandle();
        String requestId = requestToHandle.getId();

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


    private MqttClient initiateMqttClient() throws MqttException {
        MqttClient mqttClient = new MqttClient(broker, MqttClient.generateClientId(), null);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);

        return mqttClient;
    }

    private void publishToHandleRequest(RideRequestOuterClass.RideRequest requestToHandle) throws MqttException{
        RideRequestOuterClass.RideRequest payload = requestToHandle;
        String destDist = computeDistrict(new int[]{payload.getStartingPosition().getX(), payload.getStartingPosition().getX()});
        MqttMessage message = new MqttMessage(payload.toByteArray());
        message.setQos(1);
        client.publish(handledTopic+destDist, message);
        System.out.println(printInformation("REQUEST", payload.getId()) + "will handled at " + handledTopic+destDist);
    }

}
