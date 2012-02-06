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
package org.sonatype.nexus.integrationtests;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.test.utils.ResponseMatchers.isRedirecting;
import static org.sonatype.nexus.test.utils.ResponseMatchers.redirectLocation;
import static org.sonatype.nexus.test.utils.ResponseMatchers.respondsWithStatusCode;
import static org.testng.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.index.artifact.Gav;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.wagon.Wagon;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.sonatype.nexus.rest.model.GlobalConfigurationResource;
import org.sonatype.nexus.rt.prefs.FilePreferencesFactory;
import org.sonatype.nexus.test.utils.DeployUtils;
import org.sonatype.nexus.test.utils.EventInspectorsUtil;
import org.sonatype.nexus.test.utils.FileTestingUtils;
import org.sonatype.nexus.test.utils.GavUtil;
import org.sonatype.nexus.test.utils.MavenProjectFileFilter;
import org.sonatype.nexus.test.utils.NexusConfigUtil;
import org.sonatype.nexus.test.utils.NexusStatusUtil;
import org.sonatype.nexus.test.utils.SearchMessageUtil;
import org.sonatype.nexus.test.utils.SecurityConfigUtil;
import org.sonatype.nexus.test.utils.SettingsMessageUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.sonatype.nexus.test.utils.TestProperties;
import org.sonatype.nexus.test.utils.WagonDeployer;
import org.sonatype.nexus.test.utils.XStreamFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import com.google.common.base.Throwables;
import com.google.common.io.Closeables;
import com.thoughtworks.xstream.XStream;

/**
 * curl --user admin:admin123 --request PUT http://localhost:8081/nexus/service/local/status/command --data START NOTE,
 * this class is not really abstract so I can work around a the <code>@BeforeClass</code>, <code>@AfterClass</code>
 * issues, this should be refactored a little, but it might be ok, if we switch to TestNg
 */
// @RunWith(ConsoleLoggingRunner.class)
public abstract class AbstractNexusIntegrationTest
{

    public static final String REPO_NEXUS_TEST_HARNESS_RELEASE_GROUP = "nexus-test-harness-release-group";

    public static final String REPO_TEST_HARNESS_REPO = "nexus-test-harness-repo";

    public static final String REPO_TEST_HARNESS_REPO2 = "nexus-test-harness-repo2";

    public static final String REPO_TEST_HARNESS_RELEASE_REPO = "nexus-test-harness-release-repo";

    public static final String REPO_TEST_HARNESS_SNAPSHOT_REPO = "nexus-test-harness-snapshot-repo";

    public static final String REPO_RELEASE_PROXY_REPO1 = "release-proxy-repo-1";

    public static final String REPO_TEST_HARNESS_SHADOW = "nexus-test-harness-shadow";

    protected static boolean NEEDS_INIT = false;

    public static final String REPOSITORY_RELATIVE_URL = "content/repositories/";

    /**
     * relative path from {@link #nexusBaseDir} to plugin-repository
     */
    public static final String RELATIVE_PLUGIN_REPOSITORY_DIR = "nexus/WEB-INF/plugin-repository";

    public static final String GROUP_REPOSITORY_RELATIVE_URL = "content/groups/";

    public String testRepositoryId;

    public static String nexusBaseDir;

    public static final String nexusBaseUrl;

    public static final String LIx1 = "  ";

    public static final String LIx2 = "    ";

    /**
     * @deprecated Use nexusBaseUrl instead!
     */
    @Deprecated
    public static final String baseNexusUrl;

    public static final String nexusWorkDir;

    public static final String RELATIVE_CONF_DIR = "runtime/apps/nexus/conf";

    public static final String WORK_CONF_DIR;

    public static final Integer nexusControlPort;

    public static final int nexusApplicationPort;

    protected static final String nexusLogDir;

    protected Logger log = LoggerFactory.getLogger( getClass() );

    protected static Logger staticLog = LoggerFactory.getLogger( AbstractNexusIntegrationTest.class );

    private Properties systemPropertiesBackup;

    static
    {
        // Install the file preferences, to have them used from IT but also from embedded Nexus too.
        System.setProperty( "java.util.prefs.PreferencesFactory", FilePreferencesFactory.class.getName() );

        // guice finalizer turned OFF
        System.setProperty( "guice.executor.class", "NONE" );
    }

    static
    {
        nexusApplicationPort = TestProperties.getInteger( "nexus.application.port" );
        nexusControlPort = TestProperties.getInteger( "nexus.control.port" );
        nexusBaseDir = TestProperties.getString( "nexus.base.dir" );
        nexusWorkDir = TestProperties.getString( "nexus.work.dir" );
        WORK_CONF_DIR = nexusWorkDir + "/conf";
        nexusLogDir = TestProperties.getString( "nexus.log.dir" );
        nexusBaseUrl = TestProperties.getString( "nexus.base.url" );
        baseNexusUrl = nexusBaseUrl;

        TestContainer.getInstance().getTestContext().setNexusUrl( nexusBaseUrl );

        // configure the logging
        SLF4JBridgeHandler.install();
    }

    // == instance utils

    private static NexusStatusUtil nexusStatusUtil;

    public static NexusStatusUtil getNexusStatusUtil()
        throws Exception
    {
        if ( nexusStatusUtil == null )
        {
            nexusStatusUtil = new NexusStatusUtil( nexusApplicationPort );
        }

        return nexusStatusUtil;
    }

    private NexusConfigUtil nexusConfigUtil;

    public NexusConfigUtil getNexusConfigUtil()
    {
        if ( nexusConfigUtil == null )
        {
            nexusConfigUtil = new NexusConfigUtil( this );
        }

        return nexusConfigUtil;
    }

    private SecurityConfigUtil securityConfigUtil;

    public SecurityConfigUtil getSecurityConfigUtil()
    {
        if ( securityConfigUtil == null )
        {
            securityConfigUtil = new SecurityConfigUtil( this );
        }

        return securityConfigUtil;
    }

    private DeployUtils deployUtils;

    public DeployUtils getDeployUtils()
    {
        if ( deployUtils == null )
        {
            deployUtils = new DeployUtils( RequestFacade.getNexusRestClient(), new WagonDeployer.Factory()
            {
                @Override
                public Wagon get( final String protocol )
                {
                    try
                    {
                        return (Wagon) getITPlexusContainer().lookup( Wagon.ROLE, protocol );
                    }
                    catch ( ComponentLookupException e )
                    {
                        throw Throwables.propagate( e );
                    }
                }
            } );
        }

        return deployUtils;
    }

    private SearchMessageUtil searchMessageUtil;

    public SearchMessageUtil getSearchMessageUtil()
    {
        if ( searchMessageUtil == null )
        {
            searchMessageUtil = new SearchMessageUtil( this );
        }

        return searchMessageUtil;
    }

    private EventInspectorsUtil eventInspectorsUtil;

    public EventInspectorsUtil getEventInspectorsUtil()
    {
        if ( eventInspectorsUtil == null )
        {
            eventInspectorsUtil = new EventInspectorsUtil( RequestFacade.getNexusRestClient() );
        }

        return eventInspectorsUtil;
    }

    // == Constructors

    protected AbstractNexusIntegrationTest()
    {
        this( "nexus-test-harness-repo" );
    }

    protected AbstractNexusIntegrationTest( String testRepositoryId )
    {
        // we also need to setup a couple fields, that need to be pulled out of a bundle
        this.testRepositoryId = testRepositoryId;
        // this.nexusTestRepoUrl = baseNexusUrl + REPOSITORY_RELATIVE_URL + testRepositoryId + "/";

        // redirect filePrefs
        FilePreferencesFactory.setPreferencesFile( getFilePrefsFile( new File( getNexusBaseDir() ), getTestId() ) );
    }

    // ==

    public static File getFilePrefsFile( File dir, String testId )
    {
        return new File( dir, getFilePrefsFileName( testId ) );
    }

    public static String getFilePrefsFileName( String testId )
    {
        return testId + "-filePrefs";
    }

    // == Test "lifecycle" (@Before/@After...)

    @BeforeClass( alwaysRun = true )
    public static void staticOncePerClassSetUp()
        throws Exception
    {
        startProfiler();

        // hacky state machine
        NEEDS_INIT = true;
    }
    
    @BeforeClass( alwaysRun = true )
    public void startITPlexusContainer()
    {
        // start per-IT plexus container
        TestContainer.getInstance().startPlexusContainer( getClass() );
    }

    /**
     * To me this seems like a bad hack around this problem. I don't have any other thoughts though. <BR/>
     * If you see this and think: "Wow, why did he to that instead of XYZ, please let me know." <BR/>
     * The issue is that we want to init the tests once (to start/stop the app) and the <code>@BeforeClass</code> is
     * static, so we don't have access to the package name of the running tests. We are going to use the package name to
     * find resources for additional setup. NOTE: With this setup running multiple Test at the same time is not
     * possible.
     * 
     * @throws Exception
     */
    @BeforeMethod( alwaysRun = true )
    public void oncePerClassSetUp()
        throws Exception
    {
        synchronized ( AbstractNexusIntegrationTest.class )
        {
            if ( NEEDS_INIT )
            {
                systemPropertiesBackup = System.getProperties();

                final String useDebugFor = System.getProperty( "it.nexus.log.level.use.debug" );
                if( !StringUtils.isEmpty( useDebugFor ) )
                {
                    final String[] segments = useDebugFor.split( "," );
                    for ( final String segment : segments )
                    {
                        if(getClass().getSimpleName().matches( segment.replace( ".", "\\." ).replace( "*", ".*" ) ))
                        {
                            System.setProperty( "it.nexus.log.level", "DEBUG" );
                        }
                    }
                }

                // tell the console what we are doing, now that there is no output its
                String logMessage = "Running Test: " + getTestId() + " - Class: " + this.getClass().getSimpleName();
                staticLog.info( String.format( "%1$-" + logMessage.length() + "s", " " ).replaceAll( " ", "*" ) );
                staticLog.info( logMessage );
                staticLog.info( String.format( "%1$-" + logMessage.length() + "s", " " ).replaceAll( " ", "*" ) );

                // clean common work dir
                beforeStartClean();

                copyTestResources();

                this.copyConfigFiles();

                // start nexus
                startNexus();

                final boolean testRequiresSecurityEnabled =
                    TestContainer.getInstance().getTestContext().isSecureTest()
                        || Boolean.valueOf( System.getProperty( "secure.test" ) );

                // set security enabled/disabled as expected by current IT
                TestContainer.getInstance().invokeAsAdministrator( new Callable<Object>()
                {
                    @Override
                    public Object call()
                        throws Exception
                    {
                        final GlobalConfigurationResource globalConfig = SettingsMessageUtil.getCurrentSettings();
                        globalConfig.setSecurityEnabled( testRequiresSecurityEnabled );
                        SettingsMessageUtil.save( globalConfig );
                        return null;
                    }
                } );

                // deploy artifacts
                deployArtifacts();

                runOnce();

                // TODO: we can remove this now that we have the soft restart
                NEEDS_INIT = false;
            }

            getEventInspectorsUtil().waitForCalmPeriod();
        }
    }

    @AfterMethod( alwaysRun = true )
    public void afterTest()
        throws Exception
    {
        // reset this for each test
        TestContainer.getInstance().reset();
        TestContainer.getInstance().getTestContext().setSecureTest( true );
    }

    @AfterClass( alwaysRun = true )
    public void afterClassTearDown()
    {
        System.setProperties( systemPropertiesBackup );
        Nullificator.nullifyMembers( this );
    }

    @AfterClass( alwaysRun = true )
    public static void oncePerClassTearDown()
        throws Exception
    {
        try
        {
            TaskScheduleUtil.waitForAllTasksToStop();
            new EventInspectorsUtil( RequestFacade.getNexusRestClient() ).waitForCalmPeriod();
        }
        catch ( IOException e )
        {
            // throw if server is already stopped, not a problem for me
        }

        // turn off security, of the current IT with security on won't affect the next IT
        TestContainer.getInstance().getTestContext().setSecureTest( false );

        // stop nexus
        stopNexus();

        takeSnapshot();

        // stop per-IT plexus container
        TestContainer.getInstance().stopPlexusContainer();
    }

    protected void runOnce()
        throws Exception
    {
        // must override to happen something
    }

    protected File getNexusLogFile()
    {
        return new File( "target/logs/" + getTestId() + "/nexus.log" );
    }

    protected void beforeStartClean()
        throws Exception
    {
        cleanWorkDir();
    }

    protected void copyTestResources()
        throws IOException
    {
        File source = new File( TestProperties.getString( "test.resources.source.folder" ), getTestId() );
        if ( !source.exists() )
        {
            return;
        }

        File destination = new File( TestProperties.getString( "test.resources.folder" ), getTestId() );

        FileTestingUtils.interpolationDirectoryCopy( source, destination, getTestProperties() );
    }

    protected void copyConfigFiles()
        throws IOException
    {
        Map<String, String> testProperties = getTestProperties();

        this.copyConfigFile( "nexus.xml", testProperties, WORK_CONF_DIR );

        // this is comment out for now, this has been moved into an upgrade step, that will get hit the same system
        // property is set
        // we might need to enable this if we have any nexus.xml with version 1.4.5 (which would NOT hit the upgrade
        // step)
        // now we need to filter the nexus.xml to potentially change the default http provider
        // if( System.getProperty( RemoteProviderHintFactory.DEFAULT_HTTP_PROVIDER_KEY ) != null)
        // {
        // String providerString = "<provider>" + System.getProperty(
        // RemoteProviderHintFactory.DEFAULT_HTTP_PROVIDER_KEY ) + "</provider>";
        // this.findReplaceInFile( new File( WORK_CONF_DIR, "nexus.xml" ), "<provider>apacheHttpClient3x</provider>",
        // providerString );
        // }

        // copy security config
        this.copyConfigFile( "security.xml", testProperties, WORK_CONF_DIR );
        this.copyConfigFile( "security-configuration.xml", testProperties, WORK_CONF_DIR );

        this.copyConfigFile( "logback.properties", testProperties, WORK_CONF_DIR );
        this.copyConfigFile( "logback-nexus.xml", testProperties, WORK_CONF_DIR );
    }

    protected void findReplaceInFile( File file, String findString, String replaceString )
        throws IOException
    {
        BufferedReader bufferedFileReader = new BufferedReader( new FileReader( file ) );
        File tmpFile = new File( file.getAbsolutePath() + "-tmp" );

        FileWriter writer = null;

        try
        {
            writer = new FileWriter( tmpFile );

            String line = null;
            while ( ( line = bufferedFileReader.readLine() ) != null )
            {
                writer.write( line.replaceAll( findString, replaceString ) );
                writer.write( "\n" ); // new line
            }

            // close the streams and move the file
            IOUtil.close( bufferedFileReader );
            IOUtil.close( writer );

            FileUtils.rename( tmpFile, file );
        }
        finally
        {
            IOUtil.close( bufferedFileReader );
            IOUtil.close( writer );
        }

    }

    protected static void cleanWorkDir()
        throws Exception
    {
        final File workDir = new File( AbstractNexusIntegrationTest.nexusWorkDir );

        // to make sure I don't delete all my MP3's and pictures, or totally screw anyone.
        // check for 'target' and not allow any '..'
        if ( workDir.getAbsolutePath().lastIndexOf( "target" ) != -1
            && workDir.getAbsolutePath().lastIndexOf( ".." ) == -1 )
        {
            // we cannot delete the plugin-repository or the tests will fail

            File[] filesToDelete = workDir.listFiles( new FilenameFilter()
            {
                public boolean accept( File dir, String name )
                {
                    // anything but the plugin-repository directory
                    return ( !name.contains( "plugin-repository" ) );
                }
            } );

            if ( filesToDelete != null )
            {
                for ( File fileToDelete : filesToDelete )
                {
                    // delete work dir
                    if ( fileToDelete != null )
                    {
                        FileUtils.forceDelete( fileToDelete );
                    }
                }
            }

        }
    }

    /**
     * Deploys all the provided files needed before IT actually starts.
     * 
     * @throws Exception
     */
    protected void deployArtifacts()
        throws Exception
    {
        // test the test directory
        File projectsDir = getTestResourceAsFile( "projects" );

        deployArtifacts( projectsDir );
    }

    /**
     * This is a "switchboard" to detech HOW to deploy. For now, just using the protocol from POM's
     * DistributionManagement section and invoking the getWagonHintForDeployProtocol(String protocol) to get the wagon
     * hint.
     * 
     * @throws Exception
     */
    protected void deployArtifacts( final File projectsDir )
        throws Exception
    {
        TestContainer.getInstance().getTestContext().useAdminForRequests();

        log.debug( "projectsDir: " + projectsDir );

        // if null there is nothing to deploy...
        if ( projectsDir != null && projectsDir.isDirectory() )
        {

            // we have the parent dir, for each child (one level) we need to grab the pom.xml out of it and parse it,
            // and then deploy the artifact, sounds like fun, right!

            final File[] projectFolders = projectsDir.listFiles( MavenProjectFileFilter.INSTANCE );

            if ( projectFolders == null )
            {
                // bail out
                return;
            }

            // to achieve same ordering on different OSes
            Arrays.sort( projectFolders );

            for ( int ii = 0; ii < projectFolders.length; ii++ )
            {
                File project = projectFolders[ii];

                // we already check if the pom.xml was in here.
                File pom = new File( project, "pom.xml" );

                MavenXpp3Reader reader = new MavenXpp3Reader();
                FileInputStream fis = new FileInputStream( pom );
                Model model = null;
                try
                {
                    model = reader.read( fis );
                }
                finally
                {
                    Closeables.closeQuietly( fis );
                }

                // a helpful note so you don't need to dig into the code to much.
                if ( model.getDistributionManagement() == null
                    || model.getDistributionManagement().getRepository() == null )
                {
                    Assert.fail( "The test artifact is either missing or has an invalid Distribution Management section." );
                }

                // get the URL to deploy
                String deployUrl = model.getDistributionManagement().getRepository().getUrl();

                // get the protocol
                String deployUrlProtocol = deployUrl.substring( 0, deployUrl.indexOf( ":" ) );

                // calculate the wagon hint
                String wagonHint = getWagonHintForDeployProtocol( deployUrlProtocol );

                deployArtifacts( project, wagonHint, deployUrl, model );
            }
        }
    }

    /**
     * Does "protocol to wagon hint" converion: the default is just return the same, but maybe some test wants to
     * override this.
     * 
     * @param deployProtocol
     * @return
     */
    protected String getWagonHintForDeployProtocol( String deployProtocol )
    {
        return deployProtocol;
    }

    /**
     * Deploys with given Wagon (hint is provided), to deployUrl. It is caller matter to adjust those two (ie. deployUrl
     * with file: protocol to be deployed with file wagon would be error). Model is supplied since it is read before.
     * 
     * @param wagonHint
     * @param deployUrl
     * @param model
     * @throws Exception
     */
    protected void deployArtifacts( File project, String wagonHint, String deployUrl, Model model )
        throws Exception
    {
        log.info( "Deploying project \"" + project.getAbsolutePath() + "\" using Wagon:" + wagonHint + " to URL=\""
            + deployUrl + "\"." );

        // we already check if the pom.xml was in here.
        File pom = new File( project, "pom.xml" );

        // FIXME, this needs to be fluffed up a little, should add the classifier, etc.
        String artifactFileName = model.getArtifactId() + "." + model.getPackaging();
        File artifactFile = new File( project, artifactFileName );

        log.debug( "wow, this is working: " + artifactFile.getName() );

        final Gav gav =
            new Gav( model.getGroupId(), model.getArtifactId(), model.getVersion(), null,
                FileUtils.getExtension( artifactFile.getName() ), null, null, artifactFile.getName(), false, null,
                false, null );

        // the Restlet Client does not support multipart forms:
        // http://restlet.tigris.org/issues/show_bug.cgi?id=71

        // int status = DeployUtils.deployUsingPomWithRest( deployUrl, repositoryId, gav, artifactFile, pom );

        if ( !"pom".equals( model.getPackaging() ) && !artifactFile.isFile() )
        {
            throw new FileNotFoundException( "File " + artifactFile.getAbsolutePath() + " doesn't exists!" );
        }

        File artifactSha1 = new File( artifactFile.getAbsolutePath() + ".sha1" );
        File artifactMd5 = new File( artifactFile.getAbsolutePath() + ".md5" );
        File artifactAsc = new File( artifactFile.getAbsolutePath() + ".asc" );

        File pomSha1 = new File( pom.getAbsolutePath() + ".sha1" );
        File pomMd5 = new File( pom.getAbsolutePath() + ".md5" );
        File pomAsc = new File( pom.getAbsolutePath() + ".asc" );

        try
        {
            if ( artifactSha1.exists() )
            {
                getDeployUtils().deployWithWagon( wagonHint, deployUrl, artifactSha1,
                    this.getRelitiveArtifactPath( gav ) + ".sha1" );
            }
            if ( artifactMd5.exists() )
            {
                getDeployUtils().deployWithWagon( wagonHint, deployUrl, artifactMd5,
                    this.getRelitiveArtifactPath( gav ) + ".md5" );
            }
            if ( artifactAsc.exists() )
            {
                getDeployUtils().deployWithWagon( wagonHint, deployUrl, artifactAsc,
                    this.getRelitiveArtifactPath( gav ) + ".asc" );
            }

            if ( artifactFile.exists() )
            {
                getDeployUtils().deployWithWagon( wagonHint, deployUrl, artifactFile,
                    this.getRelitiveArtifactPath( gav ) );
            }

            if ( pomSha1.exists() )
            {
                getDeployUtils().deployWithWagon( wagonHint, deployUrl, pomSha1,
                    this.getRelitivePomPath( gav ) + ".sha1" );
            }
            if ( pomMd5.exists() )
            {
                getDeployUtils().deployWithWagon( wagonHint, deployUrl, pomMd5, this.getRelitivePomPath( gav ) + ".md5" );
            }
            if ( pomAsc.exists() )
            {
                getDeployUtils().deployWithWagon( wagonHint, deployUrl, pomAsc, this.getRelitivePomPath( gav ) + ".asc" );
            }

            getDeployUtils().deployWithWagon( wagonHint, deployUrl, pom, this.getRelitivePomPath( gav ) );
        }
        catch ( Exception e )
        {
            log.error( getTestId() + " Unable to deploy " + artifactFileName, e );
            throw e;
        }
    }

    protected void startNexus()
        throws Exception
    {
        log.info( "Starting Nexus" );

        TestContainer.getInstance().getTestContext().useAdminForRequests();

        try
        {
            getNexusStatusUtil().start( getTestId() );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            staticLog.error( e.getMessage(), e );
            throw e;
        }
    }

    protected static void stopNexus()
        throws Exception
    {
        staticLog.info( "Stopping Nexus" );

        getNexusStatusUtil().stop();
    }

    protected void restartNexus()
        throws Exception
    {
        log.info( "RESTARTING Nexus" );
        stopNexus();
        startNexus();
    }

    protected File getOverridableFile( String file )
    {
        // the test can override the test config.
        File testConfigFile = this.getTestResourceAsFile( "test-config/" + file );

        // if the tests doesn't have a different config then use the default.
        // we need to replace every time to make sure no one changes it.
        if ( testConfigFile == null || !testConfigFile.exists() )
        {
            testConfigFile = getResource( "default-configs/" + file );
        }
        else
        {
            log.debug( "This test is using its own " + file + " " + testConfigFile );
        }
        return testConfigFile;
    }

    protected void copyConfigFile( String configFile, String destShortName, Map<String, String> variables, String path )
        throws IOException
    {
        // the test can override the test config.
        File testConfigFile = this.getOverridableFile( configFile );

        if ( testConfigFile == null )
        {
            return;
        }

        File parent = new File( path );
        if ( !parent.isAbsolute() )
        {
            parent = new File( nexusBaseDir, path == null ? RELATIVE_CONF_DIR : path );
        }

        File destFile = new File( parent, destShortName );
        log.debug( LIx2 + "copying " + configFile + " to:  " + destFile );

        FileTestingUtils.interpolationFileCopy( testConfigFile, destFile, variables );

    }

    // Overloaded helpers

    protected void copyConfigFile( String configFile, String path )
        throws IOException
    {
        this.copyConfigFile( configFile, new HashMap<String, String>(), path );
    }

    protected void copyConfigFile( String configFile, Map<String, String> variables, String path )
        throws IOException
    {
        this.copyConfigFile( configFile, configFile, variables, path );

    }

    /**
     * Returns a File if it exists, null otherwise. Files returned by this method must be located in the
     * "src/test/resourcs/nexusXXX/" folder.
     * 
     * @param relativePath path relative to the nexusXXX directory.
     * @return A file specified by the relativePath. or null if it does not exist.
     */
    protected File getTestResourceAsFile( String relativePath )
    {
        String resource = this.getTestId() + "/" + relativePath;
        return getResource( resource );
    }

    protected String getTestId()
    {
        String packageName = this.getClass().getPackage().getName();
        return packageName.substring( packageName.lastIndexOf( '.' ) + 1, packageName.length() );
    }

    /**
     * Returns a File if it exists, null otherwise. Files returned by this method must be located in the
     * "src/test/resourcs/nexusXXX/files/" folder.
     * 
     * @param relativePath path relative to the files directory.
     * @return A file specified by the relativePath. or null if it does not exist.
     */
    protected File getTestFile( String relativePath )
    {
        return this.getTestResourceAsFile( "files/" + relativePath );
    }

    public static File getResource( String resource )
    {
        staticLog.debug( LIx1 + "Looking for resource: " + resource );
        // URL classURL = Thread.currentThread().getContextClassLoader().getResource( resource );

        File rootDir = new File( TestProperties.getString( "test.resources.folder" ) );
        File file = new File( rootDir, resource );

        if ( !file.exists() )
        {
            return null;
        }

        staticLog.debug( LIx2 + "found: " + file );

        return file;
    }

    // profiling with yourkit, activate using -P youtkit-profile
    private static Object profiler;

    private static void startProfiler()
    {
        Class<?> controllerClazz;
        try
        {
            controllerClazz = Class.forName( "com.yourkit.api.Controller" );
        }
        catch ( Exception e )
        {
            staticLog.info( "Profiler not present" );
            return;
        }

        try
        {
            profiler = controllerClazz.newInstance();
            controllerClazz.getMethod( "captureMemorySnapshot" ).invoke( profiler );
        }
        catch ( Exception e )
        {
            fail( "Profiler was active, but failed due: " + e.getMessage() );
        }
    }

    private static void takeSnapshot()
    {
        if ( profiler != null )
        {
            try
            {
                profiler.getClass().getMethod( "forceGC" ).invoke( profiler );
                profiler.getClass().getMethod( "captureMemorySnapshot" ).invoke( profiler );
            }
            catch ( Exception e )
            {
                fail( "Profiler was active, but failed due: " + e.getMessage() );
            }
        }
    }

    /**
     * Returns the basedir.
     * 
     * @return
     * @deprecated Use {@link TestContainer#getBasedir()} instead.
     */
    @Deprecated
    public static String getBasedir()
    {
        return TestContainer.getBasedir();
    }

    @Deprecated
    protected Object lookup( String role )
        throws ComponentLookupException
    {
        return getITPlexusContainer().lookup( role );
    }

    @Deprecated
    protected Object lookup( String role, String hint )
        throws ComponentLookupException
    {
        return getITPlexusContainer().lookup( role, hint );
    }

    protected <E> E lookup( Class<E> role )
        throws ComponentLookupException
    {
        return getITPlexusContainer().lookup( role );
    }

    protected <E> E lookup( Class<E> role, String hint )
        throws ComponentLookupException
    {
        return getITPlexusContainer().lookup( role, hint );
    }

    protected String getRelitivePomPath( Gav gav )
        throws FileNotFoundException
    {
        return GavUtil.getRelitivePomPath( gav );
    }

    protected String getRelitiveArtifactPath( Gav gav )
        throws FileNotFoundException
    {
        return GavUtil.getRelitiveArtifactPath( gav );
    }

    protected String getRelitiveArtifactPath( String groupId, String artifactId, String version, String extension,
                                              String classifier )
        throws FileNotFoundException
    {
        return GavUtil.getRelitiveArtifactPath( groupId, artifactId, version, extension, classifier );
    }

    public File downloadSnapshotArtifact( String repository, Gav gav, File parentDir )
        throws IOException
    {
        // @see http://issues.sonatype.org/browse/NEXUS-599
        // r=<repoId> -- mandatory
        // g=<groupId> -- mandatory
        // a=<artifactId> -- mandatory
        // v=<version> -- mandatory
        // c=<classifier> -- optional
        // p=<packaging> -- optional, jar is taken as default
        // http://localhost:8087/nexus/service/local/artifact/maven/redirect?r=tasks-snapshot-repo&g=nexus&a=artifact&
        // v=1.0-SNAPSHOT
        String c = gav.getClassifier() == null ? "" : "&c=" + Reference.encode( gav.getClassifier() );
        String serviceURI =
            "service/local/artifact/maven/redirect?r=" + repository + "&g=" + gav.getGroupId() + "&a="
                + gav.getArtifactId() + "&v=" + Reference.encode( gav.getVersion() ) + c;

        Response response = null;
        try
        {
            response = RequestFacade.doGetRequest( serviceURI );

            assertThat(
                response,
                allOf( isRedirecting(), respondsWithStatusCode( 301 ), redirectLocation( notNullValue( String.class ) ) ) );

            serviceURI = response.getLocationRef().toString();
        }
        finally
        {
            RequestFacade.releaseResponse( response );
        }

        File file = FileUtils.createTempFile( gav.getArtifactId(), '.' + gav.getExtension(), parentDir );
        RequestFacade.downloadFile( new URL( serviceURI ), file.getAbsolutePath() );

        return file;
    }

    protected Metadata downloadMetadataFromRepository( Gav gav, String repoId )
        throws IOException, XmlPullParserException
    {
        // File f =
        // new File( nexusWorkDir, "storage/" + repoId + "/" + gav.getGroupId() + "/" + gav.getArtifactId()
        // + "/maven-metadata.xml" );
        //
        // if ( !f.exists() )
        // {
        // throw new FileNotFoundException( "Metadata do not exist! " + f.getAbsolutePath() );
        // }

        String url =
            this.getBaseNexusUrl() + REPOSITORY_RELATIVE_URL + repoId + "/" + gav.getGroupId() + "/"
                + gav.getArtifactId() + "/maven-metadata.xml";

        Response response = null;
        InputStream stream = null;
        try
        {
            response = RequestFacade.sendMessage( new URL( url ), Method.GET, null );
            if ( response.getStatus().isError() )
            {
                return null;
            }
            stream = response.getEntity().getStream();
            MetadataXpp3Reader metadataReader = new MetadataXpp3Reader();
            return metadataReader.read( stream );
        }
        finally
        {
            RequestFacade.releaseResponse( response );
            IOUtil.close( stream );
        }

    }

    protected File downloadArtifact( Gav gav, String targetDirectory )
        throws IOException
    {
        return this.downloadArtifact( gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), gav.getExtension(),
            gav.getClassifier(), targetDirectory );
    }

    protected File downloadArtifact( String groupId, String artifact, String version, String type, String classifier,
                                     String targetDirectory )
        throws IOException
    {
        return this.downloadArtifact( this.getNexusTestRepoUrl(), groupId, artifact, version, type, classifier,
            targetDirectory );
    }

    protected File downloadArtifactFromRepository( String repoId, Gav gav, String targetDirectory )
        throws IOException
    {
        return this.downloadArtifact( AbstractNexusIntegrationTest.nexusBaseUrl + REPOSITORY_RELATIVE_URL + repoId
            + "/", gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), gav.getExtension(), gav.getClassifier(),
            targetDirectory );
    }

    protected File downloadArtifactFromGroup( String groupId, Gav gav, String targetDirectory )
        throws IOException
    {
        return this.downloadArtifact( AbstractNexusIntegrationTest.nexusBaseUrl + GROUP_REPOSITORY_RELATIVE_URL
            + groupId + "/", gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), gav.getExtension(),
            gav.getClassifier(), targetDirectory );
    }

    protected File downloadArtifact( String baseUrl, String groupId, String artifact, String version, String type,
                                     String classifier, String targetDirectory )
        throws IOException
    {
        URL url = new URL( baseUrl + this.getRelitiveArtifactPath( groupId, artifact, version, type, classifier ) );

        String classifierPart = ( classifier != null ) ? "-" + classifier : "";
        return this.downloadFile( url, targetDirectory + "/" + artifact + "-" + version + classifierPart + "." + type );
    }

    public File downloadFile( URL url, String targetFile )
        throws IOException
    {

        return RequestFacade.downloadFile( url, targetFile );
    }

    protected boolean deleteFromRepository( String groupOrArtifactPath )
        throws IOException
    {
        return this.deleteFromRepository( this.testRepositoryId, groupOrArtifactPath );
    }

    protected boolean deleteFromRepository( String repository, String groupOrArtifactPath )
        throws IOException
    {
        String serviceURI = "service/local/repositories/" + repository + "/content/" + groupOrArtifactPath;

        Status status = RequestFacade.doGetForStatus( serviceURI );
        if ( status.equals( Status.CLIENT_ERROR_NOT_FOUND ) )
        {
            log.debug( "Not deleted because it didn't exist: " + serviceURI );
            return true;
        }

        // ---
        log.debug( "deleting: " + serviceURI );
        status = RequestFacade.doDeleteForStatus( serviceURI, null );
        boolean deleted = status.isSuccess();

        if ( !deleted )
        {
            log.debug( "Failed to delete: " + serviceURI + "  - Status: " + status );
        }

        // fake it because the artifact doesn't exist
        // TODO: clean this up.
        if ( status.getCode() == 404 )
        {
            deleted = true;
        }

        return deleted;
    }

    public String getBaseNexusUrl()
    {
        return nexusBaseUrl;
    }

    public String getNexusTestRepoUrl( String repo )
    {
        return nexusBaseUrl + REPOSITORY_RELATIVE_URL + repo + "/";
    }

    public String getNexusTestRepoUrl()
    {
        return getNexusTestRepoUrl( testRepositoryId );
    }

    public String getNexusTestRepoServiceUrl()
    {
        return nexusBaseUrl + "service/local/repositories/" + testRepositoryId + "/content/";
    }

    public String getNexusBaseDir()
    {
        return nexusBaseDir;
    }

    public String getTestRepositoryId()
    {
        return testRepositoryId;
    }

    public void setTestRepositoryId( String repoId )
    {
        this.testRepositoryId = repoId;
    }

    public String getRepositoryUrl( String repoId )
    {
        return nexusBaseUrl + REPOSITORY_RELATIVE_URL + repoId + "/";
    }

    public String getGroupUrl( String groupId )
    {
        return nexusBaseUrl + GROUP_REPOSITORY_RELATIVE_URL + groupId + "/";
    }

    protected boolean printKnownErrorButDoNotFail( Class<? extends AbstractNexusIntegrationTest> clazz, String... tests )
    {
        StringBuilder error =
            new StringBuilder( "*********************************************************************************" );
        error.append( "\n* This test is being skipped because its known to fail," );
        error.append( "\n* It is a very minor error, and is only a problem if you start sending in " );
        error.append( "\n* raw REST request to Nexus. (it is not a security problem)" );
        error.append( "*\n*\n" );
        error.append( "*\n* TestClass: " ).append( clazz );
        for ( String test : tests )
        {
            error.append( "*\n* Test: " ).append( test );
        }
        error.append( "\n**********************************************************************************" );

        log.info( error.toString() );

        return true;
    }

    public XStream getXMLXStream()
    {
        return XStreamFactory.getXmlXStream();
    }

    public XStream getJsonXStream()
    {
        return XStreamFactory.getJsonXStream();
    }

    // == IT Container management

    public PlexusContainer getITPlexusContainer()
    {
        return TestContainer.getInstance().getPlexusContainer();
    }

    protected void installOptionalPlugin( final String plugin )
        throws IOException
    {
        File pluginDir = getOptionalPluginDirectory( plugin );

        if ( pluginDir != null )
        {
            File target = new File( getNexusBaseDir(), "runtime/apps/nexus/plugin-repository/" + pluginDir.getName() );
            FileUtils.copyDirectory( pluginDir, target );
        }
    }

    protected File getOptionalPluginDirectory( final String plugin )
    {
        File optionalPluginDir = new File( getNexusBaseDir(), "runtime/apps/nexus/optional-plugins/" );

        if ( optionalPluginDir.exists() && optionalPluginDir.isDirectory() )
        {
            File[] files = optionalPluginDir.listFiles( new FilenameFilter()
            {
                public boolean accept( File dir, String name )
                {
                    if ( name.startsWith( plugin ) )
                    {
                        return true;
                    }

                    return false;
                }
            } );

            if ( files == null || files.length > 1 )
            {
                log.error( "Unable to lookup plugin: " + plugin );
                return null;
            }

            return files[0];
        }

        return null;
    }

    protected Map<String, String> getTestProperties()
    {
        HashMap<String, String> variables = new HashMap<String, String>();
        variables.putAll( TestProperties.getAll() );
        variables.put( "test-id", getTestId() );
        return variables;
    }

}
