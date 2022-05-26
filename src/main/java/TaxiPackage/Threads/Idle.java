package TaxiPackage.Threads;

import TaxiPackage.Taxi;
import org.eclipse.paho.client.mqttv3.*;
import seta.smartcity.rideRequest.RideRequestOuterClass;

import java.util.List;

public class Idle extends TaxiThread{
    private final String topicString = "seta/smartcity/rides/";
    private final int qos = 2; // check
    private boolean isConnected = false;
    private final String broker = "tcp://localhost:1883";
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
            subscribe(client);
            synchronized (inputLock){
                while(thisTaxi.getCurrentStatus() == Taxi.Status.IDLE){
                    if(thisTaxi.getInput() == null || thisTaxi.getInput() == Taxi.Input.WORK){
                        System.out.println("Waiting for input.");
                        inputLock.wait(); // TODO E SE GLI INPUT FOSSERO TRAMITE MQTT?
                        // qui c'è wait "ingiustificata"
                    }
                    // waits even if the input is not null!
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

    private MqttClient initiateMqttClient() throws MqttException {
        MqttClient mqttClient = new MqttClient(broker, MqttClient.generateClientId());
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        mqttClient.setCallback(new MqttCallback() { // taxi is IDLE (or else it's unsubscribed)
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {

                RideRequestOuterClass.RideRequest receivedMessage = RideRequestOuterClass.RideRequest.parseFrom(message.getPayload());
                System.out.println("Request " + receivedMessage.getId() + " arrived.");
                synchronized (inputLock){ // will be changed after election algorithm
                    if(thisTaxi.getInput() == null){
                        thisTaxi.setInput(Taxi.Input.WORK);
                        thisTaxi.setReqToHandle(receivedMessage);
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
        System.out.println(thisStatus + " Subscribed to " + topicString + thisTaxi.getDistrict());
    }

    private void unsubscribe(MqttClient client) throws MqttException {
        client.unsubscribe(thisTaxi.getTopicString() + thisTaxi.getDistrict());
        System.out.println(thisStatus + " Unsubscribed from " + topicString + thisTaxi.getDistrict());
    }
}
