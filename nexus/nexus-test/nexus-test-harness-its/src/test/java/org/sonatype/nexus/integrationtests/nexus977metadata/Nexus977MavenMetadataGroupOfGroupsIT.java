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
package org.sonatype.nexus.integrationtests.nexus977metadata;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.List;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.codehaus.plexus.util.IOUtil;
import org.hamcrest.MatcherAssert;
import static org.hamcrest.Matchers.hasItems;
import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.maven.tasks.descriptors.RebuildMavenMetadataTaskDescriptor;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataBuilder;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.testng.annotations.Test;

public class Nexus977MavenMetadataGroupOfGroupsIT
    extends AbstractNexusProxyIntegrationTest
{

    @Override
    protected void runOnce()
        throws Exception
    {
        super.runOnce();

        ScheduledServicePropertyResource repo = new ScheduledServicePropertyResource();
        repo.setKey( "repositoryId" );
        repo.setValue( "release" );
        TaskScheduleUtil.runTask( "RebuildMavenMetadata-release", RebuildMavenMetadataTaskDescriptor.ID, repo );

        repo = new ScheduledServicePropertyResource();
        repo.setKey( "repositoryId" );
        repo.setValue( "release2" );
        TaskScheduleUtil.runTask( "RebuildMavenMetadata-release2", RebuildMavenMetadataTaskDescriptor.ID, repo );

        repo = new ScheduledServicePropertyResource();
        repo.setKey( "repositoryId" );
        repo.setValue( "snapshot" );
        TaskScheduleUtil.runTask( "RebuildMavenMetadata-snapshot", RebuildMavenMetadataTaskDescriptor.ID, repo );
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void checkMetadata()
        throws Exception
    {
        File metadataFile =
            downloadFile( new URL( nexusBaseUrl + "content/repositories/g4/"
                + "nexus977metadata/project/maven-metadata.xml" ), "target/downloads/nexus977" );

        final FileInputStream in = new FileInputStream( metadataFile );
        Metadata metadata = MetadataBuilder.read( in );
        IOUtil.close( in );

        List<String> versions = metadata.getVersioning().getVersions();
        MatcherAssert.assertThat( versions, hasItems( "1.5", "1.0.1", "1.0-SNAPSHOT", "0.8", "2.1" ) );
    }
}
