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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.codehaus.plexus.util.FileUtils;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.util.WrappingInputStream;

import com.google.common.base.Preconditions;

/**
 * A content locator that is backed by a file. It has ability to create a temporary file for you, and to become
 * "non reusable" locator, when it cleans up the file after it.
 * 
 * @author cstamas
 * @since 2.1
 */
public class FileContentLocator
    implements ContentLocator
{
    private final File file;

    private final String mimeType;

    private final boolean deleteOnCloseInput;

    /**
     * Creates a temporary file backed instance, that will be not reusable, once file content is consumed (using
     * {@link #getInputStream()} or {@link #getContent()}).
     * 
     * @param mimeType
     * @throws IOException
     */
    public FileContentLocator( final String mimeType )
        throws IOException
    {
        this( File.createTempFile( "nx-tmp-content-locator", "tmp" ), mimeType, true );
    }

    /**
     * Creates a file backed instance that will be backed by passed in {@link File}. It will be reusable, the passed in
     * file should be removed (if needed) by caller.
     * 
     * @param file
     * @param mimeType
     */
    public FileContentLocator( final File file, final String mimeType )
    {
        this( file, mimeType, false );
    }

    /**
     * Creates a file backed instance.
     * 
     * @param file the file to be used with this instance.
     * @param mimeType the mime type of this instance.
     * @param deleteOnCloseInput if {@code true}, the passed in file will be deleted upon consumption (when
     *            {@link InputStream#close()} is invoked) on {@link InputStream} got by and of the
     *            {@link #getInputStream()} or {@link #getContent()} method. Also, this instance will be marked as
     *            "non reusable" (method {@link #isReusable()} will return {@code false}).
     */
    public FileContentLocator( final File file, final String mimeType, final boolean deleteOnCloseInput )
    {
        this.file = Preconditions.checkNotNull( file );
        this.mimeType = Preconditions.checkNotNull( mimeType );
        this.deleteOnCloseInput = deleteOnCloseInput;
    }

    public InputStream getInputStream()
        throws IOException
    {
        if ( deleteOnCloseInput )
        {
            return new DeleteOnCloseFileInputStream( getFile() );
        }
        else
        {
            return new FileInputStream( getFile() );
        }
    }

    public OutputStream getOutputStream()
        throws IOException
    {
        return new FileOutputStream( getFile() );
    }

    public long getLength()
    {
        return getFile().length();
    }

    public File getFile()
    {
        return file;
    }

    public void delete()
        throws IOException
    {
        FileUtils.forceDelete( getFile() );
    }

    // ==

    @Override
    public InputStream getContent()
        throws IOException
    {
        return getInputStream();
    }

    @Override
    public String getMimeType()
    {
        return mimeType;
    }

    @Override
    public boolean isReusable()
    {
        return !deleteOnCloseInput;
    }

    // ==

    public static class DeleteOnCloseFileInputStream
        extends WrappingInputStream
    {
        private final File file;

        public DeleteOnCloseFileInputStream( final File file )
            throws IOException
        {
            super( new FileInputStream( file ) );
            this.file = file;
        }

        public void close()
            throws IOException
        {
            super.close();
            FileUtils.forceDelete( file );
        }
    }
}
