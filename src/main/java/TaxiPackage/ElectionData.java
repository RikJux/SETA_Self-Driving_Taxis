package TaxiPackage;

import seta.smartcity.rideRequest.RideRequestOuterClass;
import taxi.communication.handleRideService.HandleRideServiceOuterClass.*;

import java.util.HashMap;

import static Utils.Utils.*;

public class ElectionData {

    private HashMap<String, ElectionMsg> elections;
    private Taxi thisTaxi;

    public ElectionData(Taxi thisTaxi){
        this.elections = new HashMap<String, ElectionMsg>();
        this.thisTaxi = thisTaxi;
    }

    public synchronized boolean markParticipant(RideRequestOuterClass.RideRequest request){
        if(!this.elections.containsKey(request.getId())){
            ElectionMsg myElectionMsg = ElectionMsg.newBuilder()
                    .setRequest(translateRideRequest(request))
                    .setCandidateMsg(createCandidateMsg(request))
                    .build();
            this.elections.put(request.getId(), myElectionMsg);
            return false;
        }
        return true; // the taxi was already participant
    }

    public synchronized ElectionMsg computeElectionMsg(ElectionMsg otherElectionMsg){

        boolean alreadyParticipant = markParticipant(translateRideRequest(otherElectionMsg.getRequest()));
        String requestId = otherElectionMsg.getRequest().getId();

        ElectionMsg myElectionMsg = this.elections.get(requestId);
        ElectionIdentifier myElectionId = new ElectionIdentifier(myElectionMsg.getCandidateMsg());
        ElectionIdentifier otherElectionId = new ElectionIdentifier(otherElectionMsg.getCandidateMsg());

        int comparison = myElectionId.compareTo(otherElectionId);
        if(comparison > 0){
            return myElectionMsg;
        }else if(comparison < 0){
            if(!alreadyParticipant){
                return otherElectionMsg;
            }else{
                return null;
            }
        }else{ // received its own ELECTION
            markNonParticipant(requestId);
            return null;
        }
    }

    private void markNonParticipant(String requestId){

        translateRideRequest(this.elections.remove(requestId).getRequest());
    }

    public synchronized void markNonParticipant(ElectedMsg electedMsg){
        if(this.elections.containsKey(electedMsg.getRequest().getId())){
            markNonParticipant(electedMsg.getRequest().getId());
        }
    }

    private ElectionMsg.CandidateMsg createCandidateMsg(RideRequestOuterClass.RideRequest request){
        double distance = computeDistance(thisTaxi.getCurrentP(), fromMsgToArray(request.getStartingPosition()));
        return ElectionMsg.CandidateMsg.newBuilder()
                .setIdle(thisTaxi.getCurrentStatus() == Taxi.Status.IDLE)
                .setDistance(distance)
                .setBatteryLevel(thisTaxi.getBattery())
                .setId(thisTaxi.getId())
                .build();
    }

    private RideRequest translateRideRequest(RideRequestOuterClass.RideRequest request){
        return RideRequest.newBuilder()
                .setId(request.getId())
                .setStartingPosition(translatePosition(request.getStartingPosition()))
                .setDestinationPosition(translatePosition(request.getDestinationPosition()))
                .build();
    }

    private RideRequestOuterClass.RideRequest translateRideRequest(RideRequest request){
        return RideRequestOuterClass.RideRequest .newBuilder()
                .setId(request.getId())
                .setStartingPosition(translatePosition(request.getStartingPosition()))
                .setDestinationPosition(translatePosition(request.getDestinationPosition()))
                .build();
    }

    private RideRequest.Position translatePosition(RideRequestOuterClass.RideRequest.Position position){
        return RideRequest.Position.newBuilder()
                .setX(position.getX())
                .setY(position.getY())
                .build();
    }
    private RideRequestOuterClass.RideRequest.Position translatePosition(RideRequest.Position position){
        return RideRequestOuterClass.RideRequest.Position.newBuilder()
                .setX(position.getX())
                .setY(position.getY())
                .build();
    }

    public Taxi getThisTaxi() {
        return thisTaxi;
    }
}
