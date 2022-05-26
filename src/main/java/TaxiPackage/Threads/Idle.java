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
    private MqttClient client;
    private static Object requestLock = new Object();

    public Idle(Taxi thisTaxi, Taxi.Status thisStatus, List<Taxi.Status> nextStatus, Object syncObj, MqttClient client) {
        super(thisTaxi, thisStatus, nextStatus, syncObj);
        this.client = null;
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
            synchronized (requestLock){
                while(thisTaxi.getReqToHandle() == null){
                    requestLock.wait();
                    if(thisTaxi.getReqToHandle() != null){
                        unsubscribe(client);
                        makeTransition(Taxi.Status.WORKING);
                        requestLock.notify();
                    }
                }
            }
            /*
            while(true){ // busy waiting!
                if(thisTaxi.getReqToHandle() == null){
                    Thread.sleep(1000);
                }else{
                    unsubscribe(client);
                    makeTransition(Taxi.Status.WORKING);
                    break;
                }
            }

             */
            // unsubscribe(client);
        } catch (Exception e) { // MqttException
            e.printStackTrace();
        }
        // election
        //thisTaxi.setReqToHandle(null);
        //makeTransition(Taxi.Status.WORKING);
        //makeTransition(Taxi.Status.REQUEST_RECHARGE);

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
                synchronized (requestLock){ // will be changed after election algorithm
                    thisTaxi.setReqToHandle(receivedMessage);
                    requestLock.notify();
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
