package beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import java.util.*;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Statistics {

    @XmlElement(name="stats")
    private Hashtable<String, List<TaxiStatistics>> statistics;

    private static Statistics instance;

    private Statistics(){ statistics = new Hashtable<String, List<TaxiStatistics>>();}

    public synchronized static Statistics getInstance(){
        if(instance==null)
            instance = new Statistics();
        return instance;
    }

    public synchronized Hashtable<String, List<TaxiStatistics>> getStatistics() {
        return statistics;
    }

    public synchronized void addNewStats(String id, TaxiStatistics taxiStats){
        statistics.get(id).add(taxiStats);
        // remember pollution !
    }

    public synchronized TaxiStatistics avgOfNStats(String id, int n){
        List<TaxiStatistics> listOfStats = statistics.get(id);
        int s = listOfStats.size();
        if(n > s){
            n = s;
        }
        return computeAverage(statistics.get(id).subList(s-n, s));
    }

    public synchronized TaxiStatistics avgTemporalWindow(double start, double end){

        Enumeration<List<TaxiStatistics>> stats = statistics.elements();

        List<TaxiStatistics> listOfStats = new ArrayList<>();
        while(stats.hasMoreElements()){
            for(TaxiStatistics taxiS: stats.nextElement()){
                System.out.println(taxiS.toString());
                if(taxiS.getTimestamp() >= start && taxiS.getTimestamp() <= end){
                    listOfStats.add(taxiS);
                }
            }
        }
        return computeAverage(listOfStats);
    }

    private TaxiStatistics computeAverage(List<TaxiStatistics> listOfStats){ // an aggregate TaxiStatistics

        if(listOfStats.isEmpty()){ return null; }

        int n = listOfStats.size();
        double countKilometersTravelled = 0.0;
        double countBatteryLevel = 0.0;
        double countRidesAccomplished = 0.0;
        double pollutionMeasured = 0.0;

        for(TaxiStatistics t: listOfStats){

            countKilometersTravelled += t.getKilometersTravelled();
            countBatteryLevel += t.getBatteryLevel();
            countRidesAccomplished += t.getRidesAccomplished();
            pollutionMeasured += t.getPollution();

        }

        TaxiStatistics avgTaxiStats = new TaxiStatistics("overall", listOfStats.get(listOfStats.size()-1).getTimestamp(),
                countKilometersTravelled/n, countBatteryLevel/n, countRidesAccomplished/n,
                pollutionMeasured/n);

        return avgTaxiStats;

    }

}
