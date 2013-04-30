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
package org.sonatype.nexus.proxy.item;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.codehaus.plexus.util.IOUtil;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class DefaultStorageFileItemTest
    extends AbstractStorageItemTest
{
    @Test
    public void testNonVirtualFileSimple()
        throws Exception
    {
        doReturn( "dummy" ).when( repository ).getId();
        doAnswer( new Answer<RepositoryItemUid>() {
            public RepositoryItemUid answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                return getRepositoryItemUidFactory().createUid( repository, (String)args[0] );
            }}).when( repository ).createUid( "/a.txt" );

        DefaultStorageFileItem file =
            new DefaultStorageFileItem( repository, "/a.txt", true, true, new StringContentLocator( "/a.txt" ) );
        checkAbstractStorageItem( repository, file, false, "a.txt", "/a.txt", "/" );

        // content
        InputStream is = file.getInputStream();
        assertEquals( true,
            IOUtil.contentEquals( is, new ByteArrayInputStream( file.getRepositoryItemUid().getPath().getBytes() ) ) );
    }

    @Test
    public void testNonVirtualFileWithContentSimple()
        throws Exception
    {
        doReturn( "dummy" ).when( repository ).getId();
        doAnswer( new Answer<RepositoryItemUid>() {
            public RepositoryItemUid answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                return getRepositoryItemUidFactory().createUid( repository, (String)args[0] );
            }}).when( repository ).createUid( "/a.txt" );

        DefaultStorageFileItem file =
            new DefaultStorageFileItem( repository, "/a.txt", true, true, new StringContentLocator( "THIS IS CONTENT" ) );
        checkAbstractStorageItem( repository, file, false, "a.txt", "/a.txt", "/" );

        // content
        InputStream fis = file.getInputStream();
        assertEquals( true, IOUtil.contentEquals( fis, new ByteArrayInputStream( "THIS IS CONTENT".getBytes() ) ) );
    }

    @Test
    public void testNonVirtualFileDeep()
        throws Exception
    {
        doReturn( "dummy" ).when( repository ).getId();
        doAnswer( new Answer<RepositoryItemUid>() {
            public RepositoryItemUid answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                return getRepositoryItemUidFactory().createUid( repository, (String)args[0] );
            }}).when( repository ).createUid( "/some/dir/hierarchy/a.txt" );

        DefaultStorageFileItem file =
            new DefaultStorageFileItem( repository, "/some/dir/hierarchy/a.txt", true, true, new StringContentLocator(
                "/some/dir/hierarchy/a.txt" ) );
        checkAbstractStorageItem( repository, file, false, "a.txt", "/some/dir/hierarchy/a.txt", "/some/dir/hierarchy" );

        // content
        InputStream is = file.getInputStream();
        assertEquals( true,
            IOUtil.contentEquals( is, new ByteArrayInputStream( file.getRepositoryItemUid().getPath().getBytes() ) ) );
    }

    @Test
    public void testNonVirtualFileWithContentDeep()
        throws Exception
    {
        doReturn( "dummy" ).when( repository ).getId();
        doAnswer( new Answer<RepositoryItemUid>() {
            public RepositoryItemUid answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                return getRepositoryItemUidFactory().createUid( repository, (String)args[0] );
            }}).when( repository ).createUid( "/some/dir/hierarchy/a.txt" );

        DefaultStorageFileItem file =
            new DefaultStorageFileItem( repository, "/some/dir/hierarchy/a.txt", true, true, new StringContentLocator(
                "THIS IS CONTENT" ) );
        checkAbstractStorageItem( repository, file, false, "a.txt", "/some/dir/hierarchy/a.txt", "/some/dir/hierarchy" );

        // content
        InputStream fis = file.getInputStream();
        assertEquals( true, IOUtil.contentEquals( fis, new ByteArrayInputStream( "THIS IS CONTENT".getBytes() ) ) );
    }

}
