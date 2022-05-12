package TaxiPackage;

import com.google.protobuf.InvalidProtocolBufferException;
import org.eclipse.paho.client.mqttv3.*;
import seta.smartcity.rideRequest.RideRequestOuterClass;

public class TaxiDriver extends Thread{

    private final Taxi taxi;

    public TaxiDriver(Taxi taxi){
        this.taxi = taxi;
    }

    public void run(){
        MqttClient client;
        String broker = "tcp://localhost:1883";
        String clientId = MqttClient.generateClientId();
        int qos = 2; // why?

        try {
            client = new MqttClient(broker, clientId);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);

            System.out.println(clientId + " Connecting Broker " + broker);
            client.connect(connOpts);
            System.out.println(clientId + " Connected - Thread PID: " + Thread.currentThread().getId());

            client.setCallback(new MqttCallback() {

                public void messageArrived(String topic, MqttMessage message) throws MqttException, InvalidProtocolBufferException, InterruptedException {
                    // Called when a message arrives from the server that matches any subscription made by the client;
                    // should coordinate with other taxis to decide who handles the request
                    // receive request
                    RideRequestOuterClass.RideRequest receivedMessage = RideRequestOuterClass.RideRequest.parseFrom(message.getPayload());
                    String requestId = receivedMessage.getId();
                    RideRequestOuterClass.RideRequest.Position startingP = receivedMessage.getStartingPosition();
                    RideRequestOuterClass.RideRequest.Position destinationP = receivedMessage.getDestinationPosition();
                    // coordinate
                    double distance = computeDistance(taxi.getCurrentP(), startingP); // to be sent to admin
                    // if won, handle request (sleep) and move
                    double totalDistance = distance;
                    System.out.println("Taxi " + taxi.getId() + " located at "+ taxi.getCurrentP().getX() + ", " + taxi.getCurrentP().getY() +
                            " accepted request " + requestId + " from " + startingP.getX() + ", " + startingP.getY()
                            + " to " + destinationP.getX() + ", " + destinationP.getY());
                    System.out.println(clientId + " Unsubscribing ... - Thread PID: " + Thread.currentThread().getId());
                    client.unsubscribe(taxi.getTopicString()+ taxi.getDistrict());
                    Thread.sleep(5000);
                    taxi.setCurrentP(startingP); // remains in the same district
                    taxi.setBattery(taxi.getBattery() - distance); // consume battery, based on distance
                    System.out.println("Customer reached. Distance travelled so far: " + totalDistance);
                    System.out.println("Battery left: " + taxi.getBattery());
                    System.out.println("Current position: " + taxi.getCurrentP().getX() + ", " + taxi.getCurrentP().getY());
                    Thread.sleep(5000);
                    distance = computeDistance(taxi.getCurrentP(), destinationP); // to be sent to admin
                    totalDistance += distance;
                    taxi.setCurrentP(destinationP);
                    taxi.setBattery(taxi.getBattery() - distance);
                    System.out.println("Request fulfilled. Total distance travelled: " + totalDistance);
                    System.out.println("Battery left: " + taxi.getBattery());
                    System.out.println("Current position: " + taxi.getCurrentP().getX() + ", " + taxi.getCurrentP().getY());
                    taxi.setDistrict(computeDistrict(destinationP));
                    System.out.println(clientId + " Subscribed ... - Thread PID: " + Thread.currentThread().getId());
                    client.subscribe(taxi.getTopicString()+taxi.getDistrict());
                    System.out.println(clientId + " Subscribed to topic : " + taxi.getTopicString()+taxi.getDistrict());
                    System.out.println("=====================================");
                }

                public void connectionLost(Throwable cause) {
                    // maybe quit?
                    System.out.println(clientId + " Connectionlost! cause:" + cause.getMessage()+ "-  Thread PID: " + Thread.currentThread().getId());
                }

                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Not used here
                }

            });

            System.out.println(clientId + " Subscribing ... - Thread PID: " + Thread.currentThread().getId());
            client.subscribe(taxi.getTopicString()+taxi.getDistrict(),qos);
            System.out.println(clientId + " Subscribed to topic : " + taxi.getTopicString()+taxi.getDistrict());

        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    private static double computeDistance(RideRequestOuterClass.RideRequest.Position p1, RideRequestOuterClass.RideRequest.Position p2){
        return Math.sqrt(Math.pow(p1.getX()-p2.getX(),2) +
                Math.pow(p1.getY()-p2.getY(),2));
    }

    private static String computeDistrict(RideRequestOuterClass.RideRequest.Position p){
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

        return "district_" + distN;
    }

}

