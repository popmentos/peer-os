package org.safehaus.subutai.core.template.rest;

import java.io.InputStream;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;

public interface RestService {

	@GET
	@Path ("/")
	@Produces ({MediaType.TEXT_PLAIN})
	public String getManagementHostName();

	@PUT
	@Path ("/{hostname}")
	public void setManagementHostName(@PathParam ("hostname") String hostname);

	@POST
	@Path ("/")
	@Consumes ({MediaType.MULTIPART_FORM_DATA})
	@Produces ({MediaType.TEXT_PLAIN})
	public Response importTemplate(@Multipart ("file") InputStream in,
	                               @Multipart ("config_dir") String configDir);

	@GET
	@Path ("/{template}")
	@Produces ({MediaType.APPLICATION_OCTET_STREAM})
    public Response exportTemplate(@PathParam("template") String templateName);

    @DELETE
    @Path("/{template}")
    public Response unregister(@PathParam("template") String templateName);

}
