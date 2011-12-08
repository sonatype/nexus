/**
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions
 *
 * This program is free software: you can redistribute it and/or modify it only under the terms of the GNU Affero General
 * Public License Version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License Version 3
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License Version 3 along with this program.  If not, see
 * http://www.gnu.org/licenses.
 *
 * Sonatype Nexus (TM) Open Source Version is available from Sonatype, Inc. Sonatype and Sonatype Nexus are trademarks of
 * Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation. M2Eclipse is a trademark of the Eclipse Foundation.
 * All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.plugins.lvo.api;

import java.io.IOException;

import com.thoughtworks.xstream.XStream;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.nexus.plugins.lvo.DiscoveryResponse;
import org.sonatype.nexus.plugins.lvo.LvoPlugin;
import org.sonatype.nexus.plugins.lvo.NoSuchKeyException;
import org.sonatype.nexus.plugins.lvo.NoSuchStrategyException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.plexus.rest.resource.AbstractPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

@Component( role = PlexusResource.class, hint = "LvoPlexusResource" )
public class LvoPlexusResource
    extends AbstractPlexusResource
{
    @Requirement
    private LvoPlugin lvoPlugin;

    @Override
    public Object getPayloadInstance()
    {
        // this happens to be RO resource
        return null;
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        // unprotected resource
        return new PathProtectionDescriptor( "/lvo/*", "authcBasic,perms[nexus:status]" );
    }

    @Override
    public String getResourceUri()
    {
        return "/lvo/{key}";
    }

    public void configureXStream( XStream x )
    {
        DiscoveryResponse.configureXStream( x );
    }

    @Override
    public Object get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        String key = (String) request.getAttributes().get( "key" );

        try
        {
            DiscoveryResponse dr = lvoPlugin.getLatestVersionForKey( key );

            if ( dr.isSuccessful() )
            {
                return dr;
            }
            else
            {
                throw new ResourceException( Status.CLIENT_ERROR_NOT_FOUND, "Latest version for key='" + key
                    + "' not found." );
            }
        }
        catch ( NoSuchKeyException e )
        {
            throw new ResourceException( Status.CLIENT_ERROR_NOT_FOUND, e.getMessage(), e );
        }
        catch ( NoSuchStrategyException e )
        {
            throw new ResourceException( Status.SERVER_ERROR_INTERNAL, e.getMessage(), e );
        }
        catch ( NoSuchRepositoryException e )
        {
            throw new ResourceException( Status.SERVER_ERROR_INTERNAL, e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new ResourceException( Status.SERVER_ERROR_INTERNAL, e.getMessage(), e );
        }
    }

}
