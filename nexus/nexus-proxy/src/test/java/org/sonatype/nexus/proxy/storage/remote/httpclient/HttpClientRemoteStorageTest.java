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
package org.sonatype.nexus.proxy.storage.remote.httpclient;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.proxy.RemoteStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.storage.remote.RemoteItemNotFoundException;
import org.sonatype.nexus.proxy.storage.remote.commonshttpclient.CommonsHttpClientRemoteStorage;
import org.sonatype.nexus.proxy.storage.remote.http.QueryStringBuilder;
import org.sonatype.nexus.proxy.utils.UserAgentBuilder;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

/**
 * {@link HttpClientRemoteStorage} UTs.
 *
 * @since 2.2
 */
public class HttpClientRemoteStorageTest
    extends TestSupport
{

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * When retrieving an item with a path that ends with a "/" and without a query,
     * an RemoteItemNotFoundException with a message that a collection could not be downloaded over HTTP should be
     * thrown.
     */
    @Test
    public void retrieveCollectionWhenPathEndsWithSlashAndNoQuery()
        throws Exception
    {
        final HttpClientRemoteStorage underTest = new HttpClientRemoteStorage(
            mock( UserAgentBuilder.class ),
            mock( ApplicationStatusSource.class ),
            mock( MimeSupport.class ),
            mock( QueryStringBuilder.class )
        );

        thrown.expect( RemoteItemNotFoundException.class );
        thrown.expectMessage( "The remoteURL we got to looks like is a collection" );

        underTest.retrieveItem(
            mock( ProxyRepository.class ),
            new ResourceStoreRequest( "bar/" ),
            "http://foo.com"
        );
    }

    /**
     * When retrieving an item with a path that ends with a "/" and a query string that ends with a "/",
     * an RemoteItemNotFoundException with a message that a collection could not be downloaded over HTTP should be
     * thrown.
     */
    @Test
    public void retrieveCollectionWhenPathEndsWithSlashAndQueryEndsWithSlash()
        throws Exception
    {
        final HttpClientRemoteStorage underTest = new HttpClientRemoteStorage(
            mock( UserAgentBuilder.class ),
            mock( ApplicationStatusSource.class ),
            mock( MimeSupport.class ),
            mock( QueryStringBuilder.class )
        );

        thrown.expect( RemoteItemNotFoundException.class );
        thrown.expectMessage( "The remoteURL we got to looks like is a collection" );

        underTest.retrieveItem(
            mock( ProxyRepository.class ),
            new ResourceStoreRequest( "bar/?param=x/" ),
            "http://foo.com"
        );
    }

    /**
     * When retrieving an item with a path that ends with a "/" and a query string that does not end with a "/",
     * an RemoteItemNotFoundException with a message that a collection could not be downloaded over HTTP should be
     * thrown.
     */
    @Test
    public void retrieveCollectionWhenPathEndsWithSlashAndQueryDoesNotEndWithSlash()
        throws Exception
    {
        final HttpClientRemoteStorage underTest = new HttpClientRemoteStorage(
            mock( UserAgentBuilder.class ),
            mock( ApplicationStatusSource.class ),
            mock( MimeSupport.class ),
            mock( QueryStringBuilder.class )
        );

        thrown.expect( RemoteItemNotFoundException.class );
        thrown.expectMessage( "The remoteURL we got to looks like is a collection" );

        underTest.retrieveItem(
            mock( ProxyRepository.class ),
            new ResourceStoreRequest( "bar/?param=x" ),
            "http://foo.com"
        );
    }

    /**
     * When retrieving an item with a path that does not end with a "/" and a query string that does not end with a "/",
     * no exception should be thrown.
     */
    @Test
    public void retrieveCollectionWhenPathDoesNotEndWithSlashAndQueryDoesNotEndWithSlash()
        throws Exception
    {
        final HttpClientRemoteStorage underTest = new HttpClientRemoteStorage(
            mock( UserAgentBuilder.class ),
            mock( ApplicationStatusSource.class ),
            mock( MimeSupport.class ),
            mock( QueryStringBuilder.class )
        )
        {
            @Override
            HttpResponse executeRequest( final ProxyRepository repository, final ResourceStoreRequest request,
                                         final HttpUriRequest httpRequest )
                throws RemoteStorageException
            {
                final HttpResponse httpResponse = mock( HttpResponse.class );
                final StatusLine statusLine = mock( StatusLine.class );
                when( httpResponse.getStatusLine() ).thenReturn( statusLine );
                when( statusLine.getStatusCode() ).thenReturn( 200 );
                when( httpResponse.getEntity() ).thenReturn( mock( HttpEntity.class ) );
                return httpResponse;
            }
        };

        final ProxyRepository repository = mock( ProxyRepository.class );
        when( repository.getId() ).thenReturn( "foo" );

        underTest.retrieveItem(
            repository,
            new ResourceStoreRequest( "bar?param=x" ),
            "http://foo.com"
        );
    }

}
