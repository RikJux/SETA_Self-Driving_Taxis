package services;

import beans.Taxis;
import beans.TaxiBean;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("taxi")
public class TaxiManagement {

    @Path("taxiList")
    @GET
    @Produces({"application/json", "application/xml"})
    public Response getTaxiList(){
        return Response.ok(Taxis.getInstance()).build();
    }

    @Path("join")
    @POST
    @Consumes({"application/json", "application/xml"})
    public Response joinRequest(TaxiBean t){
        if(!Taxis.getInstance().isIdPresent(t.getId())){
            Taxis.getInstance().addTaxi(t);
            return Response.ok().entity(Taxis.getInstance()).build();
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
