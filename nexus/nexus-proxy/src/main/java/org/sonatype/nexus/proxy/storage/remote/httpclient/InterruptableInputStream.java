/*
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

import org.apache.http.ConnectionClosedException;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.sonatype.nexus.proxy.RemoteStorageEofException;
import org.sonatype.nexus.proxy.repository.ProxyRepository;

/**
 * Best-effort interruptable InputStream wrapper. The wrapper checks for Thread.isInterrupred before delegating to the
 * actual stream. If the thread is interrupted, the wrapper calls AbortableHttpRequest.abort() and throws
 * InterruptedIOException.
 */
class InterruptableInputStream
    extends InputStream
{
    private final ProxyRepository proxyRepository;

    private final InputStream stream;

    private AbortableHttpRequest request;

    public InterruptableInputStream( final ProxyRepository proxyRepository, final AbortableHttpRequest request, final InputStream stream )
    {
        this.proxyRepository = proxyRepository;
        this.request = request;
        this.stream = stream;
    }

    public InterruptableInputStream( final ProxyRepository proxyRepository, final InputStream stream )
    {
        this( proxyRepository, null, stream );
    }

    private void abortIfInterrupted()
        throws IOException
    {
        if ( Thread.interrupted() )
        {
            if ( request != null )
            {
                request.abort();
            }
            throw new InterruptedIOException();
        }
    }

    @Override
    public int read()
        throws IOException
    {
        abortIfInterrupted();
        try
        {
            return stream.read();
        }
        catch ( ConnectionClosedException e )
        {
            throw new RemoteStorageEofException( proxyRepository, e );
        }
    }

    @Override
    public int read( byte[] b )
        throws IOException
    {
        abortIfInterrupted();
        try
        {
            return stream.read( b );
        }
        catch ( ConnectionClosedException e )
        {
            throw new RemoteStorageEofException( proxyRepository, e );
        }
    }

    @Override
    public int read( byte b[], int off, int len )
        throws IOException
    {
        abortIfInterrupted();
        try
        {
            return stream.read( b, off, len );
        }
        catch ( ConnectionClosedException e )
        {
            throw new RemoteStorageEofException( proxyRepository, e );
        }
    }

    @Override
    public long skip( long n )
        throws IOException
    {
        abortIfInterrupted();
        return stream.skip( n );
    }

    @Override
    public int available()
        throws IOException
    {
        abortIfInterrupted();
        return stream.available();
    }

    @Override
    public void close()
        throws IOException
    {
        // do not throw InterruptedIOException here!
        // this will not close the stream and likely mask original exception!
        stream.close();
    }

    @Override
    public void mark( int readlimit )
    {
        stream.mark( readlimit );
    }

    @Override
    public void reset()
        throws IOException
    {
        abortIfInterrupted();
        stream.reset();
    }

    @Override
    public boolean markSupported()
    {
        return stream.markSupported();
    }
}
