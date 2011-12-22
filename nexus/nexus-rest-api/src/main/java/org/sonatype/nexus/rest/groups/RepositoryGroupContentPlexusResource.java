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
package org.sonatype.nexus.rest.groups;

import org.codehaus.plexus.component.annotations.Component;
import org.restlet.data.Request;
import org.restlet.resource.ResourceException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStore;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.rest.AbstractResourceStoreContentPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

/**
 * @author tstevens
 */
@Component( role = PlexusResource.class, hint = "RepositoryGroupContentPlexusResource" )
public class RepositoryGroupContentPlexusResource
    extends AbstractResourceStoreContentPlexusResource
{
    public static final String GROUP_ID_KEY = "groupId";

    @Override
    public Object getPayloadInstance()
    {
        // group content is read only
        return null;
    }

    @Override
    public String getResourceUri()
    {
        return "/repo_groups/{" + GROUP_ID_KEY + "}/content";
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        return new PathProtectionDescriptor( "/repo_groups/*/content/**", "authcBasic,tgperms" );
    }

    @Override
    protected ResourceStore getResourceStore( final Request request )
        throws NoSuchRepositoryException, ResourceException
    {
        return getRepositoryRegistry().getRepositoryWithFacet( request.getAttributes().get( GROUP_ID_KEY ).toString(),
            GroupRepository.class );
    }

}
