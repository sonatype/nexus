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
package org.sonatype.nexus.integrationtests.nexus176;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.maven.index.artifact.Gav;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * Tests for 400 when uploading to a non-existing repository.
 */
public class Nexus176DeployToInvalidRepoIT
    extends AbstractNexusIntegrationTest
{

    private static final String TEST_RELEASE_REPO = "invalid-release";

    public Nexus176DeployToInvalidRepoIT()
    {
        super( TEST_RELEASE_REPO );
    }
    
    @BeforeClass
    public void setSecureTest(){
        TestContainer.getInstance().getTestContext().setSecureTest( true );
    }

    @Test
    public void wagonDeployTest()
        throws Exception
    {
       
        Gav gav =
            new Gav( this.getTestId(), "simpleArtifact", "1.0.0", null, "xml", 0,
                     new Date().getTime(), "Simple Test Artifact", false, null, false, null );

        // file to deploy
        File fileToDeploy =
            this.getTestFile( gav.getArtifactId() + "." + gav.getExtension() );
        
     // url to upload to
        String uploadURL = this.getBaseNexusUrl() + "service/local/artifact/maven/content";

        // the method we are calling
        PostMethod filePost = new PostMethod( uploadURL );
        filePost.getParams().setBooleanParameter( HttpMethodParams.USE_EXPECT_CONTINUE, true );

        int status = getDeployUtils().deployUsingGavWithRest( uploadURL, TEST_RELEASE_REPO, gav, fileToDeploy );

        if ( status != HttpStatus.SC_NOT_FOUND )
        {
            Assert.fail( "Upload attempt should have returned a 400, it returned:  "+ status);
        }
      
        boolean fileWasUploaded = true;
        try
        {
          // download it
          downloadArtifact( gav, "./target/downloaded-jars" );
        }
        catch(FileNotFoundException e)
        {
            fileWasUploaded = false;
        }
        
        Assert.assertFalse( fileWasUploaded, "The file was uploaded and it should not have been." );

    }


    @Test
    public void deploywithGavUsingRest()
        throws Exception
    {

        Gav gav =
            new Gav( this.getTestId(), "uploadWithGav", "1.0.0", null, "xml", 0,
                     new Date().getTime(), "Simple Test Artifact", false, null, false, null  );

        // file to deploy
        File fileToDeploy =
            this.getTestFile( gav.getArtifactId() + "." + gav.getExtension() );

        // the Restlet Client does not support multipart forms: http://restlet.tigris.org/issues/show_bug.cgi?id=71

        // url to upload to
        String uploadURL = this.getBaseNexusUrl() + "service/local/artifact/maven/content";

        // the method we are calling
        PostMethod filePost = new PostMethod( uploadURL );
        filePost.getParams().setBooleanParameter( HttpMethodParams.USE_EXPECT_CONTINUE, true );

        int status = getDeployUtils().deployUsingGavWithRest( uploadURL, TEST_RELEASE_REPO, gav, fileToDeploy );

        if ( status != HttpStatus.SC_NOT_FOUND )
        {
            Assert.fail( "Upload attempt should have returned a 400, it returned:  "+ status);
        }
      
        boolean fileWasUploaded = true;
        try
        {
          // download it
          downloadArtifact( gav, "./target/downloaded-jars" );
        }
        catch(FileNotFoundException e)
        {
            fileWasUploaded = false;
        }
        
        Assert.assertFalse( fileWasUploaded, "The file was uploaded and it should not have been." );

    }
    
    

    @Test
    public void deploywithPomUsingRest()
        throws Exception
    {

        Gav gav =
            new Gav( this.getTestId(), "uploadWithGav", "1.0.0", null, "xml", 0,
                     new Date().getTime(), "Simple Test Artifact", false, null, false, null  );

        // file to deploy
        File fileToDeploy =
            this.getTestFile( gav.getArtifactId() + "." + gav.getExtension() );
        
        File pomFile =
            this.getTestFile( "pom.xml" );

        // the Restlet Client does not support multipart forms: http://restlet.tigris.org/issues/show_bug.cgi?id=71

        // url to upload to
        String uploadURL = this.getBaseNexusUrl() + "service/local/artifact/maven/content";
            
        int status = getDeployUtils().deployUsingPomWithRest( uploadURL, TEST_RELEASE_REPO, fileToDeploy, pomFile, null, null );
        
        if ( status != HttpStatus.SC_NOT_FOUND )
        {
            Assert.fail( "Upload attempt should have returned a 400, it returned:  "+ status);
        }
      
        boolean fileWasUploaded = true;
        try
        {
          // download it
          downloadArtifact( gav, "./target/downloaded-jars" );
        }
        catch(FileNotFoundException e)
        {
            fileWasUploaded = false;
        }
        
        Assert.assertFalse( fileWasUploaded, "The file was uploaded and it should not have been." );

    }
    
    
    
    @Test
    public void wagonSnapshotDeployTest()
        throws Exception
    {
       
        Gav gav =
            new Gav( this.getTestId(), "simpleArtifact", "1.0.0-SNAPSHOT", null, "xml", 0,
                     new Date().getTime(), "Simple Test Artifact", false, null, false, null  );

        // file to deploy
        File fileToDeploy =
            this.getTestFile( gav.getArtifactId() + "." + gav.getExtension() );
        
     // url to upload to
        String uploadURL = this.getBaseNexusUrl() + "service/local/artifact/maven/content";

        // the method we are calling
        PostMethod filePost = new PostMethod( uploadURL );
        filePost.getParams().setBooleanParameter( HttpMethodParams.USE_EXPECT_CONTINUE, true );

        int status = getDeployUtils().deployUsingGavWithRest( uploadURL, TEST_RELEASE_REPO, gav, fileToDeploy );

        if ( status != HttpStatus.SC_NOT_FOUND )
        {
            Assert.fail( "Upload attempt should have returned a 400, it returned:  "+ status);
        }
      
        boolean fileWasUploaded = true;
        try
        {
          // download it
          downloadArtifact( gav, "./target/downloaded-jars" );
        }
        catch(FileNotFoundException e)
        {
            fileWasUploaded = false;
        }
        
        Assert.assertFalse( fileWasUploaded, "The file was uploaded and it should not have been." );

    }


    @Test
    public void deploySnapshotWithGavUsingRest()
        throws Exception
    {

        Gav gav =
            new Gav( this.getTestId(), "uploadWithGav", "1.0.0-SNAPSHOT", null, "xml", 0,
                     new Date().getTime(), "Simple Test Artifact", false, null, false, null  );

        // file to deploy
        File fileToDeploy =
            this.getTestFile( gav.getArtifactId() + "." + gav.getExtension() );

        // the Restlet Client does not support multipart forms: http://restlet.tigris.org/issues/show_bug.cgi?id=71

        // url to upload to
        String uploadURL = this.getBaseNexusUrl() + "service/local/artifact/maven/content";

        // the method we are calling
        PostMethod filePost = new PostMethod( uploadURL );
        filePost.getParams().setBooleanParameter( HttpMethodParams.USE_EXPECT_CONTINUE, true );

        int status = getDeployUtils().deployUsingGavWithRest( uploadURL, TEST_RELEASE_REPO, gav, fileToDeploy );

        if ( status != HttpStatus.SC_NOT_FOUND )
        {
            Assert.fail( "Upload attempt should have returned a 400, it returned:  "+ status);
        }
      
        boolean fileWasUploaded = true;
        try
        {
          // download it
          downloadArtifact( gav, "./target/downloaded-jars" );
        }
        catch(FileNotFoundException e)
        {
            fileWasUploaded = false;
        }
        
        Assert.assertFalse( fileWasUploaded, "The file was uploaded and it should not have been." );

    }
    
    

    @Test
    public void deploySnapshotWithPomUsingRest()
        throws Exception
    {

        Gav gav =
            new Gav( this.getTestId(), "uploadWithGav", "1.0.0-SNAPSHOT", null, "xml", 0,
                     new Date().getTime(), "Simple Test Artifact", false, null, false, null  );

        // file to deploy
        File fileToDeploy =
            this.getTestFile( gav.getArtifactId() + "." + gav.getExtension() );
        
        File pomFile =
            this.getTestFile( "pom.xml" );

        // the Restlet Client does not support multipart forms: http://restlet.tigris.org/issues/show_bug.cgi?id=71

        // url to upload to
        String uploadURL = this.getBaseNexusUrl() + "service/local/artifact/maven/content";
            
        int status = getDeployUtils().deployUsingPomWithRest( uploadURL, TEST_RELEASE_REPO, fileToDeploy, pomFile, null, null );
        
        if ( status != HttpStatus.SC_NOT_FOUND )
        {
            Assert.fail( "Upload attempt should have returned a 400, it returned:  "+ status);
        }
      
        boolean fileWasUploaded = true;
        try
        {
          // download it
          downloadArtifact( gav, "./target/downloaded-jars" );
        }
        catch(FileNotFoundException e)
        {
            fileWasUploaded = false;
        }
        
        Assert.assertFalse( fileWasUploaded, "The file was uploaded and it should not have been." );

    }
    
  
    
}
