/**
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2012 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.rest.mirrors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.codehaus.plexus.component.annotations.Component;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.repository.Mirror;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.rest.model.MirrorStatusResource;
import org.sonatype.nexus.rest.model.MirrorStatusResourceListResponse;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

@Component( role = PlexusResource.class, hint = "RepositoryMirrorStatusPlexusResource" )
@Path( RepositoryMirrorStatusPlexusResource.RESOURCE_URI )
@Consumes( { "application/xml", "application/json" } )
public class RepositoryMirrorStatusPlexusResource
    extends AbstractRepositoryMirrorPlexusResource
{
    public static final String RESOURCE_URI = "/repository_mirrors_status/{" + REPOSITORY_ID_KEY + "}";
    
    public RepositoryMirrorStatusPlexusResource()
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
        return new PathProtectionDescriptor( "/repository_mirrors_status/*", "authcBasic,perms[nexus:repositorymirrorsstatus]" );
    }

    @Override
    public String getResourceUri()
    {
        return RESOURCE_URI;
    }

    /**
     * Get the status of the mirrors of a repository.
     * 
     * @param repositoryId The repository to retrieve the assigned mirrors for.
     */
    @Override
    @GET
    @ResourceMethodSignature( pathParams = { @PathParam( AbstractRepositoryMirrorPlexusResource.REPOSITORY_ID_KEY ) },
                              output = MirrorStatusResourceListResponse.class )
    public Object get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        MirrorStatusResourceListResponse dto = new MirrorStatusResourceListResponse();
        
        try
        {
            Repository repository = getRepositoryRegistry().getRepository( getRepositoryId( request ) );
            
            if ( repository.getRepositoryKind().isFacetAvailable( ProxyRepository.class ) )
            {
                ProxyRepository px = repository.adaptToFacet( ProxyRepository.class );
                
                for ( Mirror mirror : px.getDownloadMirrors().getMirrors() )
                {
                    MirrorStatusResource resource = new MirrorStatusResource();
                    resource.setId( mirror.getId() );
                    resource.setUrl( mirror.getUrl() );
                    resource.setStatus( px.getDownloadMirrors().isBlacklisted( mirror ) ? "Blacklisted" : "Available" );
                    
                    dto.addData( resource );
                }
            }
            else
            {
                throw new ResourceException( Status.CLIENT_ERROR_BAD_REQUEST, "Repository is invalid type" );
            }
        }
        catch ( NoSuchRepositoryException e )
        {
            throw new ResourceException( Status.CLIENT_ERROR_BAD_REQUEST, "Invalid repository id " + getRepositoryId( request ), e);
        }
        
        return dto;
    }
}
