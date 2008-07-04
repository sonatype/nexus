/*
 * Nexus: Maven Repository Manager
 * Copyright (C) 2008 Sonatype Inc.                                                                                                                          
 * 
 * This file is part of Nexus.                                                                                                                                  
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 */
package org.sonatype.nexus.rest.users;

import java.util.logging.Level;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.sonatype.nexus.rest.AbstractNexusResourceHandler;
import org.sonatype.nexus.rest.model.UserResource;
import org.sonatype.nexus.rest.model.UserStatusResource;
import org.sonatype.nexus.rest.model.UserStatusRoleResource;

public class AbstractUserResourceHandler
extends AbstractNexusResourceHandler
{
    public static final String USER_ID_KEY = "userID";
    private static final String ROLE_VALIDATION_ERROR = "The user cannot have zero roles!";

    /**
     * The default constructor.
     * 
     * @param context
     * @param request
     * @param response
     */
    public AbstractUserResourceHandler( Context context, Request request, Response response )
    {
        super( context, request, response );
    }
    
    public boolean validateFields( UserResource resource, Representation representation )
    {        
        if ( resource.getRoles() == null || resource.getRoles().size() == 0 )
        {
            getLogger().log(
                Level.INFO,
                "The userID (" + resource.getUserID() + ") cannot have 0 roles!" );
            
            getResponse().setStatus(
                Status.CLIENT_ERROR_BAD_REQUEST,
                ROLE_VALIDATION_ERROR );

            getResponse().setEntity(
                serialize( representation, getNexusErrorResponse(
                "users",
                ROLE_VALIDATION_ERROR ) ) );

            return false;
        }   
        
        return true;
    }
    
    public UserStatusResource nexusToRestModel()
    {
        //TODO: ultimately this method will take a parameter which is the nexus object
        //and will convert to the rest object
        UserStatusResource resource = new UserStatusResource();
        resource.setEmail( "someemail@someemail.com" );
        resource.setName( "Real Name" );
        resource.setStatus( "active" );
        resource.setUserID( "realuser" );
        resource.setResourceURI( calculateSubReference( resource.getUserID() ).toString() );

        UserStatusRoleResource roleResource = new UserStatusRoleResource();
        roleResource.setValue( "roleid" );
        roleResource.setDisplay( "rolename" );
        
        resource.addRole( roleResource );
        
        return resource;
    }
}
