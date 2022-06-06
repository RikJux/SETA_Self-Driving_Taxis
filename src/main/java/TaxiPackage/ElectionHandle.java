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

    public HandleRideServiceOuterClass.ElectedMsg receiveElectedMsg(HandleRideServiceOuterClass.ElectedMsg electedMsg){


        String requestId = electedMsg.getRequest().getId();

        markNonParticipant(electedMsg);

        if(!electedMsg.getTaxiId().equals(thisTaxi.getId())){ // this taxi was not elected
            return electedMsg;
        }

        return null;

    }

    public HandleRideServiceOuterClass.ElectionMsg receiveElectionMsg(HandleRideServiceOuterClass.ElectionMsg otherElectionMsg){

        String requestId = otherElectionMsg.getRequest().getId();
        boolean alreadyParticipant = markParticipant(otherElectionMsg);

        ElectionIdentifier thisElectionId = this.elections.get(requestId).getElectionIdentifier();
        ElectionIdentifier otherElectionId = new ElectionIdentifier(otherElectionMsg.getCandidateMsg());

        int comparison = otherElectionId.compareTo(thisElectionId);

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

    public HandleRideServiceOuterClass.ElectionMsg receiveRideRequest(RideRequestOuterClass.RideRequest rideRequest){

        String requestId = rideRequest.getId();

        if(!markParticipant(rideRequest)){
            return HandleRideServiceOuterClass.ElectionMsg.newBuilder()
                    .setRequest(translateRideRequest(rideRequest))
                    .setCandidateMsg(this.elections.get(requestId).getElectionIdentifier().toMsg())
                    .build();
        }
        // this taxi was already participant, so it already sent its own candidature
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
            System.out.println(printInformation("TAXI", thisTaxi.getId()) +
                    "marked PARTICIPANT in election for" +
                    printInformation("REQUEST", requestId));
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
            if(isParticipant(requestId)){
                this.elections.remove(requestId);
                System.out.println(printInformation("TAXI", thisTaxi.getId()) +
                        "marked NON-PARTICIPANT in election for" +
                        printInformation("REQUEST", requestId));
            }
        }

    }

    private void markNonParticipant(HandleRideServiceOuterClass.ElectedMsg electedMsg){

        String requestId = electedMsg.getRequest().getId();
        if(isParticipant(requestId)){
            this.elections.remove(requestId);
            System.out.println(printInformation("TAXI", thisTaxi.getId()) +
                    "marked NON-PARTICIPANT in election for" +
                    printInformation("REQUEST", requestId));
        }

    }

    private boolean isParticipant(String requestId){
        return this.elections.containsKey(requestId);
    }

    public Taxi getThisTaxi() {
        return thisTaxi;
    }
}
