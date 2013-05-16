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
package org.sonatype.nexus.client.testsuite;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.sonatype.nexus.client.core.subsystem.content.Location.repositoryLocation;
import static org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers.matchSha1;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonatype.nexus.client.core.exception.NexusClientNotFoundException;
import org.sonatype.nexus.client.core.subsystem.content.Content;
import org.sonatype.nexus.client.core.subsystem.content.Location;

import com.google.common.collect.Maps;

public class ContentIT
    extends NexusClientITSupport
{

    private static final String AOP_POM_PARENT = "aopalliance/aopalliance/1.0/";

    private static final String AOP_POM = AOP_POM_PARENT + "aopalliance-1.0.pom";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    public ContentIT( final String nexusBundleCoordinates )
    {
        super( nexusBundleCoordinates );
    }

    @Test
    public void successfulUploadAndDownloadAndDelete()
        throws IOException
    {
        final Location location = repositoryLocation( "releases", AOP_POM );

        final File toDeploy = testData().resolveFile( "artifacts/" + AOP_POM );
        final File downloaded = new File( testIndex().getDirectory( "downloads" ), "aopalliance-1.0.pom" );

        content().upload( location, toDeploy );
        content().download( location, downloaded );

        assertThat( downloaded, matchSha1( toDeploy ) );

        content().delete( location );
    }

    @Test
    public void successfulUploadWithAttributeAndDescribe()
        throws IOException
    {
        final Location location = repositoryLocation( "releases", AOP_POM );

        final File toDeploy = testData().resolveFile( "artifacts/" + AOP_POM );

        final Map<String, String> tags = Maps.newHashMap();
        tags.put( "foo", "bar" );
        tags.put( "one", "1" );
        tags.put( "null", null ); // this one not added: value is null
        tags.put( "empty", "" ); // this one not added: value is empty
        tags.put( "", "emptyKey" ); // this one not added: key is empty
        tags.put( null, "nullKey" ); // this one not added: key is null
        content().uploadWithAttributes( location, toDeploy, tags );

        final Map<String, String> attributes = content().getFileAttributes( location );

        assertThat( attributes, notNullValue() );
        // core in hosted reposes has 16 attributes, we added 2 above
        assertThat( attributes.size(), equalTo( 18 ) );
        assertThat( attributes.get( "deploy.foo" ), equalTo( "bar" ) );
        assertThat( attributes.get( "deploy.one" ), equalTo( "1" ) );
        // by the fact we counted the map size, and checked our extra 2
        // we verified that other attributes did not get in, still
        // some extra (and redundant) assertions:
        assertThat( "Key should not be added", !attributes.containsKey( "null" ) );
        assertThat( "Key should not be added", !attributes.containsKey( "empty" ) );
        assertThat( "Key should not be added", !attributes.containsKey( "" ) );
        assertThat( "Key should not be added", !attributes.containsKey( null ) );

        // need to clean up as this IT uses per-class Nexus instance
        content().delete( location );
    }

    @Test( expected = IllegalArgumentException.class )
    public void successfulUploadWithAttributeAndDescribeOfParentDirectory()
        throws IOException
    {
        // upload to make sure parent is created
        final Location location = repositoryLocation( "releases", AOP_POM );
        final File toDeploy = testData().resolveFile( "artifacts/" + AOP_POM );
        content().upload( location, toDeploy );

        try
        {
            // this should throw IAEx as we are targeting a directory
            content().getFileAttributes( repositoryLocation( "releases", AOP_POM_PARENT ) );
        }
        finally
        {
            // need to clean up as this IT uses per-class Nexus instance
            content().delete( location );
        }
    }

    @Test( expected = NexusClientNotFoundException.class )
    public void describeOfNonexistentPath()
        throws IOException
    {
        content().getFileAttributes( repositoryLocation( "releases", AOP_POM_PARENT + "nonexistent.jar" ) );
    }

    @Test
    public void wrongUploadLocation()
        throws IOException
    {
        thrown.expect( NexusClientNotFoundException.class );
        thrown.expectMessage(
            "Inexistent path: repositories/foo/aopalliance/aopalliance/1.0/aopalliance-1.0.pom"
        );
        content().upload( repositoryLocation( "foo", AOP_POM ), testData().resolveFile( "artifacts/" + AOP_POM ) );
    }

    @Test
    public void wrongDownloadLocation()
        throws IOException
    {
        thrown.expect( NexusClientNotFoundException.class );
        thrown.expectMessage(
            "Inexistent path: repositories/foo/aopalliance/aopalliance/1.0/aopalliance-1.0.pom"
        );
        content().download(
            repositoryLocation( "foo", AOP_POM ),
            new File( testIndex().getDirectory( "downloads" ), "aopalliance-1.0.pom" )
        );
    }

    @Test
    public void wrongDeleteLocation()
        throws IOException
    {
        thrown.expect( NexusClientNotFoundException.class );
        thrown.expectMessage(
            "Inexistent path: repositories/foo/aopalliance/aopalliance/1.0/aopalliance-1.0.pom"
        );
        content().delete( repositoryLocation( "foo", AOP_POM ) );
    }

    protected Content content()
    {
        return client().getSubsystem( Content.class );
    }

}
