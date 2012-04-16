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
package org.sonatype.nexus.proxy.repository;

import java.io.File;
import java.net.URL;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.codehaus.plexus.util.FileUtils;
import org.sonatype.jettytestsuite.ServletServer;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.proxy.AbstractProxyTestEnvironment;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.EnvironmentBuilder;
import org.sonatype.nexus.proxy.M2TestsuiteEnvironmentBuilder;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.maven2.Maven2ContentClass;
import org.sonatype.nexus.proxy.target.Target;
import org.sonatype.nexus.proxy.target.TargetMatch;
import org.sonatype.nexus.proxy.target.TargetRegistry;
import org.sonatype.nexus.security.WebSecurityUtil;
import org.sonatype.security.SecuritySystem;
import org.sonatype.security.authentication.AuthenticationException;

public class AccessTest
    extends AbstractProxyTestEnvironment
{
    private M2TestsuiteEnvironmentBuilder jettyTestsuiteEnvironmentBuilder;

    protected ApplicationConfiguration applicationConfiguration;

    @Override
    public void setUp()
        throws Exception
    {
        applicationConfiguration = this.lookup( ApplicationConfiguration.class );
        applicationConfiguration.saveConfiguration();

        super.setUp();

        String resource = this.getClass().getName().replaceAll( "\\.", "\\/" ) + "-security-configuration.xml";
        URL url = Thread.currentThread().getContextClassLoader().getResource( resource );
        FileUtils.copyURLToFile( url, new File( getConfHomeDir(), "security-configuration.xml" ) );

        TargetRegistry targetRegistry = this.lookup( TargetRegistry.class );

        Target t1 =
            new Target( "maven2-all", "All (Maven2)", new Maven2ContentClass(), Arrays.asList( new String[] { ".*" } ) );

        targetRegistry.addRepositoryTarget( t1 );

        applicationConfiguration.saveConfiguration();

        // setup security
        this.lookup( SecuritySystem.class ).start(); // need to call start to clear caches
    }

    @Override
    public void tearDown()
        throws Exception
    {
        try
        {
            ThreadContext.remove();
        }
        finally
        {
            super.tearDown();
        }
    }

    @Override
    protected EnvironmentBuilder getEnvironmentBuilder()
        throws Exception
    {
        if ( this.jettyTestsuiteEnvironmentBuilder == null )
        {
            ServletServer ss = (ServletServer) lookup( ServletServer.ROLE );
            this.jettyTestsuiteEnvironmentBuilder = new M2TestsuiteEnvironmentBuilder( ss );
        }
        return this.jettyTestsuiteEnvironmentBuilder;
    }

    @Test
    public void testGroupAccess()
        throws AuthenticationException, Exception
    {

        // user does not have access to the group
        try
        {
            this.getItem( "allrepo3", "test", "/spoof/simple.txt" );
            Assert.fail( "Expected AccessDeniedException" );
        }
        catch ( AccessDeniedException e )
        {
            // expected
        }

        // this user has access to the group
        StorageItem item = this.getItem( "alltest", "test", "/spoof/simple.txt" );
        // the first repo in the group
        Assert.assertEquals( "repo1", item.getRepositoryId() );

    }

    @Test
    public void testRepositoryAccess()
        throws AuthenticationException, Exception
    {
        // Not true, currently perms are always transitive
        // group access imples repository access, IF NOT EXPOSED, SO "UN"-EXPOSE IT
        // getRepositoryRegistry().getRepositoryWithFacet( "test", GroupRepository.class ).setExposed( false );

        StorageItem item = this.getItem( "alltest", "repo1", "/spoof/simple.txt" );
        Assert.assertEquals( "repo1", item.getRepositoryId() );
    }

    //
    // public void testAuthorizerDirectly()
    // throws Exception
    // {
    // String repoId = "repo1";
    // String path = "/spoof/simple.txt";
    // String username = "alltest";
    //
    // Subject subject = SecurityUtils.getSecurityManager().login( new UsernamePasswordToken( username, "" ) );
    //
    // NexusItemAuthorizer authorizer = this.lookup( NexusItemAuthorizer.class );
    //
    // ResourceStoreRequest request = new ResourceStoreRequest( "/repositories/" + repoId + "/" + path, true );
    // Assert.assertTrue( authorizer.authorizePath( request, Action.read ) );
    //
    // // not sure if we really need to log the user out, we are not using a remember me,
    // // but what can it hurt?
    // SecurityUtils.getSecurityManager().logout( subject.getPrincipals() );
    //
    // }

    private StorageItem getItem( String username, String repositoryId, String path )
        throws AuthenticationException, Exception
    {
        WebSecurityUtil.setupWebContext( username + "-" + repositoryId + "-" + path );

        SecuritySystem securitySystem = this.lookup( SecuritySystem.class );

        Subject subject = securitySystem.login( new UsernamePasswordToken( username, "" ) );

        Repository repo = this.getRepositoryRegistry().getRepository( repositoryId );

        ResourceStoreRequest request = new ResourceStoreRequest( path, false );

        StorageItem item = repo.retrieveItem( request );

        // not sure if we really need to log the user out, we are not using a remember me,
        // but what can it hurt?
        securitySystem.logout( subject );

        return item;
    }

    private String targetMatchToString( TargetMatch targetMatch )
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append( "RepoId: " ).append( targetMatch.getRepository().getId() ).append( "\n" );
        Target target = targetMatch.getTarget();
        buffer.append( "target: " ).append( target.getId() ).append( " - " ).append( target.getName() ).append( "\n" );

        for ( String pattern : target.getPatternTexts() )
        {
            buffer.append( "\tpattern: " ).append( pattern );
        }

        return buffer.toString();
    }

}
