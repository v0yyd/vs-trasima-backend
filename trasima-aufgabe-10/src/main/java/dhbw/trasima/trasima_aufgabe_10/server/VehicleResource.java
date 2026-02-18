package dhbw.trasima.trasima_aufgabe_10.server;

import dhbw.trasima.trasima_aufgabe_10.model.V2State;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/trasima/vehicles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VehicleResource {

    private static final InMemoryV2Store STORE = new InMemoryV2Store();

    @GET
    public List<V2State> list() {
        return STORE.list();
    }

    @GET
    @Path("{id}")
    public Response get(@PathParam("id") int id) {
        V2State state = STORE.get(id);
        if (state == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(state).build();
    }

    @POST
    @Path("{id}")
    public Response create(@PathParam("id") int id, V2State state) {
        if (state == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing JSON body").build();
        }
        state.id = id;

        boolean created = STORE.create(state);
        if (!created) {
            return Response.status(Response.Status.CONFLICT).entity("Vehicle ID already exists").build();
        }
        return Response.status(Response.Status.CREATED).entity(state).build();
    }

    @PUT
    @Path("{id}")
    public Response update(@PathParam("id") int id, V2State state) {
        if (state == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing JSON body").build();
        }
        state.id = id;

        boolean updated = STORE.update(state);
        if (!updated) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(state).build();
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") int id) {
        boolean deleted = STORE.delete(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}

