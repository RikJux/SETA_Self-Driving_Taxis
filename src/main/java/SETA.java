import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import org.eclipse.paho.client.mqttv3.*;
import seta.smartcity.rideRequest.RideRequestOuterClass.RideRequest;
import seta.smartcity.rideRequest.RideRequestOuterClass.RideRequest.Position;

import static Utils.Utils.*;

public class SETA {

    private static Hashtable<String, List<RideRequest>> unhandledRequests = new Hashtable<String, List<RideRequest>>();

    public static void main(String args[]) throws InterruptedException, MqttException {

        // create some RideRequest and publish them!
        MqttClient client;
        String broker = "tcp://localhost:1883";
        String clientId = MqttClient.generateClientId();
        String topic = "seta/smartcity/rides/"; // add the string related to the district
        String handledTopic = "seta/smartcity/handled/";
        String availableTaxiTopic = "seta/smartcity/available/";

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

                switch(topic){
                    case "seta/smartcity/handled/"+DISTRICT_1:
                        break;
                    case "seta/smartcity/handled/"+DISTRICT_2:
                        break;
                    case "seta/smartcity/handled/"+DISTRICT_3:
                        break;
                    case "seta/smartcity/handled/"+DISTRICT_4:
                        break;
                    default:
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

        Random rand = new Random();
        int id = 0;

        while(true){
            Thread.sleep(5000); // publish 2 requests each 5 seconds
            for(int i=0; i<2; i++){
            publishRequest(rand, id++, clientId, client, topic);
            }

        }
    }

    private static void publishRequest(Random rand, int id, String clientId, MqttClient client, String topic) throws MqttException {
        RideRequest payload = generateRideRequest(rand, id);
        String destDist = computeDistrict(new int[]{payload.getStartingPosition().getX(), payload.getStartingPosition().getY()});
        MqttMessage message = new MqttMessage(payload.toByteArray());
        message.setQos(1);
        // should notify the SETA that request was handled
        client.publish(topic+destDist, message);
        System.out.println(clientId + "[REQUEST : " + payload.getId() + "] published.");
        unhandledRequests.get(destDist).add(payload);
        System.out.println(clientId + "[REQUEST : " + payload.getId() + "] put in unhandled data structure.");
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
}
