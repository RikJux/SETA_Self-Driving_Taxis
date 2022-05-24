package TaxiPackage;

import beans.TaxiStatistics;
import com.google.protobuf.InvalidProtocolBufferException;
import org.eclipse.paho.client.mqttv3.*;
import seta.smartcity.rideRequest.RideRequestOuterClass;

public class TaxiDriver extends Thread {

    private final Taxi thisTaxi;

    public TaxiDriver(Taxi taxi) {
        this.thisTaxi = taxi;
    }

    public void run() {
        MqttClient client;
        String broker = "tcp://localhost:1883";
        String clientId = MqttClient.generateClientId();
        int qos = 2; // why?
        thisTaxi.setBattery(100.0);

        try {
            client = new MqttClient(broker, clientId);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);

            System.out.println(clientId + "[TAXI DRIVER] Connecting Broker " + broker);
            client.connect(connOpts);
            System.out.println(clientId + "[TAXI DRIVER] Connected - Thread PID: " + Thread.currentThread().getId());

            client.setCallback(new MqttCallback() {

                public void messageArrived(String topic, MqttMessage message) throws MqttException, InvalidProtocolBufferException, InterruptedException {
                    // Called when a message arrives from the server that matches any subscription made by the client;
                    // should coordinate with other taxis to decide who handles the request
                    // receive request
                    RideRequestOuterClass.RideRequest receivedMessage = RideRequestOuterClass.RideRequest.parseFrom(message.getPayload());
                    String requestId = receivedMessage.getId();
                    RideRequestOuterClass.RideRequest.Position startingPMsg = receivedMessage.getStartingPosition();
                    RideRequestOuterClass.RideRequest.Position destinationPMsg = receivedMessage.getDestinationPosition();

                    System.out.println("[TAXI DRIVER] A message arrived!");

                    synchronized (thisTaxi) { // cannot get this lock
                        if(thisTaxi.getCurrentStatus() != Taxi.Status.IDLE){
                            System.out.println("[TAXI DRIVER] Taxi currently unavailable");
                            thisTaxi.wait();
                        }
                        if (thisTaxi.getCurrentStatus() == Taxi.Status.IDLE) {
                            int[] startingP = fromMsgToArray(startingPMsg);
                            int[] destinationP = fromMsgToArray(destinationPMsg);

                            // coordinate first!
                            System.out.println("[TAXI DRIVER] Taxi " + thisTaxi.getId() + " located at " + thisTaxi.getX() + ", " + thisTaxi.getY() +
                                    " accepted request " + requestId + " from " + startingP[0] + ", " + startingP[1]
                                    + " to " + destinationP[0] + ", " + destinationP[1]);
                            thisTaxi.setCurrentStatus(Taxi.Status.WORKING);
                            System.out.println("[TAXI DRIVER] " + clientId + " Unsubscribing ... - Thread PID: " + Thread.currentThread().getId());
                            client.unsubscribe(thisTaxi.getTopicString() + thisTaxi.getDistrict());
                            travel(thisTaxi.getCurrentP(), startingP, requestId, thisTaxi, false, 2.5f); // reach the user
                            travel(startingP, destinationP, requestId, thisTaxi, true, 2.5f); // reach the final destination
                            if (thisTaxi.getTaxiStats().getBatteryLevel() <= 30.0) {
                                System.out.println("[TAXI DRIVER] Taxi " + thisTaxi.getId() + " needs recharge.");
                                thisTaxi.setCurrentStatus(Taxi.Status.REQUEST_RECHARGE);
                                thisTaxi.setRechargeRequestTimestamp(System.currentTimeMillis());
                                // mutual exclusion
                                TaxiRechargeComm r = new TaxiRechargeComm(thisTaxi);
                                r.start();
                                //r.join(); // wait for all responses
                                synchronized (thisTaxi) {
                                    while (thisTaxi.getCurrentStatus() == Taxi.Status.GO_RECHARGE) {
                                        thisTaxi.wait();
                                        if (thisTaxi.getCurrentStatus() == Taxi.Status.IDLE) {
                                            System.out.println("[TAXI DRIVER] Recharge done.");
                                            thisTaxi.notifyAll();
                                        }
                                    }
                                }
                                thisTaxi.setCurrentStatus(Taxi.Status.GO_RECHARGE);
                                recharge(thisTaxi);
                                thisTaxi.setRechargeRequestTimestamp(Double.MAX_VALUE);
                                thisTaxi.setCurrentStatus(Taxi.Status.IDLE);
                            }
                            System.out.println("[TAXI DRIVER] " + clientId + " Subscribed ... - Thread PID: " + Thread.currentThread().getId());
                            client.subscribe(thisTaxi.getTopicString() + thisTaxi.getDistrict());
                            System.out.println("[TAXI DRIVER] " + clientId + " Subscribed to topic : " + thisTaxi.getTopicString() + thisTaxi.getDistrict());
                            System.out.println("=====================================");
                            thisTaxi.setCurrentStatus(Taxi.Status.IDLE);
                            thisTaxi.notifyAll();
                        }
                    }
                    // if no thread is waiting for the lock over thisTaxi

                }

                public void connectionLost(Throwable cause) {
                    // maybe quit?
                    System.out.println("[TAXI DRIVER] " + clientId + " Connectionlost! cause:" + cause.getMessage() + "-  Thread PID: " + Thread.currentThread().getId());
                }

                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Not used here
                }

            });

            System.out.println("[TAXI DRIVER] " + clientId + " Subscribing ... - Thread PID: " + Thread.currentThread().getId());
            client.subscribe(thisTaxi.getTopicString() + thisTaxi.getDistrict(), qos);
            System.out.println("[TAXI DRIVER] " + clientId + " Subscribed to topic : " + thisTaxi.getTopicString() + thisTaxi.getDistrict());

        } catch (MqttException e) {
            e.printStackTrace();
        }

        synchronized (thisTaxi) {
            while (thisTaxi.getCurrentStatus() != Taxi.Status.GO_RECHARGE) {
                try {
                    System.out.println("[TAXI DRIVER] Taxi waiting for GO_RECHARGE");
                    System.out.println(thisTaxi.getCurrentStatus());
                    thisTaxi.wait();
                    if (thisTaxi.getCurrentStatus() == Taxi.Status.GO_RECHARGE) {
                        System.out.println("[TAXI DRIVER] Taxi going to recharge");
                        recharge(thisTaxi);
                        thisTaxi.notifyAll();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void recharge(Taxi thisTaxi) throws InterruptedException {
        synchronized (thisTaxi) {
            int[] destinationP = computeRechargeStation(thisTaxi.getDistrict());
            travel(thisTaxi.getCurrentP(), destinationP, null, thisTaxi, false, 5);
            System.out.println("[TAXI DRIVER] Reached the recharge station");
            // mutual exclusion over recharge station
            Thread.sleep(10000);
            thisTaxi.setBattery(100.0);
            thisTaxi.zeroRechargeReqCounter();
            thisTaxi.setCurrentStatus(Taxi.Status.IDLE);
            System.out.println("[TAXI DRIVER] Taxi " + thisTaxi.getId() + " is now fully recharged.");
            thisTaxi.notifyAll();
        }
    }

    private static void travel(int[] startingP, int[] destinationP, String requestId, Taxi taxi, boolean accomplished, float time) throws InterruptedException {

        Thread.sleep((int) time * 1000); // half because you need to reach the user
        double distance = computeDistance(startingP, destinationP);
        taxi.setCurrentP(destinationP);
        taxi.lowerBattery(distance);
        taxi.addKilometers(distance);
        taxi.setDistrict(computeDistrict(destinationP));
        if (accomplished) {
            taxi.addRideAccomplished();
            System.out.println("[TAXI DRIVER] Taxi " + taxi.getId() + " fulfilled request " + requestId);
        }
    }

    private static int[] computeRechargeStation(String district) {
        switch (district) {
            case ("district_1"):
                return new int[]{0, 0};
            case ("district_2"):
                return new int[]{9, 0};
            case ("district_3"):
                return new int[]{9, 9};
            default:
                return new int[]{0, 9};
        }
    }

    private static double computeDistance(int[] p1, int[] p2) {
        return Math.sqrt(Math.pow(getCoordX(p1) - getCoordX(p2), 2) +
                Math.pow(getCoordY(p1) - getCoordY(p2), 2));
    }

    private static String computeDistrict(int[] p) {
        int x = getCoordX(p);
        int y = getCoordY(p);
        String distN;

        if (y < 5) {
            // we are in the upper city
            if (x < 5) {
                distN = "1";
            } else {
                distN = "2";
            }
        } else {
            // we are in the lower city
            if (x < 5) {
                distN = "4";
            } else {
                distN = "3";
            }
        }

        return "district" + distN;
    }

    private static int getCoordX(int[] p) {
        return p[0];
    }

    private static int getCoordY(int[] p) {
        return p[1];
    }

    private static int[] fromMsgToArray(RideRequestOuterClass.RideRequest.Position pMsg) {

        int[] p = new int[2];

        p[0] = pMsg.getX();
        p[1] = pMsg.getY();
        return p;

    }

}

