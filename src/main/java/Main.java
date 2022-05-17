import AdministratorPackage.AdministratorServer;
import TaxiPackage.Taxi;
import org.eclipse.paho.client.mqttv3.MqttException;

public class Main {
    public static void main(String args[]) {
        int id = 0;
        final int port = 1884;

        SETA s = new SETA();
        AdministratorServer adminServer = new AdministratorServer();
        Taxi t = new Taxi(String.valueOf(id++), "localhost", port+id);

        new Thread(() -> {
            try {
                s.main(null);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                adminServer.main(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            t.main(null);
        }).start();

    }
}
