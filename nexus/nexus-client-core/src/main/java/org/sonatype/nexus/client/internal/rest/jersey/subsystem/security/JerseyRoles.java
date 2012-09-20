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
package org.sonatype.nexus.client.internal.rest.jersey.subsystem.security;

import java.util.List;

import org.sonatype.nexus.client.core.subsystem.security.Roles;
import org.sonatype.nexus.client.internal.rest.jersey.subsystem.JerseyCRUDSupport;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.security.rest.model.RoleListResourceResponse;
import org.sonatype.security.rest.model.RoleResource;
import org.sonatype.security.rest.model.RoleResourceRequest;
import org.sonatype.security.rest.model.RoleResourceResponse;

public class JerseyRoles
    extends JerseyCRUDSupport<RoleResource, RoleListResourceResponse, RoleResourceResponse, RoleResourceRequest>
    implements Roles
{

    public JerseyRoles( JerseyNexusClient nexusClient )
    {
        super( nexusClient, "roles" );
    }

    @Override
    protected String getId( RoleResource item )
    {
        return item.getId();
    }

    @Override
    protected List<RoleResource> getListData( RoleListResourceResponse response )
    {
        return response.getData();
    }

    @Override
    protected RoleResource getData( RoleResourceResponse response )
    {
        return response.getData();
    }

    @Override
    protected void setData( RoleResourceRequest request, RoleResource item )
    {
        request.setData( item );
    }

}

