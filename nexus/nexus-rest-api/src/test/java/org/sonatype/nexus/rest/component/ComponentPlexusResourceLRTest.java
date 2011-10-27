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
package org.sonatype.nexus.rest.component;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.restlet.data.Request;
import org.restlet.resource.ResourceException;
import org.sonatype.nexus.AbstractNexusTestCase;
import org.sonatype.nexus.rest.model.PlexusComponentListResource;
import org.sonatype.nexus.rest.model.PlexusComponentListResourceResponse;
import org.sonatype.plexus.rest.resource.PlexusResource;

// This is an IT just because it runs longer then 15 seconds
public class ComponentPlexusResourceLRTest
    extends AbstractNexusTestCase
{
    private AbstractComponentListPlexusResource getComponentPlexusResource()
        throws Exception
    {
        return (AbstractComponentListPlexusResource) this.lookup( PlexusResource.class, "ComponentPlexusResource" );
    }

    private PlexusComponentListResourceResponse runGetForRole( String role )
        throws Exception
    {
        AbstractComponentListPlexusResource componentPlexusResource = this.getComponentPlexusResource();

        Request request = new Request();
        request.getAttributes().put( AbstractComponentListPlexusResource.ROLE_ID, role );

        return (PlexusComponentListResourceResponse) componentPlexusResource.get( null, request, null, null );
    }

    @Test
    public void testInvalidRole()
        throws Exception
    {
        try
        {
            runGetForRole( "JUNK-FOO_BAR-JUNK" );
            Assert.fail( "expected error thrown" );
        }
        catch ( ResourceException e )
        {
            Assert.assertEquals( "Expected a 404 error message.", 404, e.getStatus().getCode() );
        }
    }

    @Test
    public void testValidRoleMultipleResults()
        throws Exception
    {
        PlexusComponentListResourceResponse result = runGetForRole( PlexusResource.class.getName() );

        Assert.assertTrue( result.getData().size() > 1 ); // expected a bunch of these thing, with new ones being
        // added all the time.

        // now for a more controled test
        result = runGetForRole( "MULTI_TEST" );
        Assert.assertEquals( 2, result.getData().size() );

        // the order is undefined
        PlexusComponentListResource resource1 = null;
        PlexusComponentListResource resource2 = null;

        for ( PlexusComponentListResource resource : (List<PlexusComponentListResource>) result.getData() )
        {
            if ( resource.getRoleHint().endsWith( "1" ) )
            {
                resource1 = resource;
            }
            else
            {
                resource2 = resource;
            }
        }

        // make sure we found both
        Assert.assertNotNull( resource1 );
        Assert.assertNotNull( resource2 );

        Assert.assertEquals( "Description-1", resource1.getDescription() );
        Assert.assertEquals( "hint-1", resource1.getRoleHint() );

        Assert.assertEquals( "Description-2", resource2.getDescription() );
        Assert.assertEquals( "hint-2", resource2.getRoleHint() );

    }

    @Test
    public void testValidRoleSingleResult()
        throws Exception
    {
        PlexusComponentListResourceResponse result = runGetForRole( "TEST_ROLE" );

        Assert.assertTrue( result.getData().size() == 1 );

        PlexusComponentListResource resource = (PlexusComponentListResource) result.getData().get( 0 );

        Assert.assertEquals( "Test Description.", resource.getDescription() );
        Assert.assertEquals( "test-hint", resource.getRoleHint() );
    }

    @Test
    public void testNullDescriptionAndHint()
        throws Exception
    {
        PlexusComponentListResourceResponse result = runGetForRole( "TEST_NULL" );

        Assert.assertTrue( result.getData().size() == 1 );

        PlexusComponentListResource resource = (PlexusComponentListResource) result.getData().get( 0 );

        Assert.assertEquals( "default", resource.getDescription() );
        Assert.assertEquals( "default", resource.getRoleHint() );
    }

    @Test
    public void testEmptyDescriptionAndHint()
        throws Exception
    {
        PlexusComponentListResourceResponse result = runGetForRole( "TEST_EMPTY" );

        Assert.assertTrue( result.getData().size() == 1 );

        PlexusComponentListResource resource = (PlexusComponentListResource) result.getData().get( 0 );

        Assert.assertEquals( "default", resource.getDescription() );
        Assert.assertEquals( "default", resource.getRoleHint() );
    }
}
