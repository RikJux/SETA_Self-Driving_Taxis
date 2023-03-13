package TaxiPackage;

import taxi.communication.rechargeTokenService.RechargeTokenServiceOuterClass;

import java.util.*;

import static Utils.Utils.createRechargeToken;

public class TokenQueue {

    private List<RechargeTokenServiceOuterClass.RechargeToken> tokens = new ArrayList<RechargeTokenServiceOuterClass.RechargeToken>();
    private RechargeTokenServiceOuterClass.RechargeToken inUse;
    private static Object rechargeLock = new Object();
    private static Object inUseLock = new Object();

    public TokenQueue(List<RechargeTokenServiceOuterClass.RechargeToken> tokensList){
        this.tokens = tokensList;
        inUse = null;
    }

    public int size(){
        synchronized (rechargeLock){
            return this.tokens.size();
        }
    }

    public void add(RechargeTokenServiceOuterClass.RechargeToken token){
        synchronized (rechargeLock){
            this.tokens.add(token);
            rechargeLock.notifyAll();
            synchronized (inUseLock){
                inUseLock.notifyAll();
            }
        }
    }

    private boolean remove(RechargeTokenServiceOuterClass.RechargeToken rechargeToken){
        synchronized (rechargeLock){
            return this.tokens.remove(rechargeToken);
        }
    }

    public RechargeTokenServiceOuterClass.RechargeToken extract(){
        synchronized (rechargeLock){
            while(this.tokens.isEmpty()){
                try {
                    rechargeLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            RechargeTokenServiceOuterClass.RechargeToken removed = this.tokens.remove(0);
            rechargeLock.notifyAll();
            return removed;
        }
    }

    public void setUnused(){
        synchronized (inUseLock){
            if(this.inUse != null){
                this.add(this.inUse);
            }
            this.inUse = null;
            inUseLock.notifyAll();
        }
    }

    public void setInUse(RechargeTokenServiceOuterClass.RechargeToken rechargeToken){
        synchronized (inUseLock){
            while (!this.remove(rechargeToken)) { // wait util the rechargeToken is present
                try {
                    inUseLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            this.setUnused();
            this.inUse = rechargeToken;
            inUseLock.notifyAll();
        }
    }

    @Override
    public String toString() {
        return tokens.toString();
    }
}
