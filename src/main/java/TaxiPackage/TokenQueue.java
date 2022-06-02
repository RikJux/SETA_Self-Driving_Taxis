package TaxiPackage;

import taxi.communication.rechargeTokenService.RechargeTokenServiceOuterClass;

import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import static Utils.Utils.createRechargeToken;

public class TokenQueue {

    private Queue<RechargeTokenServiceOuterClass.RechargeToken> tokens = new LinkedList<RechargeTokenServiceOuterClass.RechargeToken>();
    private RechargeTokenServiceOuterClass.RechargeToken inUse;
    private static Object rechargeLock = new Object();
    private static Object inUseLock = new Object();

    public TokenQueue(List<RechargeTokenServiceOuterClass.RechargeToken> q){
        for(RechargeTokenServiceOuterClass.RechargeToken r: q){
            this.tokens.add(r);
        }
        inUse = createRechargeToken("NONE");
    }

    public void add(RechargeTokenServiceOuterClass.RechargeToken token){
        synchronized (rechargeLock){
            this.tokens.add(token);
            rechargeLock.notifyAll();
        }
    }

    public RechargeTokenServiceOuterClass.RechargeToken remove() {
        synchronized (rechargeLock) {
            while (this.tokens.isEmpty()) {
                try {
                    rechargeLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            RechargeTokenServiceOuterClass.RechargeToken removed = this.tokens.remove();
            rechargeLock.notifyAll();
            synchronized (inUseLock) {
                if (!removed.getDistrict().equals(this.inUse.getDistrict())) { // the head of the queue is not in use
                    inUseLock.notifyAll();
                    return removed;
                } else {
                    while (removed.getDistrict().equals(this.inUse.getDistrict())) {
                        try {
                            if (!this.isEmpty()) {
                                removed = this.tokens.remove();
                            }
                            if (!removed.getDistrict().equals(this.inUse.getDistrict())) { // the head of the queue is not in use
                                inUseLock.notifyAll();
                                return removed;
                            }
                            inUseLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (this.tokens.size() > 0) {
                        this.tokens.add(removed);
                        rechargeLock.notifyAll();
                        inUseLock.notifyAll();
                        return removed;
                    }
                }
            }
        }
        return null;
    }

    public boolean isEmpty(){
        synchronized (rechargeLock){
            return this.tokens.isEmpty();
        }
    }

    public boolean checkPresence(RechargeTokenServiceOuterClass.RechargeToken rechargeToken){
        synchronized (rechargeLock){
            return this.tokens.contains(rechargeToken);
        }
    }

    public void setInUse(RechargeTokenServiceOuterClass.RechargeToken rechargeToken){
        synchronized (inUseLock){
            while (!checkPresence(rechargeToken)) {
                try {
                    inUseLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if(checkPresence(rechargeToken)){ // the recharge token for the required recharging station
                if(!checkPresence(this.inUse) && !this.inUse.getDistrict().equals("NONE")){ // the token was used and has to be re-put to be sent away
                    this.tokens.add(this.inUse);
                }
                this.inUse = rechargeToken;
            }
            inUseLock.notifyAll();
        }
    }

    public RechargeTokenServiceOuterClass.RechargeToken getInUse() {
        synchronized (inUseLock){
            return inUse;
        }
    }

    @Override
    public String toString() {
        return tokens.toString();
    }
}
