package services;

import beans.Statistics;
import beans.TaxiStatistics;
import beans.Taxis;
import beans.TaxiBean;
import com.google.gson.Gson;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

@Path("taxi")
public class TaxiManagement {

    @Path("taxiList")
    @GET
    @Produces({"application/json", "application/xml", MediaType.TEXT_PLAIN})
    public Response getTaxiList(){

        String taxiListString = new Gson().toJson(Taxis.getInstance().getTaxiList());

        return Response.ok(taxiListString).build();
    }

    @Path("join")
    @POST
    @Consumes({"application/json", "application/xml", MediaType.TEXT_PLAIN})
    public Response joinRequest(TaxiBean t){
        if(!Taxis.getInstance().isIdPresent(t.getId())){
            Taxis.getInstance().addTaxi(t);
            Statistics stats = Statistics.getInstance();
            stats.getStatistics().put(t.getId(), new ArrayList<TaxiStatistics>());
            String taxisString = new Gson().toJson(Taxis.getInstance());
            return Response.status(Response.Status.OK)
                    .entity(taxisString)
                    .build();
        }else{
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
    }

    @Path("leave/{id}")
    @DELETE
    @Consumes({"application/json", "application/xml"})
    public Response leaveRequest(@PathParam("id") String id){
        boolean removed = Taxis.getInstance().removeTaxi(id);
        if(removed){
            return Response.ok().build();
        }else{
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

}
