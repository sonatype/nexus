package org.sonatype.nexus.rest.users;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class UserResetResourceHandler
    extends AbstractUserResourceHandler
{    
    public UserResetResourceHandler( Context context, Request request, Response response )
    {
        super( context, request, response );
    }
    
    public boolean allowDelete()
    {
        return true;
    }
    
    @Override
    public void delete()
    {
        //TODO: delete something
    }
}