package org.sonatype.nexus.plugins.security.api;

import org.codehaus.plexus.component.annotations.Component;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.nexus.rest.AbstractNexusPlexusResource;
import org.sonatype.nexus.rest.AdvancedSecurityPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

@Component( role = PlexusResource.class, hint = "ThreePrivsSecurityPlexusResource" )
public class ThreePrivsSecurityPlexusResource extends AbstractNexusPlexusResource
implements AdvancedSecurityPlexusResource
{

    private static final String URI = "/three_privs";

    private static final PathProtectionDescriptor[] PROTECTIONS = new PathProtectionDescriptor[] {
        new PathProtectionDescriptor( URI, "authcBasic,perms[nexus:threepriv:one]" ),
        new PathProtectionDescriptor( URI, "authcBasic,perms[nexus:threepriv:two]" ),
        new PathProtectionDescriptor( URI, "authcBasic,perms[nexus:threepriv:three]" ) };

    @Override
    public PathProtectionDescriptor[] getResourceProtections()
    {
        return PROTECTIONS;
    }

    @Override
    public String getResourceUri()
    {
        return URI;
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getPayloadInstance()
    {
        return null;
    }

    @Override
    public Object get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        return "pong";
    }

}
