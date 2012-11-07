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

import static org.sonatype.nexus.client.internal.rest.jersey.subsystem.security.JerseyRoles.path;

import java.util.Collections;
import java.util.List;

import org.sonatype.nexus.client.core.subsystem.security.Role;
import org.sonatype.nexus.client.internal.rest.jersey.subsystem.JerseyEntitySupport;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.security.rest.model.RoleResource;
import org.sonatype.security.rest.model.RoleResourceRequest;
import org.sonatype.security.rest.model.RoleResourceResponse;

/**
 * Jersey based {@link Role} implementation.
 *
 * @since 2.3
 */
public class JerseyRole
    extends JerseyEntitySupport<Role, RoleResource>
    implements Role
{

    public JerseyRole( final JerseyNexusClient nexusClient, final String id )
    {
        super( nexusClient, id );
    }

    @Override
    protected RoleResource createSettings( final String id )
    {
        final RoleResource resource = new RoleResource();
        resource.setId( id );
        resource.setUserManaged( true );
        return resource;
    }

    @Override
    protected RoleResource doGet()
    {
        return getNexusClient().serviceResource( path( id() ) )
            .get( RoleResourceResponse.class )
            .getData();
    }

    @Override
    protected RoleResource doCreate()
    {
        final RoleResourceRequest request = new RoleResourceRequest();
        request.setData( settings() );
        return getNexusClient().serviceResource( "roles" )
            .post( RoleResourceResponse.class, request )
            .getData();
    }

    @Override
    protected RoleResource doUpdate()
    {
        final RoleResourceRequest request = new RoleResourceRequest();
        request.setData( settings() );
        return getNexusClient().serviceResource( path( id() ) )
            .put( RoleResourceResponse.class, request )
            .getData();
    }

    @Override
    protected void doRemove()
    {
        getNexusClient().serviceResource( path( id() ) ).delete();
    }

    @Override
    public String name()
    {
        return settings().getName();
    }

    @Override
    public String description()
    {
        return settings().getDescription();
    }

    @Override
    public List<String> privileges()
    {
        return Collections.unmodifiableList( settings().getPrivileges() );
    }

    @Override
    public List<String> roles()
    {
        return Collections.unmodifiableList( settings().getRoles() );
    }

    @Override
    public Role withDescription( final String value )
    {
        settings().setDescription( value );
        return this;
    }

    @Override
    public Role withName( final String value )
    {
        settings().setName( value );
        return this;
    }

    @Override
    public Role withPrivilege( final String value )
    {
        settings().addPrivilege( value );
        return this;
    }

    @Override
    public Role withRole( final String value )
    {
        settings().addRole( value );
        return this;
    }

    @Override
    public Role removePrivilege( final String value )
    {
        settings().removePrivilege( value );
        return this;
    }

    @Override
    public Role removeRole( final String value )
    {
        settings().removeRole( value );
        return this;
    }

}
