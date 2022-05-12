package beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

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

    public synchronized TaxiBean removeTaxi(String id){

        TaxiBean removed = null;

        for(TaxiBean t: taxiList){
            if(id.equals(t.getId())){
                taxiList.remove(t);
                removed = t;
            }
        }

        return removed;

    }

}
