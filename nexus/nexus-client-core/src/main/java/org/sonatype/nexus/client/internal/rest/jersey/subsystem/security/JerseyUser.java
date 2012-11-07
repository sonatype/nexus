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

import static org.sonatype.nexus.client.internal.rest.jersey.subsystem.security.JerseyUsers.path;

import java.util.Collections;
import java.util.List;

import org.sonatype.nexus.client.core.subsystem.security.User;
import org.sonatype.nexus.client.internal.rest.jersey.subsystem.JerseyEntitySupport;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.security.rest.model.UserResource;
import org.sonatype.security.rest.model.UserResourceRequest;
import org.sonatype.security.rest.model.UserResourceResponse;

/**
 * Jersey based {@link User} implementation.
 *
 * @since 2.3
 */
public class JerseyUser
    extends JerseyEntitySupport<User, UserResource>
    implements User
{

    public JerseyUser( final JerseyNexusClient nexusClient, final String id )
    {
        super( nexusClient, id );
    }

    @Override
    protected UserResource createSettings( final String id )
    {
        final UserResource resource = new UserResource();
        resource.setUserId( id );
        return resource;
    }

    @Override
    protected UserResource doGet()
    {
        return getNexusClient().serviceResource( path( id() ) )
            .get( UserResourceResponse.class )
            .getData();
    }

    @Override
    protected UserResource doCreate()
    {
        final UserResourceRequest request = new UserResourceRequest();
        request.setData( settings() );
        return getNexusClient().serviceResource( "Users" )
            .post( UserResourceResponse.class, request )
            .getData();
    }

    @Override
    protected UserResource doUpdate()
    {
        final UserResourceRequest request = new UserResourceRequest();
        request.setData( settings() );
        return getNexusClient().serviceResource( path( id() ) )
            .put( UserResourceResponse.class, request )
            .getData();
    }

    @Override
    protected void doRemove()
    {
        getNexusClient().serviceResource( path( id() ) ).delete();
    }

    @Override
    public String firstName()
    {
        return settings().getFirstName();
    }

    @Override
    public String lastName()
    {
        return settings().getLastName();
    }

    @Override
    public String email()
    {
        return settings().getEmail();
    }

    @Override
    public boolean isActive()
    {
        return "active".equals( settings().getStatus() );
    }

    @Override
    public List<String> roles()
    {
        return Collections.unmodifiableList( settings().getRoles() );
    }

    @Override
    public User withFirstName( final String value )
    {
        settings().setFirstName( value );
        return this;
    }

    @Override
    public User withLastName( final String value )
    {
        settings().setLastName( value );
        return this;
    }

    @Override
    public User withEmail( final String value )
    {
        settings().setEmail( value );
        return this;
    }

    @Override
    public User enableAccess()
    {
        settings().setStatus( "active" );
        return this;
    }

    @Override
    public User disableAccess()
    {
        settings().setStatus( "disabled" );
        return this;
    }

    @Override
    public User withRole( final String value )
    {
        settings().addRole( value );
        return this;
    }

    @Override
    public User removeRole( final String value )
    {
        settings().removeRole( value );
        return this;
    }

}
