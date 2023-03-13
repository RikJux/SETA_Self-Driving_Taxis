package TaxiPackage;

import seta.smartcity.rideRequest.RideRequestOuterClass;
import taxi.communication.handleRideService.HandleRideServiceOuterClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static Utils.Utils.*;

public class ElectionHandle {

    private HashMap<String, ElectionInfo> elections;
    private List<RideRequestOuterClass.RideRequest> elected;
    private List<RideRequestOuterClass.RideRequest> handled;
    private Taxi thisTaxi;
    private Object markLock = new Object();

    public ElectionHandle(Taxi thisTaxi){
        this.elections = new HashMap<String, ElectionInfo>();
        this.elected = new ArrayList<RideRequestOuterClass.RideRequest>();
        this.handled = new ArrayList<RideRequestOuterClass.RideRequest>();
        this.thisTaxi = thisTaxi;
    }

    public HandleRideServiceOuterClass.ElectedMsg receiveElectedMsg(HandleRideServiceOuterClass.ElectedMsg electedMsg){

        String requestId = electedMsg.getRequest().getId();

        markNonParticipant(electedMsg);

        if(isHandling(electedMsg.getRequest()) || alreadyElected(electedMsg.getRequest())){
            return null;
        }

        if(!electedMsg.getTaxiId().equals(thisTaxi.getId())){ // this taxi was not elected
            return electedMsg;
        }else{
            synchronized (thisTaxi.getElectedLock()){
                if(!isHandling(electedMsg.getRequest()) && !alreadyElected(electedMsg.getRequest())){
                    this.elected.add(translateRideRequest(electedMsg.getRequest()));
                    thisTaxi.getElectedLock().notifyAll();
                }
            }
        }

        return null;

    }

    public HandleRideServiceOuterClass.ElectionMsg receiveElectionMsg(HandleRideServiceOuterClass.ElectionMsg otherElectionMsg){

        String requestId = otherElectionMsg.getRequest().getId();

        if(isHandling(otherElectionMsg.getRequest()) || alreadyElected(otherElectionMsg.getRequest())){
            return null;
        }

        ElectionIdentifier thisElectionId;
        ElectionIdentifier otherElectionId;
        boolean alreadyParticipant;

        synchronized (markLock){
            alreadyParticipant = markParticipant(otherElectionMsg);
            thisElectionId = this.elections.get(requestId).getElectionIdentifier();
            otherElectionId = new ElectionIdentifier(otherElectionMsg.getCandidateMsg());
        }

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

        synchronized (markLock){
            if(!isParticipant(requestId)){
                double distance = computeDistance(thisTaxi.getCurrentP(), fromMsgToArray(rideRequest.getStartingPosition()));
                this.elections.put(requestId, new ElectionInfo(rideRequest,
                        new ElectionIdentifier(thisTaxi, distance), true));
                alreadyParticipant = false;
            }
        }

        return alreadyParticipant;

    }

    private boolean markParticipant(HandleRideServiceOuterClass.ElectionMsg electionMsg){

        return markParticipant(translateRideRequest(electionMsg.getRequest()));

    }

    private void markNonParticipant(HandleRideServiceOuterClass.ElectionMsg electionMsg){

        String requestId = electionMsg.getRequest().getId();
        synchronized (markLock) {
            if(electionMsg.getCandidateMsg().getId().equals(thisTaxi.getId())){
                if(isParticipant(requestId)){
                    this.elections.remove(requestId);
                }
            }
        }

    }

    private void markNonParticipant(HandleRideServiceOuterClass.ElectedMsg electedMsg){

        String requestId = electedMsg.getRequest().getId();
        synchronized (markLock){
            if(isParticipant(requestId)){
                this.elections.remove(requestId);
            }
        }

    }

    private boolean isParticipant(String requestId){
        return this.elections.containsKey(requestId);
    }

    public Taxi getThisTaxi() {
        return thisTaxi;
    }

    public RideRequestOuterClass.RideRequest getFirst(){
        return this.elected.remove(0);
    }

    public int getElectedSize(){
        int result = 0;
        synchronized (thisTaxi.getElectedLock()){
            result = this.elected.size();
        }
        return result;
    }

    public List<RideRequestOuterClass.RideRequest> getElected(){
        List<RideRequestOuterClass.RideRequest> toSend;
        synchronized (thisTaxi.getElectedLock()){
            toSend = this.elected;
            this.elected = new ArrayList<RideRequestOuterClass.RideRequest>();
        }
        return toSend;
    }

    private boolean isHandling(HandleRideServiceOuterClass.RideRequest rideRequest){
        synchronized (this.handled){
            return this.handled.contains(rideRequest);
        }
    }

    private boolean alreadyElected(HandleRideServiceOuterClass.RideRequest rideRequest){
        boolean alreadyElected = false;
        synchronized (thisTaxi.getElectedLock()){
            alreadyElected = this.elected.contains(translateRideRequest(rideRequest).getId());
            thisTaxi.getElectedLock().notifyAll();
        }
        return alreadyElected;
    }

    public void addHandled(RideRequestOuterClass.RideRequest rideRequest){
        synchronized (this.handled){
            this.handled.add(rideRequest);
        }
    }
}
