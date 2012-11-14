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
package org.sonatype.nexus.client.testsuite;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.sonatype.nexus.client.core.subsystem.content.Location.repositoryLocation;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonatype.nexus.client.core.NotFoundException;
import org.sonatype.nexus.client.core.subsystem.artifact.ArtifactMaven;
import org.sonatype.nexus.client.core.subsystem.artifact.DeleteRequest;
import org.sonatype.nexus.client.core.subsystem.artifact.ResolveRequest;
import org.sonatype.nexus.client.core.subsystem.artifact.ResolveResponse;
import org.sonatype.nexus.client.core.subsystem.artifact.UploadRequest;
import org.sonatype.nexus.client.core.subsystem.content.Content;
import org.sonatype.nexus.client.core.subsystem.repository.Repositories;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenHostedRepository;

/**
 * @since 2.3
 */
public class ArtifactMavenIT
    extends NexusClientITSupport
{

    private static final String AOP_POM = "aopalliance/aopalliance/1.0/aopalliance-1.0.pom";

    private static final String AOP_JAR = "aopalliance/aopalliance/1.0/aopalliance-1.0.jar";

    private static final String AOP_META = "aopalliance/aopalliance/maven-metadata.xml";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    public ArtifactMavenIT( final String nexusBundleCoordinates )
    {
        super( nexusBundleCoordinates );
    }

    @Test
    public void artifactMavenResolveSuccess()
        throws IOException
    {
        final MavenHostedRepository repository =
            repositories().create( MavenHostedRepository.class, repositoryIdForTest() )
                .excludeFromSearchResults()
                .save();

        contentUpload( repository.id(), AOP_POM );
        contentUpload( repository.id(), AOP_JAR );
        contentUpload( repository.id(), AOP_META );

        final ResolveResponse response = artifacts().resolve(
            new ResolveRequest(
                repository.id(), "aopalliance", "aopalliance", ResolveRequest.VERSION_RELEASE
            )
        );
        assertThat( response, is( notNullValue() ) );
        assertThat( response.getGroupId(), is( "aopalliance" ) );
        assertThat( response.getArtifactId(), is( "aopalliance" ) );
        assertThat( response.getExtension(), is( "jar" ) );
        assertThat( response.isSnapshot(), is( false ) );
    }

    @Test
    public void artifactMavenResolveFailure()
    {
        thrown.expect( NotFoundException.class );
        artifacts().resolve(
            new ResolveRequest(
                "releases", "com.sonatype.nexus.plugin", "nexus-staging-plugin", ResolveRequest.VERSION_RELEASE
            )
        );
    }

    @Test
    public void uploadWithCoordinates()
    {
        final MavenHostedRepository repository =
            repositories().create( MavenHostedRepository.class, repositoryIdForTest() )
                .excludeFromSearchResults()
                .save();

        artifacts().upload(
            UploadRequest.artifact( repository.id(), "aopalliance", "aopalliance", "1.0" )
                .attach( testData().resolveFile( "artifacts/" + AOP_JAR ) )
        );
    }

    @Test
    public void uploadWithPom()
    {
        final MavenHostedRepository repository =
            repositories().create( MavenHostedRepository.class, repositoryIdForTest() )
                .excludeFromSearchResults()
                .save();

        artifacts().upload(
            UploadRequest.pom( repository.id(), testData().resolveFile( "artifacts/" + AOP_POM ) )
                .attach( testData().resolveFile( "artifacts/" + AOP_JAR ) )
        );
    }

    @Test
    public void delete()
        throws IOException
    {
        final MavenHostedRepository repository =
            repositories().create( MavenHostedRepository.class, repositoryIdForTest() )
                .excludeFromSearchResults()
                .save();

        contentUpload( repository.id(), AOP_POM );
        contentUpload( repository.id(), AOP_JAR );
        contentUpload( repository.id(), AOP_META );

        artifacts().delete(
            new DeleteRequest( repository.id() )
                .withGroupId( "aopalliance" )
                .withArtifactId( "aopalliance" )
                .withVersion( "1.0" )
                .withExtension( "jar" )
        );
        artifacts().delete(
            new DeleteRequest( repository.id() )
                .withGroupId( "aopalliance" )
                .withArtifactId( "aopalliance" )
                .withVersion( "1.0" )
        );
        artifacts().delete(
            new DeleteRequest( repository.id() )
                .withGroupId( "aopalliance" )
                .withArtifactId( "aopalliance" )
        );
        artifacts().delete(
            new DeleteRequest( repository.id() )
                .withGroupId( "aopalliance" )
        );
        artifacts().delete(
            new DeleteRequest( repository.id() )
        );
    }

    private void contentUpload( final String repositoryId, final String path )
        throws IOException
    {
        content().upload( repositoryLocation( repositoryId, path ), testData().resolveFile( "artifacts/" + path ) );
    }

    private ArtifactMaven artifacts()
    {
        return client().getSubsystem( ArtifactMaven.class );
    }

    private Content content()
    {
        return client().getSubsystem( Content.class );
    }

    private Repositories repositories()
    {
        return client().getSubsystem( Repositories.class );
    }

}
