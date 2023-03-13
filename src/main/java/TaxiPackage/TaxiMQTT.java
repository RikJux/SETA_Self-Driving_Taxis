package TaxiPackage;

import beans.TaxiBean;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.eclipse.paho.client.mqttv3.*;
import seta.smartcity.rideRequest.RideRequestOuterClass;
import taxi.communication.handleRideService.HandleRideServiceGrpc;
import taxi.communication.handleRideService.HandleRideServiceOuterClass;

import static Utils.Utils.*;
import static Utils.Utils.ridesTopic;

public class TaxiMQTT {

    private MqttClient client;
    private Taxi thisTaxi;
    private int qos = 1;

    public TaxiMQTT(Taxi thisTaxi) throws MqttException {
        this.thisTaxi = thisTaxi;
        this.client = initiateMqttClient(thisTaxi);
        this.client.connect();
    }

    private MqttClient initiateMqttClient(Taxi thisTaxi) throws MqttException {
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

                HandleRideServiceOuterClass.ElectionMsg myElectionMsg = thisTaxi.getElectionHandle().receiveRideRequest(receivedMessage);
                if(myElectionMsg != null){
                    forwardMessage(thisTaxi, myElectionMsg);
                }

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        return mqttClient;
    }

    public void subscribe() throws MqttException {
        client.subscribe(thisTaxi.getTopicString() + thisTaxi.getDistrict(), qos);
        System.out.println("Subscribed to " + ridesTopic + thisTaxi.getDistrict());
    }

    public void unsubscribe() throws MqttException {
        client.unsubscribe(thisTaxi.getTopicString() + thisTaxi.getDistrict());
        System.out.println("Unsubscribed from " + ridesTopic + thisTaxi.getDistrict());
    }

    public void publishToHandleRequest(RideRequestOuterClass.RideRequest requestToHandle) throws MqttException{
        RideRequestOuterClass.RideRequest payload = requestToHandle;
        MqttMessage message = new MqttMessage(payload.toByteArray());
        message.setQos(1);
        client.publish(handledTopic+computeDistrict(requestToHandle), message);
        System.out.println(printInformation("REQUEST", payload.getId()) + "will handled at " +
                handledTopic+computeDistrict(requestToHandle));
    }

    public void publishAvailability() throws MqttException {
        client.publish(availableTopic+thisTaxi.getDistrict(), new MqttMessage("".getBytes()));
        System.out.println("Made taxi available at " + availableTopic+thisTaxi.getDistrict());
    }

    private static ManagedChannel createChannel(Taxi thisTaxi){

        synchronized (thisTaxi.getNextLock()) {
            TaxiBean t = thisTaxi.getNextTaxi();
            ManagedChannel channel = ManagedChannelBuilder.forTarget(t.getIp() + ":" + t.getPort()).usePlaintext().build();
            thisTaxi.getNextLock().notifyAll();
            return channel;
        }

    }


    public static void forwardMessage(Taxi thisTaxi, HandleRideServiceOuterClass.ElectionMsg electionMsg){

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
