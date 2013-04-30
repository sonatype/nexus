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
package org.sonatype.nexus.proxy.storage.local.fs;

import java.io.File;
import java.io.InputStream;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.attributes.AttributesHandler;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.LinkPersister;
import org.sonatype.nexus.proxy.item.PreparedContentLocator;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.wastebasket.Wastebasket;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

/**
 * Testing NEXUS-5468 http connection leak triggered by LocalStorageException
 * 
 * @author cstamas
 */
public class NEXUS5468ConnectionLeakOnStoreItemTest
    extends TestSupport
{
    @Mock
    private Wastebasket wastebasket;

    @Mock
    private LinkPersister linkPersister;

    @Mock
    private MimeSupport mimeSupport;

    @Mock
    private FSPeer fsPeer;

    @Test
    public void closeIsCalledOnPreparedContentLocatorIfAllWentOkay()
        throws Exception
    {
        final InputStream preparedStream = Mockito.mock( InputStream.class );
        try
        {
            final DefaultFSLocalRepositoryStorage testSubject =
                new DefaultFSLocalRepositoryStorage( wastebasket, linkPersister, mimeSupport, fsPeer );

            final Repository repository = Mockito.mock( Repository.class );
            Mockito.when( repository.getId() ).thenReturn( "test" );
            Mockito.when( repository.getAttributesHandler() ).thenReturn( Mockito.mock( AttributesHandler.class ) );
            // we return some URL, but does not matter which, this is only to avoid NPE
            // so execution path is "normal success" of storeItem in this case
            Mockito.when( repository.getLocalUrl() ).thenReturn( new File( "target" ).toURI().toURL().toString() );

            final PreparedContentLocator pcl = new PreparedContentLocator( preparedStream, "text/plain" );

            final DefaultStorageFileItem file =
                new DefaultStorageFileItem( repository, new ResourceStoreRequest( "/some/file.txt" ), true, true, pcl );

            testSubject.storeItem( repository, file );
        }
        finally
        {
            Mockito.verify( preparedStream, Mockito.times( 1 ) ).close();
        }
    }

    @Test( expected = RuntimeException.class )
    public void closeIsCalledOnPreparedContentLocatorIfUnexpectedExceptionIsMet()
        throws Exception
    {
        final InputStream preparedStream = Mockito.mock( InputStream.class );
        try
        {
            final DefaultFSLocalRepositoryStorage testSubject =
                new DefaultFSLocalRepositoryStorage( wastebasket, linkPersister, mimeSupport, fsPeer );

            final Repository repository = Mockito.mock( Repository.class );
            Mockito.when( repository.getId() ).thenReturn( "test" );
            Mockito.when( repository.getAttributesHandler() ).thenReturn( Mockito.mock( AttributesHandler.class ) );
            // we intentionally throw some unexpected exception here
            // so execution path here will be interrupted
            Mockito.when( repository.getLocalUrl() ).thenThrow( new RuntimeException( "Something unexpected!" ) );

            final PreparedContentLocator pcl = new PreparedContentLocator( preparedStream, "text/plain" );

            final DefaultStorageFileItem file =
                new DefaultStorageFileItem( repository, new ResourceStoreRequest( "/some/file.txt" ), true, true, pcl );

            testSubject.storeItem( repository, file );
        }
        finally
        {
            Mockito.verify( preparedStream, Mockito.times( 1 ) ).close();
        }
    }

}
