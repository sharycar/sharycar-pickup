package sharycar.pickup.api;


import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;

import com.kumuluz.ee.discovery.annotations.DiscoverService;
import com.kumuluz.ee.logs.cdi.Log;
import sharycar.pickup.persistence.Pickup;
import sharycar.pickup.persistence.Payment;


@Path("/pickups")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Log
public class PickupResource {


    @Inject
    @DiscoverService(value = "payment-service", version = "1.0.x", environment = "dev")
    private WebTarget target;



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

    /**
     *
     * @param pickup
     * @return
     */
    @POST
    @Path("/return")
    public Response createReturn(Pickup pickup) {


        if (pickup.getId() == null ) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (pickup.getReturnLocation() == null || pickup.getReturnLocation().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } else {
            // Update record in database
            try {
                em.getTransaction().begin();
                Pickup pick = em.getReference(Pickup.class, pickup.getId());
                pick.setReturnLocation(pickup.getReturnLocation());
                pick.setReturnDateTime(new Date());
                em.merge(pick);
                em.getTransaction().commit();
            } catch (Exception e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(pickup).build();
            }
        }

        Client client = ClientBuilder.newClient();
        //  WebTarget paymentService = client.target("http://104.197.143.157:8080");
        WebTarget paymentService = target;
        // Execute reservation on credit card
        paymentService = paymentService.path("payments/add");



        Response response;

        Payment p = new Payment();
        p.setUser_id(pickup.getUser_id());
        p.setPrice(Math.random() * 10);
        p.setCurrency("USD");
        p.setOrderId(pickup.getId());

        try {
            response = paymentService.request().post(Entity.json(p));
        } catch (ProcessingException e) {
            return Response.ok("Error service call message: "+"( "+paymentService.getUri()+")"+"ERRmsg"+ e.getMessage()+ e.getStackTrace()).build();
        }


        if (pickup.getId() != null) {
            return Response.status(Response.Status.ACCEPTED).entity(pickup).build();
        } else {
            return Response.status(Response.Status.CONFLICT).entity(pickup).build();
        }
    }



}
