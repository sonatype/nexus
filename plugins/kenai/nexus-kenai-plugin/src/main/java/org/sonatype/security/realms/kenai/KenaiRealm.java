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
package org.sonatype.security.realms.kenai;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.shiro.authc.AccountException;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.inject.Description;
import org.sonatype.nexus.apachehttpclient.Hc4Provider;
import org.sonatype.security.realms.kenai.config.KenaiRealmConfiguration;

import com.google.common.collect.Lists;

/**
 * A Realm that connects to a java.net kenai API.
 * 
 * @author Brian Demers
 */
@Singleton
@Typed( Realm.class )
@Named( "kenai" )
@Description( "Kenai Realm" )
public class KenaiRealm
    extends AuthorizingRealm
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final KenaiRealmConfiguration kenaiRealmConfiguration;

    private final Hc4Provider hc4Provider;

    @Inject
    public KenaiRealm( final KenaiRealmConfiguration kenaiRealmConfiguration,
                       final Hc4Provider hc4Provider )
    {
        this.kenaiRealmConfiguration = checkNotNull( kenaiRealmConfiguration );
        this.hc4Provider = checkNotNull( hc4Provider );

        // TODO: write another test before enabling this
        // this.setAuthenticationCachingEnabled( true );
    }

    @Override
    public String getName()
    {
        return "kenai";
    }

    // ------------ AUTHENTICATION ------------

    @Override
    public boolean supports( final AuthenticationToken token )
    {
        return ( token instanceof UsernamePasswordToken );
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo( final AuthenticationToken token )
        throws AuthenticationException
    {
        final UsernamePasswordToken upToken = (UsernamePasswordToken) token;

        // if the user can authenticate we are good to go
        if ( authenticateViaUrl( upToken ) )
        {
            return buildAuthenticationInfo( upToken );
        }
        else
        {
            throw new AccountException( "User \"" + upToken.getUsername()
                + "\" cannot be authenticated via Kenai Realm." );
        }
    }

    private AuthenticationInfo buildAuthenticationInfo( final UsernamePasswordToken token )
    {
        return new SimpleAuthenticationInfo( token.getPrincipal(), token.getCredentials(), getName() );
    }

    private boolean authenticateViaUrl( final UsernamePasswordToken usernamePasswordToken )
    {
        final HttpClient client = getHttpClient( null );

        try
        {
            final String url = kenaiRealmConfiguration.getConfiguration().getBaseUrl() + "api/login/authenticate.json";
            final List<NameValuePair> nameValuePairs = Lists.newArrayListWithCapacity( 2 );
            nameValuePairs.add( new BasicNameValuePair( "username", usernamePasswordToken.getUsername() ) );
            nameValuePairs.add( new BasicNameValuePair( "password", new String( usernamePasswordToken.getPassword() ) ) );
            final HttpPost post = new HttpPost( url );
            post.setEntity( new UrlEncodedFormEntity( nameValuePairs, Consts.UTF_8 ) );
            final HttpResponse response = client.execute( post );

            try
            {
                logger.debug( "User \"{}\" validated against URL={} as {}", usernamePasswordToken.getUsername(), url,
                    response.getStatusLine() );
                final boolean success =
                    response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() <= 299;
                return success;
            }
            finally
            {
                HttpClientUtils.closeQuietly( response );
            }
        }
        catch ( IOException e )
        {
            logger.info( "URLRealm was unable to perform authentication.", e );
            return false;
        }
    }

    // ------------ AUTHORIZATION ------------

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo( final PrincipalCollection principals )
    {
        // shortcut for now
        return buildAuthorizationInfo();
    }

    private AuthorizationInfo buildAuthorizationInfo()
    {
        final SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        // add the default role
        authorizationInfo.addRole( kenaiRealmConfiguration.getConfiguration().getDefaultRole() );
        return authorizationInfo;
    }

    // ==

    private HttpClient getHttpClient( final UsernamePasswordToken usernamePasswordToken )
    {
        // risky, but we must blindly assume it is
        final DefaultHttpClient client = (DefaultHttpClient) hc4Provider.createHttpClient();
        if ( usernamePasswordToken != null )
        {
            final List<String> authorisationPreference = new ArrayList<String>( 2 );
            authorisationPreference.add( AuthPolicy.DIGEST );
            authorisationPreference.add( AuthPolicy.BASIC );
            final Credentials credentials =
                new UsernamePasswordCredentials( usernamePasswordToken.getUsername(),
                    String.valueOf( usernamePasswordToken.getPassword() ) );
            client.getCredentialsProvider().setCredentials( AuthScope.ANY, credentials );
            client.getParams().setParameter( AuthPNames.TARGET_AUTH_PREF, authorisationPreference );
        }
        return client;
    }
}
