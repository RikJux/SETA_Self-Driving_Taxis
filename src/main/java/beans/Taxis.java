package beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Taxis {

    @XmlElement(name="taxis")
    private List<TaxiBean> taxiList;

    private static Taxis instance;

    private Taxis() {this.taxiList = new ArrayList<TaxiBean>();}

    public synchronized static Taxis getInstance(){
        if(instance==null)
            instance = new Taxis();
        return instance;
    }

    public synchronized List<TaxiBean> getTaxiList(){
        return new ArrayList<>(taxiList);
    }

    public synchronized boolean isIdPresent(String newId){

        boolean isPresent = false;

        for(TaxiBean t: taxiList){
            if(t.getId().equals(newId)){
                isPresent = true;
                break;
            }
        }

        return isPresent;

    }

    public synchronized void addTaxi(TaxiBean t){taxiList.add(t);}

    public synchronized boolean removeTaxi(String id){

        boolean removed = false;

        for(TaxiBean t: taxiList){
            if(id.equals(t.getId())){
                taxiList.remove(t);
                removed = true;
                break;
            }
        }

        return removed;

    }

    public static int[] randomCoord() {
        Random rand = new Random();

        int coord[] = new int[2];

        for(int i=0; i<2; i++){
            if(rand.nextBoolean()){
                coord[i] = 0;
            }else{
                coord[i] = 9;
            }
        }

        return coord;

    }

}
