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
package org.sonatype.nexus.integrationtests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;

/**
 * Helper class to create Maven Verifier.
 * 
 * @author cstamas
 * @since 2.1
 */
public class MavenVerifierHelper
{
    /**
     * Creates a Verifier against passed in {@link MavenDeployment}.
     * 
     * @param mavenDeployment
     * @return
     * @throws VerificationException
     * @throws IOException
     */
    public Verifier createMavenVerifier( final MavenDeployment mavenDeployment )
        throws VerificationException, IOException
    {
        System.setProperty( "maven.home", mavenDeployment.getMavenHomeFile().getAbsolutePath() );
        Verifier verifier = new Verifier( mavenDeployment.getMavenProjectFile().getAbsolutePath() )
        {
            @Override
            public void executeGoals(List<String> goals, Map envVars) throws VerificationException {
                try {
                    super.executeGoals(goals, envVars);
                }
                catch (VerificationException e) {
                    // HACK: Strip out the entire log which is included in the message by default! :-(
                    File logFile = new File( getBasedir(), getLogFileName() );
                    throw new VerificationException("Goals execution failed: " + goals + "; see log for more details: " + logFile.getAbsolutePath(), e.getCause());
                }
            }
        };
        verifier.setLogFileName( mavenDeployment.getLogFileName() );
        verifier.setLocalRepo( mavenDeployment.getLocalRepositoryFile().getAbsolutePath() );
        verifier.resetStreams();

        List<String> options = new ArrayList<String>();
        options.add( "-X" );
        options.add( "-Dmaven.repo.local=" + mavenDeployment.getLocalRepositoryFile().getAbsolutePath() );
        options.add( "-s " + mavenDeployment.getSettingsXmlFile().getAbsolutePath() );
        verifier.setCliOptions( options );
        return verifier;
    }

    /**
     * Removes artifacts from passed in local repository that has groupId of testId.
     * 
     * @param mavenRepo
     * @param testId
     * @throws IOException
     */
    protected void cleanRepository( final File mavenRepo, final String testId )
        throws IOException
    {
        final File testGroupIdFolder = new File( mavenRepo, testId );
        FileUtils.deleteDirectory( testGroupIdFolder );
    }

    /**
     * Creates a "failure" message spitting out the Verifier execution log. This methods does not check any assertion,
     * but should be rather call when you already checked and assertion, you know it is failed, and all you want to fail
     * the test with some extra logging.
     * 
     * @param verifier
     * @throws IOException
     *
     * @deprecated This method will make huge/confusing output on build execution, and should not be used.
     */
    @Deprecated
    public void failTest( final Verifier verifier )
        throws IOException
    {
        final File logFile = new File( verifier.getLogFileName() );
        final String log = FileUtils.fileRead( logFile );
        Assert.fail( log );
    }
}
