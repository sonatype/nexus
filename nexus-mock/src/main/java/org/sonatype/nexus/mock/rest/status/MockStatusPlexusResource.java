package org.sonatype.nexus.mock.rest.status;

import org.codehaus.plexus.component.annotations.Component;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.nexus.mock.rest.MockHelper;
import org.sonatype.nexus.rest.status.StatusPlexusResource;
import org.sonatype.plexus.rest.resource.ManagedPlexusResource;

@Component( role = ManagedPlexusResource.class, hint = "StatusPlexusResource" )
public class MockStatusPlexusResource
    extends StatusPlexusResource
{
    @Override
    public Object get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        return MockHelper.getMockContentFor( request.getMethod().getName(), getResourceUri() );
    }
}
