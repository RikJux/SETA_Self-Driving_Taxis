import java.util.Random;
import org.eclipse.paho.client.mqttv3.*;
import seta.smartcity.rideRequest.RideRequestOuterClass.RideRequest;
import seta.smartcity.rideRequest.RideRequestOuterClass.RideRequest.Position;

public class SETA {

    public static void main(String args[]) throws InterruptedException, MqttException {

        // create some RideRequest and publish them!
        MqttClient client;
        String broker = "tcp://localhost:1883";
        String clientId = MqttClient.generateClientId();
        String topic = "seta/smartcity/rides/"; // add the string related to the district
        int qos = 2; // why?

        client = new MqttClient(broker, clientId); //create client
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);

        System.out.println(clientId + " Connecting Broker " + broker);
        client.connect(connOpts);
        System.out.println(clientId + " Connected");

        Random rand = new Random();
        int id = 0;

        while(true){ // busy waiting
            Thread.sleep(5000); // publish 2 requests
            RideRequest payload = generateRideRequest(rand, id++);
            String destDist = getDistrict(payload.getStartingPosition());
            MqttMessage message = new MqttMessage(payload.toByteArray());
            message.setQos(2);
            // should notify the SETA that request was handled
            System.out.println(clientId + " Publishing message: " + payload + " ...");
            client.publish(topic+destDist, message);
            System.out.println(clientId + " Message published at " + topic+destDist);

        }
    }

    private static RideRequest generateRideRequest(Random rand, int id){

        Position startingP = Position.newBuilder()
                .setX(rand.nextInt(10))
                .setY(rand.nextInt(10))
                .build();

        Position destinationP = Position.newBuilder()
                .setX(rand.nextInt(10))
                .setY(rand.nextInt(10))
                .build();

        return RideRequest.newBuilder()
                .setId(String.valueOf(id))
                .setStartingPosition(startingP)
                .setDestinationPosition(destinationP)
                .build();

    }

    private static String getDistrict(Position p){
        int x = p.getX();
        int y = p.getY();
        String distN;

        if(y < 5){
            // we are in the upper city
            if(x < 5){
                distN = "1";
            }else{
                distN = "2";
            }
        }else{
            // we are in the lower city
            if(x < 5){
                distN = "4";
            }else{
                distN = "3";
            }
        }

        return "district" + distN;
    }
}
