package TaxiPackage;

import taxi.communication.handleRideService.HandleRideServiceGrpc;
import taxi.communication.handleRideService.HandleRideServiceOuterClass;

import java.util.Stack;

public class ElectionIdentifier implements Comparable<ElectionIdentifier> {

    private Stack<Double> stack;

    public ElectionIdentifier(Taxi thisTaxi, double distance){ // called when sending msg
        stack = new Stack<Double>();

        stack.push(Double.parseDouble(thisTaxi.getId()));
        stack.push(thisTaxi.getBattery());
        stack.push(-1*distance);
        stack.push(thisTaxi.getCurrentStatus() == Taxi.Status.IDLE ? 1d : 0d);

    }

    public ElectionIdentifier(boolean isIdle, double distance, double battery, String id){ // called when received msg

        stack = new Stack<Double>();
        stack.push(Double.parseDouble(id));
        stack.push(battery);
        stack.push(-1*distance);
        stack.push(isIdle ? 1d : 0d);

    }

    public ElectionIdentifier(HandleRideServiceOuterClass.ElectionMsg.CandidateMsg msg){
        stack = new Stack<Double>();
        stack.push(Double.parseDouble(msg.getId()));
        stack.push(msg.getBatteryLevel());
        stack.push(-1* msg.getDistance());
        stack.push(msg.getIdle() ? 1d : 0d);

    }

    @Override
    public int compareTo(ElectionIdentifier otherId) {
        // compare in order: status, (the inverse of) distance, battery level and id
        Stack<Double> stackCopy = (Stack<Double>) this.stack.clone();
        Stack<Double> otherIdStackCopy = (Stack<Double>) otherId.stack.clone();
        do{
            double el1 = stackCopy.pop();
            double el2 = otherIdStackCopy.pop();
            if(el1 > el2){
                return 1;
            }else if(el1 < el2){
                return -1;
            }
        }while(!stackCopy.isEmpty());

        return 0; // the two are identical
    }

    public HandleRideServiceOuterClass.ElectionMsg.CandidateMsg toMsg(){

        Stack<Double> stackCopy = (Stack<Double>) this.stack.clone();

        return HandleRideServiceOuterClass.ElectionMsg.CandidateMsg
                .newBuilder()
                .setIdle(stackCopy.pop()==1.0d)
                .setDistance(-1*stackCopy.pop())
                .setBatteryLevel(stackCopy.pop())
                .setId(String.valueOf(stackCopy.pop()))
                .build();

    }

    @Override
    public String toString() {
        return "ElectionIdentifier{" +
                "stack=" + stack +
                '}';
    }
}
