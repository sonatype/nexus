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
package org.sonatype.nexus.rest.index;

import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.codehaus.plexus.component.annotations.Component;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

@Component( role = PlexusResource.class, hint = "RepositoryOrGroupIncrementalIndexPlexusResource" )
@Path( RepositoryOrGroupIncrementalIndexPlexusResource.RESOURCE_URI )
public class RepositoryOrGroupIncrementalIndexPlexusResource
    extends AbstractIndexPlexusResource
{
    public static final String RESOURCE_URI = "/data_incremental_index/{" + DOMAIN + "}/{" + TARGET_ID + "}";

    @Override
    public String getResourceUri()
    {
        return RESOURCE_URI;
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        return new PathProtectionDescriptor( "/data_incremental_index/*/**", "authcBasic,perms[nexus:index]" );
    }

    @Override
    protected boolean getIsFullReindex()
    {
        return false;
    }

    /**
     * Perform an incremental reindex against the provided repository or group. Note that
     * appended to the end of the url should be the path that you want to index.  i.e.
     * /data_incremental_index/repositories/myRepo/org/blah will index everything under the org/blah directory.  
     * Leaving blank will simply index whole domain content.
     * 
     * @param domain The domain that will be used, valid options are 'repositories' or 'repo_groups' (Required).
     * @param target The unique id in the domain to use (i.e. repository or group id) (Required).
     */
    @Override
    @DELETE
    @ResourceMethodSignature( pathParams = { @PathParam( AbstractIndexPlexusResource.DOMAIN ), @PathParam( AbstractIndexPlexusResource.TARGET_ID ) } )
    public void delete( Context context, Request request, Response response )
        throws ResourceException
    {
        super.delete( context, request, response );
    }
}
