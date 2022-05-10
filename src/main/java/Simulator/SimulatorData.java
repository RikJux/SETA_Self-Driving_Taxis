package Simulator;

import java.util.ArrayList;
import java.util.List;

public class SimulatorData implements Buffer{

    private List<Measurement> measurements = new ArrayList<Measurement>();
    private List<Measurement> averages = new ArrayList<Measurement>();
    private double oldAvg = 0;
    private double newAvg = 0;

    @Override
    public void addMeasurement(Measurement m) {

        measurements.add(m);
        int size = measurements.size();

        if(size % 4 == 0){
            oldAvg = newAvg; // shift
            newAvg = computeMean(measurements.subList(size-4, size)); // avg of last four
            if(size % 8 == 0){
                averages.add(new Measurement("0", "avg", (oldAvg+newAvg)/2,
                        measurements.get(size-1).getTimestamp()));
                // already weighted as the size of the sample is always the same
            }
        }

    }

    @Override
    public List<Measurement> readAllAndClean() {

        return averages;

    }

    private double computeMean(List<Measurement> meas){

        double mean = 0;

        for(Measurement m: meas){
            mean += m.getValue();
        }

        mean = mean / meas.size();

        return mean;

    }

}
