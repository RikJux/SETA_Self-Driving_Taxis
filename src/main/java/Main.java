import AdministratorPackage.AdministratorServer;
import TaxiPackage.Taxi;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String args[]) {
        int id = 0;
        int port = 9900;

        SETA s = new SETA();
        AdministratorServer adminServer = new AdministratorServer();
        Taxi t = new Taxi(String.valueOf(id), "localhost", port);

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
