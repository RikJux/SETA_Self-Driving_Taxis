package TaxiPackage;

import seta.smartcity.rideRequest.RideRequestOuterClass;

public class ElectionInfo {

    private RideRequestOuterClass.RideRequest rideRequest;
    private ElectionIdentifier electionIdentifier;
    private boolean isParticipant;

    public ElectionInfo(RideRequestOuterClass.RideRequest rideRequest, ElectionIdentifier electionIdentifier, boolean isParticipant) {
        this.rideRequest = rideRequest;
        this.electionIdentifier = electionIdentifier;
        this.isParticipant = isParticipant;
    }

    public RideRequestOuterClass.RideRequest getRideRequest() {
        return rideRequest;
    }

    public ElectionIdentifier getElectionIdentifier() {
        return electionIdentifier;
    }

    public boolean isParticipant() {
        return isParticipant;
    }

    public void setParticipant(boolean participant) {
        isParticipant = participant;
    }
}
