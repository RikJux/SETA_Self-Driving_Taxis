package Simulator;

import com.sun.org.apache.bcel.internal.generic.NEW;

import java.util.ArrayList;
import java.util.List;

public class SimulatorData implements Buffer{

    private List<Measurement> measurements = new ArrayList<Measurement>();
    private List<Measurement> averages = new ArrayList<Measurement>();

    @Override
    public void addMeasurement(Measurement m) {

        measurements.add(m);
        int size = measurements.size();

        if(measurements.size() == 8){

            averages.add(new Measurement(measurements.get(0).getId(), measurements.get(0).getType(),
                    computeMean(measurements), measurements.get(size-1).getTimestamp()));

            measurements = measurements.subList(4, 8); // overlap factor: 50%
        }

    }

    @Override
    public List<Measurement> readAllAndClean() {
        List<Measurement> sendAverages = new ArrayList<Measurement>(averages);
        averages.clear();
        return sendAverages;
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
