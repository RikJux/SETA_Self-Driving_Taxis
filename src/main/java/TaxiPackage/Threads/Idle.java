package TaxiPackage.Threads;

import TaxiPackage.ElectedThread;
import TaxiPackage.Taxi;
import beans.TaxiBean;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.eclipse.paho.client.mqttv3.*;
import seta.smartcity.rideRequest.RideRequestOuterClass;
import taxi.communication.handleRideService.HandleRideServiceGrpc;
import taxi.communication.handleRideService.HandleRideServiceOuterClass;

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
            subscribe(client);
            synchronized (thisTaxi.getRechargeTimestampLock()){
                thisTaxi.setRechargeRequestTimestamp(Double.MAX_VALUE); // GO_RECHARGE
            }

            client.publish(availableTopic+thisTaxi.getDistrict(), new MqttMessage("".getBytes()));
            System.out.println("Made taxi available at " + availableTopic+thisTaxi.getDistrict());

            new Thread(() -> {
                for(HandleRideServiceOuterClass.ElectionMsg electionMsg: thisTaxi.getElectionData().getElected()) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    forwardMessage(thisTaxi, electionMsg);
                }
            }).start();

            synchronized (inputLock){
                while(thisTaxi.getCurrentStatus() == Taxi.Status.IDLE){
                    if(thisTaxi.getInput() == null){
                        System.out.println("Waiting for input.");
                        inputLock.wait();
                        if(thisTaxi.getInput() == null){
                            thisTaxi.setInput(Taxi.Input.WORK);
                            thisTaxi.setReqToHandle(thisTaxi.getElectionData().getRequestToHandle());
                            System.out.println("To handle " + thisTaxi.getReqToHandle());
                        }
                    }
                    if(thisTaxi.getInput() != null){
                        System.out.println("Arrived [INPUT " + thisTaxi.getInput() + "]");
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
        System.out.println(printInformation("REQUEST", payload.getId()) + "will handled at " + handledTopic+destDist);
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
                System.out.println("Arrived" + printInformation("REQUEST", receivedMessage.getId()));
                //TODO start the election
                switch (thisTaxi.getElectionData().decideWhatToSend(receivedMessage)){
                    case NOTHING:
                        break;
                    case SELF_ELECTION:
                        thisTaxi.getElectionData().markParticipant(receivedMessage);
                        HandleRideServiceOuterClass.ElectionMsg myElectionMsg = thisTaxi.getElectionData()
                                .computeElectionMsg(receivedMessage.getId());
                        forwardMessage(thisTaxi, myElectionMsg);
                        break;
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

    private static ManagedChannel createChannel(Taxi thisTaxi){

        synchronized (thisTaxi.getNextLock()) {
            TaxiBean t = thisTaxi.getNextTaxi();
            ManagedChannel channel = ManagedChannelBuilder.forTarget(t.getIp() + ":" + t.getPort()).usePlaintext().build();
            thisTaxi.getNextLock().notifyAll();
            return channel;
        }

    }


    private static void forwardMessage(Taxi thisTaxi, HandleRideServiceOuterClass.ElectionMsg electionMsg){

        final ManagedChannel channel = createChannel(thisTaxi);

        HandleRideServiceGrpc.HandleRideServiceStub stub = HandleRideServiceGrpc.newStub(channel);

        stub.election(electionMsg, new StreamObserver<HandleRideServiceOuterClass.ElectionOk>() {
            @Override
            public void onNext(HandleRideServiceOuterClass.ElectionOk value) {
                //System.out.println("Sent"+ printInformation("ELECTION", electionMsg.getRequest().getId()));
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                channel.shutdownNow();
            }
        });

    }
}
