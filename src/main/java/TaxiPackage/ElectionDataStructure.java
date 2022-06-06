package TaxiPackage;

import seta.smartcity.rideRequest.RideRequestOuterClass;
import taxi.communication.handleRideService.HandleRideServiceOuterClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static Utils.Utils.*;

public class ElectionDataStructure {
    private HashMap<String, ElectionInfo> elections;
    private List<RideRequestOuterClass.RideRequest> elected;
    private Taxi thisTaxi;
    private Object electedLock = new Object();

    public enum ToSend{
        SELF_ELECTION,
        OTHER_ELECTION,
        NOTHING,
        ELECTED
    }

    public ElectionDataStructure(Taxi thisTaxi){
        this.elections = new HashMap<String, ElectionInfo>();
        this.elected = new ArrayList<RideRequestOuterClass.RideRequest>();
        this.thisTaxi = thisTaxi;
    }

    public List<HandleRideServiceOuterClass.ElectionMsg> getElected(){
        List<HandleRideServiceOuterClass.ElectionMsg> electionMsgs = new ArrayList<HandleRideServiceOuterClass.ElectionMsg>();
        List<RideRequestOuterClass.RideRequest> electedCopy;
        synchronized (electedLock){
            electedCopy = new ArrayList<RideRequestOuterClass.RideRequest>(this.elected);
            this.elected = new ArrayList<RideRequestOuterClass.RideRequest>();
            electedLock.notifyAll();
        }
        for(RideRequestOuterClass.RideRequest r: electedCopy){
            updateElectionInfo(r.getId());
            HandleRideServiceOuterClass.ElectionMsg electionMsg = computeElectionMsg(r.getId());
            markParticipant(electionMsg);
            electionMsgs.add(electionMsg);

        }
        return electionMsgs;
    }

    public RideRequestOuterClass.RideRequest getRequestToHandle(){
        RideRequestOuterClass.RideRequest r;
        synchronized (electedLock){
            while(this.elected.isEmpty()){
                try {
                    electedLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            r = this.elected.remove(0);
            electedLock.notifyAll();
        }
        return r;
    }

    public ToSend decideWhatToSend(RideRequestOuterClass.RideRequest rideRequest){
        if(isParticipant(rideRequest.getId())){
            return ToSend.NOTHING;
        }else{
            markParticipant(rideRequest);
            return ToSend.SELF_ELECTION;
        }
    }

    public ToSend decideWhatToSend(HandleRideServiceOuterClass.ElectionMsg otherElectionMsg){
        String requestId = otherElectionMsg.getRequest().getId();
        if(isParticipant(requestId)){
            // this taxi was already participant
            ElectionIdentifier otherElectionId = new ElectionIdentifier(otherElectionMsg.getCandidateMsg());
            ElectionIdentifier thisElectionId = this.elections.get(requestId).getElectionIdentifier();
            int idComparison = thisElectionId.compareTo(otherElectionId);
            if(idComparison < 0){
                return ToSend.OTHER_ELECTION; // already participant but received a better candidate
            }else if(idComparison > 0){
                return ToSend.NOTHING; // already participant and received a worse candidate
            }else{ // idComparison == 0
                putInElected(requestId);
                return ToSend.ELECTED;
            }
        }else{
            markParticipant(otherElectionMsg);
            // this taxi was not already participant
            ElectionIdentifier otherElectionId = new ElectionIdentifier(otherElectionMsg.getCandidateMsg());
            ElectionIdentifier thisElectionId = this.elections.get(requestId).getElectionIdentifier();
            if(thisElectionId.compareTo(otherElectionId) < 0){
                return ToSend.OTHER_ELECTION; // received a better candidate
            }else{
                return ToSend.SELF_ELECTION; // received a worse candidate
            }
        }
    }

    public void markParticipant(RideRequestOuterClass.RideRequest rideRequest){
        String requestId = rideRequest.getId();
        if(!isParticipant(requestId)){
            double dist = computeDistance(fromMsgToArray(rideRequest.getStartingPosition()), thisTaxi.getCurrentP());
            ElectionInfo e = new ElectionInfo(rideRequest,
                    new ElectionIdentifier(thisTaxi, dist),
                    true);
            insertElection(e);
        }else{
            ElectionInfo e = this.elections.get(requestId);
            e.setParticipant(true);
        }
    }

    public void markParticipant(HandleRideServiceOuterClass.ElectionMsg electionMsg){
        String requestId = electionMsg.getRequest().getId();
        if(!isParticipant(requestId)){
            double dist = computeDistance(fromMsgToArray(electionMsg.getRequest().getStartingPosition()), thisTaxi.getCurrentP());
            ElectionInfo e = new ElectionInfo(translateRideRequest(electionMsg.getRequest()),
                    new ElectionIdentifier(thisTaxi, dist),
                    true);
            insertElection(e);
        }else{
            ElectionInfo e = this.elections.get(requestId);
            e.setParticipant(true);
        }
    }

    public void putInElected(String requestId){
        ElectionInfo e = this.elections.get(requestId);
        synchronized (electedLock){
            this.elected.add(e.getRideRequest());
        }
        markNonParticipant(requestId);
    }

    public void markNonParticipant(String requestId){
        ElectionInfo e = this.elections.get(requestId);
        e.setParticipant(false);
        this.elections.replace(requestId, e);

    }

    public void markNonParticipant(HandleRideServiceOuterClass.ElectedMsg electedMsg){
        // someone else was elected or this taxi.
        // if this taxi is elected, keep the record for later
        String requestId = electedMsg.getRequest().getId();
        if(!electedMsg.getTaxiId().equals(this.thisTaxi.getId())){ // this taxi was not elected
            this.elections.remove(requestId);
        }else{
            ElectionInfo e = this.elections.get(requestId);
            e.setParticipant(false);
            this.elections.replace(requestId, e);
        }
    }

    public void updateElectionInfo(String requestId){

        ElectionInfo e = this.elections.get(requestId);
        this.elections.replace(requestId, new ElectionInfo(e.getRideRequest(),
                new ElectionIdentifier(thisTaxi, computeDistance(fromMsgToArray(e.getRideRequest().getStartingPosition()),
                        thisTaxi.getCurrentP())), true));

    }

    public HandleRideServiceOuterClass.ElectionMsg computeElectionMsg(String requestId){

        ElectionInfo e = this.elections.get(requestId);
        HandleRideServiceOuterClass.ElectionMsg myElectionMsg = HandleRideServiceOuterClass.ElectionMsg.newBuilder()
                .setRequest(translateRideRequest(e.getRideRequest()))
                .setCandidateMsg(e.getElectionIdentifier().toMsg()) // e.getElectionIdentifier().toMsg()
                .build();
        return myElectionMsg;
    }

    private void insertElection(ElectionInfo e){
        if(!this.elections.containsKey(e.getRideRequest().getId())){
            this.elections.put(e.getRideRequest().getId(), e);
        }
        else{
            this.elections.replace(e.getRideRequest().getId(), e);
        }
    }

    private boolean isParticipant(String requestId){
        return this.elections.containsKey(requestId) && this.elections.get(requestId).isParticipant();
    }

    public Taxi getThisTaxi() {
        return thisTaxi;
    }
}
