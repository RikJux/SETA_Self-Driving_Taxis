import seta.smartcity.requestToJoin.RequestToJoinOuterClass.RequestToJoin;
import seta.smartcity.requestToJoinAccept.RequestToJoinAcceptOuterClass.RequestToJoinAccept;

import java.io.IOException;
import java.net.Socket;

public class TaxiComAdmin extends Thread{

    private final Taxi taxi;

    public TaxiComAdmin(Taxi taxi){
        this.taxi = taxi;
    }

    @Override
    public void run() {


        try {
            Socket s = new Socket("localhost", 9999);

            RequestToJoin r = RequestToJoin.newBuilder()
                    .setId(taxi.getId())
                    .setIp(taxi.getIp())
                    .setPort(taxi.getPort())
                    .build();

            System.out.println(r);
            System.out.println("Sending join request to Administrator Server at port " + s.getPort());
            r.writeTo(s.getOutputStream());
            System.out.println("Join request sent to Administrator Server at port " + s.getPort());
            // can just wait
            // IMPORTANT: FIND A WAY HERE TO READ THE RESPONSE
            //RequestToJoinAccept accept = RequestToJoinAccept.parseFrom(s.getInputStream());
            //System.out.println(accept);
            s.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
