package TaxiPackage.Threads;

import TaxiPackage.Taxi;
import org.eclipse.paho.client.mqttv3.*;
import seta.smartcity.rideRequest.RideRequestOuterClass;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static Utils.Utils.*;

public class Idle extends TaxiThread{
    private final int qos = 1;
    private boolean isConnected = false;
    private Object inputLock;
    private MqttClient client;
    private static Object requestLock = new Object();


    public Idle(Taxi thisTaxi, Taxi.Status thisStatus, Object syncObj) {
        super(thisTaxi, thisStatus, syncObj);
        this.client = null;
        this.inputLock = thisTaxi.getInputLock();
        this.nextStatus.add(Taxi.Status.REQUEST_RECHARGE);
        this.nextStatus.add(Taxi.Status.WORKING);
        this.nextStatus.add(Taxi.Status.LEAVING);
    }

    @Override
    public void doStuff() throws InterruptedException{
        try {
            if(!isConnected){
                client = initiateMqttClient();
                client.connect();
                isConnected = true;
            }
            synchronized (inputLock){
                thisTaxi.setInput(null);
            }
            subscribe(client);
            thisTaxi.setReqToHandle(null);
            synchronized (thisTaxi.getRechargeTimestampLock()){
                thisTaxi.setRechargeRequestTimestamp(Double.MAX_VALUE); // GO_RECHARGE
            }

            client.publish(availableTopic+thisTaxi.getDistrict(), new MqttMessage("".getBytes()));
            System.out.println("Made taxi available at " + availableTopic+thisTaxi.getDistrict());

            synchronized (inputLock){
                while(thisTaxi.getCurrentStatus() == Taxi.Status.IDLE){
                    if(thisTaxi.getInput() == null){
                        System.out.println("Waiting for input.");
                        inputLock.wait();
                    }
                    if(thisTaxi.getInput() != null){
                        System.out.println("Input arrived.");
                        unsubscribe(client);
                        switch (thisTaxi.getInput()){
                            case QUIT:
                                makeTransition(Taxi.Status.LEAVING);
                                break;
                            case WORK:
                                makeTransition(Taxi.Status.WORKING);
                                break;
                            case RECHARGE:
                                makeTransition(Taxi.Status.REQUEST_RECHARGE);
                                break;
                            default:
                                System.out.println("Error in input at " + thisStatus + " : " + thisTaxi.getCurrentStatus());
                                makeTransition(Taxi.Status.LEAVING);
                                break;
                        }
                    }
                }
                inputLock.notifyAll();
            }

        } catch (Exception e) { // MqttException
            e.printStackTrace();
        }

    }

    @Override
    public void makeTransition(Taxi.Status s) {
        super.makeTransition(s);
        thisTaxi.setInput(null);
        inputLock.notifyAll();
    }

    private void publishToHandleRequest() throws MqttException{
        RideRequestOuterClass.RideRequest payload = thisTaxi.getReqToHandle();
        String destDist = computeDistrict(new int[]{payload.getStartingPosition().getX(), payload.getStartingPosition().getX()});
        MqttMessage message = new MqttMessage(payload.toByteArray());
        message.setQos(1);
        client.publish(handledTopic+destDist, message);
        System.out.println("[REQUEST : " + payload.getId() + "] will handled at " + handledTopic+destDist);
    }

    private MqttClient initiateMqttClient() throws MqttException {
        MqttClient mqttClient = new MqttClient(broker, MqttClient.generateClientId(), null);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        mqttClient.setCallback(new MqttCallback() { // taxi is IDLE (or else it's unsubscribed)
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {

                RideRequestOuterClass.RideRequest receivedMessage = RideRequestOuterClass.RideRequest.parseFrom(message.getPayload());
                System.out.println("[REQUEST : " + receivedMessage.getId() + "] arrived.");
                synchronized (inputLock){ // will be changed after election algorithm
                    if(thisTaxi.getInput() == null && thisTaxi.getReqToHandle() == null){
                        thisTaxi.setInput(Taxi.Input.WORK);
                        thisTaxi.setReqToHandle(receivedMessage);
                        publishToHandleRequest();
                        }
                    inputLock.notifyAll();
                    }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        return mqttClient;
    }

    private void subscribe(MqttClient client) throws MqttException {
        client.subscribe(thisTaxi.getTopicString() + thisTaxi.getDistrict(), qos);
        System.out.println(thisStatus + " Subscribed to " + ridesTopic + thisTaxi.getDistrict());
    }

    private void unsubscribe(MqttClient client) throws MqttException {
        client.unsubscribe(thisTaxi.getTopicString() + thisTaxi.getDistrict());
        System.out.println(thisStatus + " Unsubscribed from " + ridesTopic + thisTaxi.getDistrict());
    }
}
