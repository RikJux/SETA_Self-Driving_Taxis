package TaxiPackage;

import taxi.communication.rechargeTokenService.RechargeTokenServiceOuterClass;

import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

public class TokenQueue {

    private Queue<RechargeTokenServiceOuterClass.RechargeToken> tokens = new LinkedList<RechargeTokenServiceOuterClass.RechargeToken>();
    private static Object rechargeLock = new Object();

    public TokenQueue(List<RechargeTokenServiceOuterClass.RechargeToken> q){
        for(RechargeTokenServiceOuterClass.RechargeToken r: q){
            this.tokens.add(r);
        }
    }

    public void add(RechargeTokenServiceOuterClass.RechargeToken token){
        synchronized (rechargeLock){
            this.tokens.add(token);
            rechargeLock.notifyAll();
        }
    }

    public RechargeTokenServiceOuterClass.RechargeToken remove(){
        synchronized (rechargeLock){
            while(this.tokens.isEmpty()){
                try {
                    rechargeLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            rechargeLock.notifyAll();
            return this.tokens.remove();
        }
    }

    @Override
    public String toString() {
        return tokens.toString();
    }
}
