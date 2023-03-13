package TaxiPackage.Impl;

import TaxiPackage.Taxi;
import beans.TaxiBean;
import io.grpc.stub.StreamObserver;
import taxi.communication.joinService.JoinServiceGrpc;
import taxi.communication.joinService.JoinServiceOuterClass;

import static Utils.Utils.*;

public class JoinServiceImpl extends JoinServiceGrpc.JoinServiceImplBase {

    private Taxi thisTaxi;

    public JoinServiceImpl(Taxi thisTaxi) {
        this.thisTaxi = thisTaxi;
    }

    @Override
    public void join(JoinServiceOuterClass.JoinMsg request, StreamObserver<JoinServiceOuterClass.JoinOk> responseObserver) {
        System.out.println("[JOIN SRV] Taxi " + request.getId() + " is joining at "
                + request.getPosition().getX() + ", " + request.getPosition().getY());
        responseObserver.onNext(JoinServiceOuterClass.JoinOk.newBuilder().build());

        if(!thisTaxi.getId().equals(request.getId())){
            TaxiBean joinTaxiBean = new TaxiBean(request.getId(), request.getIp(), request.getPort());
            thisTaxi.getTaxiList().add(joinTaxiBean);
            System.out.println("[JOIN SRV] Taxi "+ request.getId() + " was added to taxi list.");
            synchronized (thisTaxi.getNextLock()){
                thisTaxi.setNextTaxi(updateNextOnJoin(thisTaxi, joinTaxiBean));
                thisTaxi.getNextLock().notifyAll();
            }
        }

        responseObserver.onCompleted();
    }
}
