package Simulator;

import java.util.ArrayList;
import java.util.List;

public class SimulatorData implements Buffer{

    private List<Measurement> measurements = new ArrayList<Measurement>();

    @Override
    public void addMeasurement(Measurement m) {

        measurements.add(m);

    }

    @Override
    public List<Measurement> readAllAndClean() {

        return measurements;

    }
}
