package AdministratorPackage;

import beans.TaxiBean;
import beans.TaxiStatistics;
import beans.Taxis;
import com.google.gson.Gson;
import com.sun.jersey.api.client.*;

import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AdministratorClient {

    private static String serverAddress = "http://localhost:1337";
    private static String statsNPath = "/statistics/get/%s/%d";
    private static String statsWindowPath = "/statistics/get/window/%f/%f";
    private static String taxisPath = "/taxi/taxiList";
    private static Client client;

    public static void main(String args[]){
        client = Client.create();
        Scanner in = new Scanner(System.in);
        String statsType = null;

        while(statsType == null){
            TaxiStatistics taxiStats = null;
            System.out.println("What type of statistics do you want? Type:");
            System.out.println("[T] for taxi-specific statistics; [W] for overall statistics in a temporal window;");
            System.out.println("[L] for list of taxis; [Q] to quit.");
            statsType = in.nextLine();
            switch(statsType) {
                case ("T"):
                    String[] idN = getTaxiIdN(in);
                    System.out.println("Retrieving averages for " + idN[1] + " measurements for taxi " + idN[0]);
                    taxiStats = statsOfTaxi(idN[0], Integer.parseInt(idN[1]));
                    System.out.println(taxiStats.toString());
                    break;
                case ("W"):
                    double[] window = getWindow(in);
                    System.out.println("Retrieving averages for time window [" + window[0] + ", " + window[1] + "]");
                    taxiStats = statsInTempWindow(window[0], window[1]);
                    System.out.println(taxiStats.toString());
                    break;
                case ("L"):
                    List<TaxiBean> taxiList = getTaxis(client);
                    for (TaxiBean t : taxiList) {
                        System.out.println(t.toString());
                    }
                    break;
                case ("Q"):
                    System.out.println("Farewell");
                    return;
                default:
                    System.out.println("Invalid statistics selection");
            }
            statsType = null;
        }


    }

    private static double[] getWindow(Scanner in){
        double[] window = new double[2];
        double from = -1;
        double to = -1;

        while(from < 0 || to < 0){
            System.out.println("Insert the beginning timestamp:");
            try{
                from = Double.parseDouble(in.nextLine());
                if(from < 0){
                    System.out.println("Invalid input: beginning timestamp must be greater than 0");
                }else{
                    try{
                        System.out.println("Insert the ending timestamp:");
                        to = Double.parseDouble(in.nextLine());
                        if(to < 0){
                            System.out.println("Invalid input: ending timestamp must be greater than 0");
                        }else{
                            if(to < from){
                                System.out.println("Invalid input: ending timestamp must be greater than beginning timestamp");
                                from = -1;
                                to = -1;
                            }else{
                                window[0] = from;
                                window[1] = to;
                            }
                        }
                    }catch(NumberFormatException e){
                        System.out.println("Invalid input: ending timestamp is not a double");
                    }
                }
            }catch(NumberFormatException e){
                System.out.println("Invalid input: beginning timestamp is not a double");
            }

        }

        return window;

    }

    private static String[] getTaxiIdN(Scanner in){
        String[] idN = new String[2];
        String id = null;
        int n = 0;
        while(id == null){
            System.out.println("Insert the taxi id:");
            id = in.nextLine();
            for(TaxiBean t: getTaxis(client)){
                if(t.getId().equals(id)){
                    id = t.getId();
                    break; // id found
                }
                id = null;
            }
            if(id == null){
                System.out.println("Input error: id not found");
            }else{
                idN[0] = id;
                while(n <= 0){
                    try {
                        System.out.println("Insert how many measurements are to take into account:");
                        n = Integer.parseInt(in.nextLine());
                    }catch(NumberFormatException e){
                        System.out.println("Invalid input: n not an int");
                        n = 0;
                    }
                    if(n <= 0){
                        System.out.println("Invalid input: n <= 0");
                    }
                } // n > 0
                idN[1] = String.valueOf(n);
            }
        }
        return idN;
    }

    public static TaxiStatistics statsOfTaxi(String id, int n){
        String url = String.format(serverAddress+statsNPath, id, n);
        return getTaxiStats(client, url);
    }

    public static TaxiStatistics statsInTempWindow(double from, double to){
        String url = String.format(serverAddress+statsWindowPath, from, to);
        return getTaxiStats(client, url);
    }

    public static List<TaxiBean> getTaxis(Client client){
        String url = serverAddress+taxisPath;
        ClientResponse clientResponse = getRequest(client, url, MediaType.APPLICATION_JSON);
        List<TaxiBean> taxiList = clientResponse.getEntity(Taxis.class).getTaxiList();
        return taxiList;
    }

    //HERE
    public static TaxiStatistics getTaxiStats(Client client, String url){
        ClientResponse clientResponse = getRequest(client, url, MediaType.TEXT_PLAIN);
        TaxiStatistics taxiStats = new Gson().fromJson(clientResponse.getEntity(String.class), TaxiStatistics.class);
        if(taxiStats == null){
            System.out.println("Something wrong in getting statistics");
        }
        return taxiStats;
    }

    public static ClientResponse getRequest(Client client, String url, String mediaType){
        WebResource webResource = client.resource(url);
        try {
            return webResource.type(mediaType).get(ClientResponse.class);
        } catch (ClientHandlerException e) {
            System.out.println("Error in getting statistics");
            return null;
        }
    }

}
