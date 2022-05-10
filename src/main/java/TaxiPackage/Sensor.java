package TaxiPackage;
import Simulator.Measurement;
import Simulator.PM10Simulator;
import Simulator.SimulatorData;

import java.util.ArrayList;
import java.util.List;

public class Sensor extends Thread{

    public void run() {

        PM10Simulator p = new PM10Simulator(new SimulatorData());
        p.start();
        try {
            Thread.sleep(10000);
            System.out.println(p.getBuffer().readAllAndClean());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
