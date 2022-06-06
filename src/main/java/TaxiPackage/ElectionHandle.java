package TaxiPackage;

import seta.smartcity.rideRequest.RideRequestOuterClass;
import taxi.communication.handleRideService.HandleRideServiceOuterClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static Utils.Utils.*;

public class ElectionHandle {

    private HashMap<String, ElectionInfo> elections;
    private Taxi thisTaxi;

    public ElectionHandle(Taxi thisTaxi){
        this.elections = new HashMap<String, ElectionInfo>();
        this.thisTaxi = thisTaxi;
    }

    public RideRequestOuterClass.RideRequest receiveElectedMsg(HandleRideServiceOuterClass.ElectedMsg electedMsg){


        String requestId = electedMsg.getRequest().getId();
        if(electedMsg.getTaxiId().equals(thisTaxi.getId())){
            markNonParticipant(electedMsg);
            return this.elections.get(requestId).getRideRequest();
        }

        return null;

    }

    public HandleRideServiceOuterClass.ElectionMsg receiveElectionMsg(HandleRideServiceOuterClass.ElectionMsg otherElectionMsg){

        String requestId = otherElectionMsg.getRequest().getId();

        ElectionIdentifier thisElectionId = this.elections.get(requestId).getElectionIdentifier();
        ElectionIdentifier otherElectionId = new ElectionIdentifier(otherElectionMsg.getCandidateMsg());

        int comparison = otherElectionId.compareTo(thisElectionId);
        boolean alreadyParticipant = markParticipant(otherElectionMsg);

        if(comparison > 0){ // receives a better candidate
            return otherElectionMsg;

        }else if(comparison < 0){ // receives a worse candidate
            if(!alreadyParticipant){
                return HandleRideServiceOuterClass.ElectionMsg.newBuilder()
                        .setRequest(otherElectionMsg.getRequest())
                        .setCandidateMsg(thisElectionId.toMsg())
                        .build();
            }
        }else{
            markNonParticipant(otherElectionMsg); // the taxi was elected
        }

        // receives a worse candidate in an election in which the taxi was already participant
        // or this taxi was elected
        return null;

    }

    private boolean markParticipant(RideRequestOuterClass.RideRequest rideRequest){

        String requestId = rideRequest.getId();
        boolean alreadyParticipant = true;

        if(!isParticipant(requestId)){
            double distance = computeDistance(thisTaxi.getCurrentP(), fromMsgToArray(rideRequest.getStartingPosition()));
            this.elections.put(requestId, new ElectionInfo(rideRequest,
                    new ElectionIdentifier(thisTaxi, distance),
                    true));
            alreadyParticipant = false;
        }

        return alreadyParticipant;

    }

    private boolean markParticipant(HandleRideServiceOuterClass.ElectionMsg electionMsg){

        return markParticipant(translateRideRequest(electionMsg.getRequest()));

    }

    private void markNonParticipant(HandleRideServiceOuterClass.ElectionMsg electionMsg){

        String requestId = electionMsg.getRequest().getId();

        // this taxi is elected!
        if(electionMsg.getCandidateMsg().getId().equals(thisTaxi.getId())){
            this.elections.remove(requestId);
        }

    }

    private void markNonParticipant(HandleRideServiceOuterClass.ElectedMsg electedMsg){

        String requestId = electedMsg.getRequest().getId();
        this.elections.remove(requestId);

    }

    private boolean isParticipant(String requestId){
        return this.elections.containsKey(requestId);
    }

}
