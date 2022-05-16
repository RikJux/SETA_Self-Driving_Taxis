package services;

import beans.Statistics;
import beans.TaxiStatistics;
import com.google.gson.Gson;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("statistics")
public class StatsManagement {

    @Path("post/{id}")
    @POST
    @Consumes({"application/json", "application/xml"})
    public Response putStatisticsTo(@PathParam("id") String id, TaxiStatistics taxiStats){
        // append new taxi statistics
        Statistics s = Statistics.getInstance();
        s.addNewStats(id, taxiStats);
        return Response.ok(s).build();
    }

    @Path("get/{id}/{n}")
    @GET
    @Produces({MediaType.TEXT_PLAIN})
    public Response getAveragesOf(@PathParam("id") String id, @PathParam("n") int n){

        TaxiStatistics taxiStats = Statistics.getInstance().avgOfNStats(id, n);
        String statString = new Gson().toJson(taxiStats);
        if(statString != null) {
            return Response.status(Response.Status.OK)
                    .entity(statString)
                    .build();
        }else{
            return Response.status(Response.Status.NO_CONTENT).build();
        }

    }

    @Path("get/window/{from}/{to}")
    @GET
    @Produces({MediaType.TEXT_PLAIN})
    public Response getAverageInWindow(@PathParam("from") double from, @PathParam("to") double to){

        TaxiStatistics taxiStats = Statistics.getInstance().avgTemporalWindow(from, to);
        String statString = new Gson().toJson(taxiStats);
        if(statString != null){
            return Response.status(Response.Status.OK)
                    .entity(statString)
                    .build();
        }else{
            return Response.status(Response.Status.NO_CONTENT).build();
        }

    }

}
