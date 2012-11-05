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
package org.sonatype.nexus.integrationtests.webproxy;

import org.sonatype.jettytestsuite.ProxyServer;
import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.test.utils.TestProperties;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public abstract class AbstractNexusWebProxyIntegrationTest
    extends AbstractNexusProxyIntegrationTest
{

    protected static final int webProxyPort;

    protected ProxyServer server;

    static
    {
        webProxyPort = TestProperties.getInteger( "webproxy.server.port" );
    }

    @BeforeMethod(alwaysRun = true)
    public void startWebProxy()
        throws Exception
    {
        server = lookup( ProxyServer.class );
        server.start();
    }

    @AfterMethod(alwaysRun = true)
    public void stopWebProxy()
        throws Exception
    {
        if ( server != null )
        {
            server.stop();
        }
    }

}
