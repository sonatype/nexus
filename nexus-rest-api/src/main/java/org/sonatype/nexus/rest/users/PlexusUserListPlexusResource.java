package org.sonatype.nexus.rest.users;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.jsecurity.locators.users.PlexusUser;
import org.sonatype.jsecurity.locators.users.PlexusUserManager;
import org.sonatype.nexus.rest.model.PlexusUserListResourceResponse;
import org.sonatype.nexus.rest.model.PlexusUserResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

@Component( role = PlexusResource.class, hint = "PlexusUserListPlexusResource" )
public class PlexusUserListPlexusResource
    extends AbstractPlexusUserPlexusResource
{
    public static final String USER_SOURCE_KEY = "userSource";
    
    @Requirement( role = PlexusUserManager.class, hint="additinalRoles" )
    private PlexusUserManager userManager;
    
    public PlexusUserListPlexusResource()
    {
        setModifiable( false );
    }
    
    @Override
    public Object getPayloadInstance()
    {
        return null;
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        return new PathProtectionDescriptor( "/plexus_users/*", "authcBasic,perms[nexus:users]" );
    }

    @Override
    public String getResourceUri()
    {
        return "/plexus_users/{" + USER_SOURCE_KEY + "}";
    }
    
    @Override
    public Object get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        PlexusUserListResourceResponse result = new PlexusUserListResourceResponse();
        
        for ( PlexusUser user : userManager.listUsers( getUserSource( request ) ) )
        {
            PlexusUserResource res = nexusToRestModel( user, request );

            if ( res != null )
            {
                result.addData( res );
            }
        }

        return result;
    }
    
    protected String getUserSource( Request request )
    {
        return request.getAttributes().get( USER_SOURCE_KEY ).toString();
    }
}
