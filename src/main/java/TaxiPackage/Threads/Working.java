package TaxiPackage.Threads;

import TaxiPackage.Taxi;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import seta.smartcity.rideRequest.RideRequestOuterClass;

import java.util.List;

import static Utils.Utils.*;

public class Working extends TaxiThread{
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

        if(!isConnected){
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
        int[] startingP = fromMsgToArray(requestToHandle.getStartingPosition());
        int[] destinationP = fromMsgToArray(requestToHandle.getDestinationPosition());

        System.out.println(thisStatus + " Taxi " + thisTaxi.getId() + " located at " + thisTaxi.getX() + ", " + thisTaxi.getY() +
                " accepted request " + requestId + " from " + getCoordX(startingP) + ", " + getCoordY(startingP)
                + " to " + getCoordX(destinationP) + ", " + getCoordY(destinationP));

        travel(thisTaxi.getCurrentP(), startingP, requestId, thisTaxi, false, 2.5f); // reach the user
        travel(startingP, destinationP, requestId, thisTaxi, true, 2.5f); // reach the final destination

        if(thisTaxi.getBattery() <= 30){ // after driving
            System.out.println(thisStatus + " Taxi " + thisTaxi.getId() + " needs recharge.");
            thisTaxi.setRechargeRequestTimestamp(System.currentTimeMillis());
            makeTransition(Taxi.Status.REQUEST_RECHARGE);
        } else {
            makeTransition(Taxi.Status.IDLE);
        }

    }

    private void publishHandledRequest(){

    }

    private MqttClient initiateMqttClient() throws MqttException {
        MqttClient mqttClient = new MqttClient(broker, MqttClient.generateClientId(), null);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);

        return mqttClient;
    }

}
