package sharycar.pickup.api;


import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;

import sharycar.pickup.persistence.Pickup;

@Path("/pickups")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PickupResource {

    @PersistenceContext
    private EntityManager em;


    /**
     *  Get all pickups
     */
    @GET
    @Path("/all")
    public Response getPickups() {

        TypedQuery<Pickup> query = em.createNamedQuery("Pickup.findAll", Pickup.class);

        List<Pickup> pickups = query.getResultList();

        return Response.ok(pickups).build();

    }





    /**
     * Get pickup details by id
     * @param pickupId
     * @return
     */
    @GET
    @Path("/pickup/{pickupId}")
    public Response getPickupDetails(@PathParam("pickupId") Integer pickupId) {

        try {
            Query query = em.createQuery("SELECT p FROM Pickup p WHERE p.id = :pickupId");
            query.setParameter("pickupId", pickupId);
            return Response.ok(query.getResultList()).build();

            // @TODO implement service discovery and also return car details


        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

    }

    /**
     * Get pickups for car
     * @param carId
     * @return
     */
    @GET
    @Path("/car/{carId}")
    public Response getPickupCarDetails(@PathParam("carId") Integer carId) {

        try {
            Query query = em.createQuery("SELECT p FROM Pickup p WHERE p.carId = :carId");
            query.setParameter("carId", carId);
            return Response.ok(query.getResultList()).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

    }




    @POST
    @Path("/pickup")
    public Response createPickup(Pickup pickup) {

        pickup.setId(null);
        pickup.setPickupDateTime(new Date());

        if (pickup.getCar_id() == null ) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (pickup.getPickUpLocation() == null || pickup.getPickUpLocation().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } else {
            // Create record in database
            try {
                em.getTransaction().begin();
                em.persist(pickup);
                em.getTransaction().commit();
            } catch (Exception e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(pickup).build();
            }
        }

        if (pickup.getId() != null) {
            return Response.status(Response.Status.CREATED).entity(pickup).build();
        } else {
            return Response.status(Response.Status.CONFLICT).entity(pickup).build();
        }
    }

    @POST
    @Path("/return")
    public Response createReturn(Pickup pickup) {

        pickup.setReturnDateTime(new Date());

        if (pickup.getId() == null ) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (pickup.getReturnLocation() == null || pickup.getReturnLocation().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } else {
            // Create record in database
            try {
                em.getTransaction().begin();
                em.merge(pickup); //@TODO:make sure this updates previous record
                em.getTransaction().commit();
            } catch (Exception e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(pickup).build();
            }
        }

        if (pickup.getId() != null) {
            return Response.status(Response.Status.ACCEPTED).entity(pickup).build();
        } else {
            return Response.status(Response.Status.CONFLICT).entity(pickup).build();
        }
    }



}
