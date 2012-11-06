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
package org.sonatype.nexus.integrationtests.nexus175;

import static org.sonatype.nexus.integrationtests.ITGroups.SECURITY;

import java.io.File;
import java.util.Date;

import org.apache.maven.index.artifact.Gav;
import org.apache.maven.it.VerificationException;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.test.utils.MavenDeployer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test to make sure invalid password do not allow artifacts to be deployed.
 */
public class Nexus175SnapshotDeployWrongPasswordIT
    extends AbstractNexusIntegrationTest
{

    private static final String TEST_RELEASE_REPO = "nexus-test-harness-release-repo";

    public Nexus175SnapshotDeployWrongPasswordIT()
    {
        super( TEST_RELEASE_REPO );
    }
    
    @BeforeClass(alwaysRun = true)
    public void setSecureTest(){
        TestContainer.getInstance().getTestContext().setSecureTest( true );
    }

    @Test(groups = SECURITY)
    public void deployWithMaven()
        throws Exception
    {

        // GAV
        Gav gav =
            new Gav( this.getTestId(), "artifact", "1.0.0-SNAPSHOT", null, "xml", 0, new Date().getTime(), "", 
                     false, null, false, null );

        // file to deploy
        File fileToDeploy = this.getTestFile( gav.getArtifactId() + "." + gav.getExtension() );

        // we need to delete the files...
        this.deleteFromRepository( this.getTestId() + "/" );

        try
        {
            // DeployUtils.forkDeployWithWagon( this.getContainer(), "http", this.getNexusTestRepoUrl(), fileToDeploy,
            // this.getRelitiveArtifactPath( gav ));
            MavenDeployer.deployAndGetVerifier( gav, this.getNexusTestRepoUrl(), fileToDeploy,
                                                this.getOverridableFile( "settings.xml" ) );
            Assert.fail( "File should NOT have been deployed " );
        }
        // catch ( TransferFailedException e )
        // {
        // // expected 401
        // }
        catch ( VerificationException e )
        {
            // expected 401
            // MavenDeployer, either fails or not, we can't check the cause of the problem
        }

    }

}
