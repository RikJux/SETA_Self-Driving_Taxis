package TaxiPackage;
import Simulator.PM10Simulator;
import Simulator.SimulatorData;

public class Sensor extends Thread{

    public void run() {

        PM10Simulator p = new PM10Simulator(new SimulatorData());
        p.start();
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(p.getBuffer().readAllAndClean());

    }
}
