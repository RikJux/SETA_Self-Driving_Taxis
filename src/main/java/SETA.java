import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import org.eclipse.paho.client.mqttv3.*;
import seta.smartcity.rideRequest.RideRequestOuterClass;
import seta.smartcity.rideRequest.RideRequestOuterClass.RideRequest;
import seta.smartcity.rideRequest.RideRequestOuterClass.RideRequest.Position;

import static Utils.Utils.*;

public class SETA {

    private static Object unhandledLock = new Object();

    private static Hashtable<String, List<RideRequest>> unhandledRequests = new Hashtable<String, List<RideRequest>>();

    public static void main(String args[]) throws InterruptedException, MqttException {

        // create some RideRequest and publish them!
        MqttClient client;
        String clientId = MqttClient.generateClientId();

        unhandledRequests.put(DISTRICT_1, new ArrayList<RideRequest>());
        unhandledRequests.put(DISTRICT_2, new ArrayList<RideRequest>());
        unhandledRequests.put(DISTRICT_3, new ArrayList<RideRequest>());
        unhandledRequests.put(DISTRICT_4, new ArrayList<RideRequest>());


        client = new MqttClient(broker, clientId, null); //create client
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {

                System.out.println("Message from: " + topic);
                if(topic.contains(handledTopic)){

                    RideRequest handledMessage = RideRequestOuterClass.RideRequest.parseFrom(message.getPayload());
                    System.out.println(printInformation("REQUEST", handledMessage.getId()) + "is being handled.");

                    boolean ok;
                    synchronized (unhandledLock){
                        ok = unhandledRequests.get(computeDistrict(handledMessage)).remove(handledMessage);
                    }
                    if(ok){
                        System.out.println("Correctly removed" + printInformation("REQUEST", handledMessage.getId()));
                    }else{
                        System.out.println("Something wrong in removing" + printInformation("REQUEST", handledMessage.getId()));
                    }


                } else if (topic.contains(availableTopic)) {

                    String dist = fetchDistrictFromTopic(topic, availableTopic);
                    List<RideRequest> toSend;
                    synchronized (unhandledLock){
                        toSend = new ArrayList<RideRequest>(unhandledRequests.get(dist));
                    }
                    publishManyRequests(toSend, client);

                }else{
                    System.out.println("Received message from illegal topic");
                }

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        System.out.println(clientId + " Connecting Broker " + broker);
        client.connect(connOpts);
        System.out.println(clientId + " Connected");
        client.subscribe(handledTopic+"+", 1);
        client.subscribe(availableTopic+"+", 1);

        Random rand = new Random();

        new Thread(() -> {
            int id = 0;
            while (true) {
                try {
                    Thread.sleep(5000); // generate 2 requests each 5 seconds
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for (int i = 0; i < 2; i++) {
                    RideRequest r = generateRideRequest(rand, id++);
                    synchronized (unhandledLock){
                        unhandledRequests.get(computeDistrict(r)).add(r);
                    }
                    System.out.println(printInformation("REQUEST", r.getId()) + "put in unhandled data structure");
                }
            }
        }).start();

    }

    private static void publishManyRequests(List<RideRequest> toSend, MqttClient client){

        for(RideRequest r: toSend){
            try {
                Thread.sleep(1000);
                publishRequest(r, client, ridesTopic);
            } catch (MqttException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private static void publishRequest(RideRequest payload, MqttClient client, String topic) throws MqttException {

        String dist = computeDistrict(payload);
        MqttMessage message = new MqttMessage(payload.toByteArray());
        message.setQos(1);
        client.publish(topic+dist, message);
        System.out.println(printInformation("REQUEST", payload.getId()) + "published");

    }

    private static RideRequest generateRideRequest(Random rand, int id) {

        Position startingP = Position.newBuilder()
                .setX(rand.nextInt(10))
                .setY(rand.nextInt(10))
                .build();


        int destX;
        int destY;
        do {
            destX = rand.nextInt(10);
            destY = rand.nextInt(10);
        } while (destX == startingP.getX() && destY == startingP.getY());

        Position destinationP = Position.newBuilder()
                .setX(destX)
                .setY(destY)
                .build();

        return RideRequest.newBuilder()
                .setId(String.valueOf(id))
                .setStartingPosition(startingP)
                .setDestinationPosition(destinationP)
                .build();

    }

    private static String fetchDistrictFromTopic(String topic, String mainTopic){
        switch(topic.substring(mainTopic.length())){
            case DISTRICT_1:
                return DISTRICT_1;
            case DISTRICT_2:
                return DISTRICT_2;
            case DISTRICT_3:
                return DISTRICT_3;
            case DISTRICT_4:
                return DISTRICT_4;
            default:
                System.out.println("Received message from illegal topic");
                return null;
        }

    }

}
