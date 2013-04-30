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
package org.sonatype.nexus.integrationtests.webproxy.nexus1113;

import java.io.File;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonatype.nexus.integrationtests.ITGroups.PROXY;
import org.sonatype.nexus.integrationtests.webproxy.AbstractNexusWebProxyIntegrationTest;
import org.sonatype.nexus.test.utils.FileTestingUtils;

public class Nexus1113WebProxyWithAuthenticationIT
    extends AbstractNexusWebProxyIntegrationTest
{

    @Override
    @Before
    public void startWebProxy()
        throws Exception
    {
        super.startWebProxy();
        server.getProxyServlet().setUseAuthentication( true );
        server.getProxyServlet().getAuthentications().put( "admin", "123" );
    }

    @Test
    @Category( PROXY.class )
    public void downloadArtifactOverWebProxy()
        throws Exception
    {
        File pomFile = this.getLocalFile( "release-proxy-repo-1", "nexus1113", "artifact", "1.0", "pom" );
        File pomArtifact =
            this.downloadArtifact( "nexus1113", "artifact", "1.0", "pom", null, "target/downloads/nexus1113" );
        Assert.assertTrue( FileTestingUtils.compareFileSHA1s( pomArtifact, pomFile ) );

        File jarFile = this.getLocalFile( "release-proxy-repo-1", "nexus1113", "artifact", "1.0", "jar" );
        File jarArtifact =
            this.downloadArtifact( "nexus1113", "artifact", "1.0", "jar", null, "target/downloads/nexus1113" );
        Assert.assertTrue( FileTestingUtils.compareFileSHA1s( jarArtifact, jarFile ) );

        String artifactUrl = baseProxyURL + "release-proxy-repo-1/nexus1113/artifact/1.0/artifact-1.0.jar";
        Assert.assertTrue( "Proxy was not accessed", server.getAccessedUris().contains( artifactUrl ) );
    }

    @Override
    @After
    public void stopWebProxy()
        throws Exception
    {
        if ( server != null )
        {
            if ( server.getProxyServlet() != null )
            {
                server.getProxyServlet().setUseAuthentication( false );
                server.getProxyServlet().setAuthentications( null );
            }
        }

        super.stopWebProxy();
    }
}
