package TaxiPackage;

import beans.TaxiStatistics;
import com.google.protobuf.InvalidProtocolBufferException;
import org.eclipse.paho.client.mqttv3.*;
import seta.smartcity.rideRequest.RideRequestOuterClass;

public class TaxiDriver extends Thread{

    private final Taxi taxi;

    public TaxiDriver(Taxi taxi){
        this.taxi = taxi;
    }

    public void run(){
        System.out.println("Ciao sono il driver");
        MqttClient client;
        String broker = "tcp://localhost:1883";
        String clientId = MqttClient.generateClientId();
        int qos = 2; // why?
        taxi.setBattery(100.0);

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
                    RideRequestOuterClass.RideRequest.Position startingPMsg = receivedMessage.getStartingPosition();
                    RideRequestOuterClass.RideRequest.Position destinationPMsg = receivedMessage.getDestinationPosition();

                    int[] startingP = fromMsgToArray(startingPMsg);
                    int[] destinationP = fromMsgToArray(destinationPMsg);

                    // coordinate
                    System.out.println("Taxi " + taxi.getId() + " located at "+ taxi.getX() + ", " + taxi.getY() +
                            " accepted request " + requestId + " from " + startingP[0] + ", " + startingP[1]
                            + " to " + destinationP[0] + ", " + destinationP[1]);
                    taxi.setIdle(false);
                    System.out.println(clientId + " Unsubscribing ... - Thread PID: " + Thread.currentThread().getId());
                    client.unsubscribe(taxi.getTopicString()+ taxi.getDistrict());
                    travel(taxi.getCurrentP(), startingP, requestId, taxi, false, 2500); // reach the user
                    travel(startingP, destinationP, requestId, taxi, true, 2500); // reach the final destination
                    if(taxi.getTaxiStats().getBatteryLevel() > 30.0) {
                        System.out.println(clientId + " Subscribed ... - Thread PID: " + Thread.currentThread().getId());
                        client.subscribe(taxi.getTopicString() + taxi.getDistrict());
                        System.out.println(clientId + " Subscribed to topic : " + taxi.getTopicString() + taxi.getDistrict());
                        System.out.println("=====================================");
                        taxi.setIdle(true);
                    } else {
                        System.out.println("Taxi " + taxi.getId() + " needs recharge.");
                        destinationP = computeRechargeStation(taxi.getDistrict());
                        travel(taxi.getCurrentP(), destinationP, null, taxi, false, 5);
                        // mutual exclusion over recharge station
                        Thread.sleep(10000);
                        taxi.setBattery(100.0);
                        taxi.setIdle(true);
                    }

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

    private static void travel(int[] startingP, int[] destinationP, String requestId, Taxi taxi, boolean accomplished, int time) throws InterruptedException {

        Thread.sleep(time * 1000); // half because you need to reach the user
        double distance = computeDistance(startingP, destinationP);
        taxi.setCurrentP(destinationP);
        taxi.lowerBattery(distance);
        taxi.addKilometers(distance);
        taxi.setDistrict(computeDistrict(destinationP));
        if(accomplished){
            taxi.addRideAccomplished();
            System.out.println("Taxi " + taxi.getId() + " fulfilled request " + requestId);
        }
        System.out.println(taxi.getTaxiStats().toString());

    }

    private static int[] computeRechargeStation(String district){
        switch(district){
            case("district_1"):
                return new int[]{0, 0};
            case("district_2"):
                return new int[]{9, 0};
            case("district_3"):
                return new int[]{9, 9};
            default:
                return new int[]{0, 9};
        }
    }

    private static double computeDistance(int[] p1, int[] p2){
        return Math.sqrt(Math.pow(getCoordX(p1)-getCoordX(p2),2) +
                Math.pow(getCoordY(p1)-getCoordY(p2),2));
    }

    private static String computeDistrict(int[] p){
        int x = getCoordX(p);
        int y = getCoordY(p);
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

    private static int getCoordX(int[] p){ return p[0];}

    private static int getCoordY(int[] p){ return p[1];}

    private static int[] fromMsgToArray(RideRequestOuterClass.RideRequest.Position pMsg){

        int[] p = new int[2];

        p[0] = pMsg.getX();
        p[1] = pMsg.getY();
        return p;

    }

}

