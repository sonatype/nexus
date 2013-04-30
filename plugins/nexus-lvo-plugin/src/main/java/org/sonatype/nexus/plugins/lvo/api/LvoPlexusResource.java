/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2013 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
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
