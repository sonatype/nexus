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
package org.sonatype.nexus.integrationtests.proxy.nexus262;

import static org.sonatype.nexus.integrationtests.ITGroups.PROXY;

import java.io.File;
import java.io.IOException;

import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.test.utils.FileTestingUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * One step above the Sample Test, this one adds a 'remote repository': <a href='https://docs.sonatype.com/display/NX/Nexus+Test-Harness'>Nexus Test-Harness</a>
 */
public class Nexus262SimpleProxyIT extends AbstractNexusProxyIntegrationTest
{

    public Nexus262SimpleProxyIT()
    {
        super( "release-proxy-repo-1" );
    }
    
    @BeforeClass(alwaysRun = true)
    public void setSecureTest(){
        TestContainer.getInstance().getTestContext().setSecureTest( true );
    }
    
    @Test(groups = PROXY)
    public void downloadFromProxy() throws IOException
    {
        File localFile = this.getLocalFile( "release-proxy-repo-1", "simple.artifact", "simpleXMLArtifact", "1.0.0", "xml" );
                                                                                              
        log.debug( "localFile: "+ localFile.getAbsolutePath() );
        
        File artifact = this.downloadArtifact( "simple.artifact", "simpleXMLArtifact", "1.0.0", "xml", null, "target/downloads" );
        
        Assert.assertTrue( FileTestingUtils.compareFileSHA1s( artifact, localFile ) );
    }

}
