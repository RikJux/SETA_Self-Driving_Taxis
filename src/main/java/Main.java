import TaxiPackage.Taxi;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String args[]) {
        int serverPort = 1337;
        int idOffset = 1;
        System.out.println("Hello World!");
        List<Integer> l = new ArrayList<Integer>();
        for(int i=0; i<10;i++){
            l.add(i);
        }
        System.out.println(l);
        l = l.subList(3,8);
        System.out.println(l);
    }
}
