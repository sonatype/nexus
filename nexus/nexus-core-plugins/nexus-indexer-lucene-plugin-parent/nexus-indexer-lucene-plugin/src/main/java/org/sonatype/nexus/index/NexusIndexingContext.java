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
package org.sonatype.nexus.index;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.maven.index.artifact.GavCalculator;
import org.apache.maven.index.context.DocumentFilter;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.sonatype.scheduling.TaskUtil;

import com.google.common.base.Preconditions;

/**
 * Nexus specific wrapper for Maven Indexer {@link IndexingContext}.
 * 
 * @author cstamas
 * @since 2.3
 */
public class NexusIndexingContext
    implements IndexingContext
{
    /**
     * The MI context this instance delegates to.
     */
    private final IndexingContext delegate;

    /**
     * {@link ReentrantReadWriteLock} instance, used to synchronize access. The nice thing, is that this lock will obey
     * the "sisu-resource-locks" property (possible values: "local" or "hazelcast"), and will expose lock and thread
     * informations over JMX usable for debug, or just inspection.
     */
    private final ReentrantReadWriteLock lock;

    /**
     * Flag marking this context as closed. Returns {@code true} if {@link #close(boolean)} operation has been invoked
     * on this instance.
     */
    private volatile boolean closed;

    /**
     * Constructor.
     * 
     * @param delegate
     */
    public NexusIndexingContext( final IndexingContext delegate )
    {
        this.delegate = Preconditions.checkNotNull( delegate );
        this.lock = new ReentrantReadWriteLock();
        this.closed = false;
    }

    /**
     * Returns the MI "native" {@link IndexingContext}. The returned instance is unprotected!
     * 
     * @return native context.
     */
    public IndexingContext getDelegate()
    {
        return delegate;
    }

    /**
     * Returns the lock of this context.
     * 
     * @return the lock.
     */
    public ReentrantReadWriteLock getLock()
    {
        return lock;
    }

    // ==

    protected void failIfClosedOrInterrupted()
    {
        if ( closed )
        {
            throw new IllegalStateException( "IndexingContext is closed!" );
        }
        TaskUtil.checkInterruption();
    }
    
    // ==

    @Override
    public void close( boolean deleteFiles )
        throws IOException
    {
        lock.writeLock().lock();
        try
        {
            delegate.close( deleteFiles );
        }
        finally
        {
            closed = true;
            lock.writeLock().unlock();
        }
    }

    @Override
    public void purge()
        throws IOException
    {
        failIfClosedOrInterrupted();
        lock.writeLock().lock();
        try
        {
            delegate.purge();
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void replace( Directory directory )
        throws IOException
    {
        failIfClosedOrInterrupted();
        lock.writeLock().lock();
        try
        {
            delegate.replace( directory );
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    // ==
    // These below operate on IndexSearcher, excluded by operations above
    // that nullifies it

    @Override
    public void merge( Directory directory )
        throws IOException
    {
        failIfClosedOrInterrupted();
        lock.readLock().lock();
        try
        {
            delegate.merge( directory );
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    @Override
    public void merge( Directory directory, DocumentFilter filter )
        throws IOException
    {
        failIfClosedOrInterrupted();
        lock.readLock().lock();
        try
        {
            delegate.merge( directory, filter );
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    @Override
    public Date getTimestamp()
    {
        failIfClosedOrInterrupted();
        lock.readLock().lock();
        try
        {
            return delegate.getTimestamp();
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    @Override
    public void updateTimestamp()
        throws IOException
    {
        failIfClosedOrInterrupted();
        lock.readLock().lock();
        try
        {
            delegate.updateTimestamp();
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    @Override
    public void updateTimestamp( boolean save )
        throws IOException
    {
        failIfClosedOrInterrupted();
        lock.readLock().lock();
        try
        {
            delegate.updateTimestamp( save );
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    @Override
    public void updateTimestamp( boolean save, Date date )
        throws IOException
    {
        failIfClosedOrInterrupted();
        lock.readLock().lock();
        try
        {
            delegate.updateTimestamp( save, date );
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    @Override
    public int getSize()
        throws IOException
    {
        failIfClosedOrInterrupted();
        lock.readLock().lock();
        try
        {
            return delegate.getSize();
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    @Override
    public IndexSearcher acquireIndexSearcher()
        throws IOException
    {
        TaskUtil.checkInterruption();
        // acquire searcher should NOT fail if it is closed when it comes to search
        lock.readLock().lock();
        if ( closed )
        {
            return DefaultIndexerManager.EMPTY_CTX.acquireIndexSearcher();
        }
        else
        {
            return delegate.acquireIndexSearcher();
        }
    }

    @Override
    public void releaseIndexSearcher( IndexSearcher s )
        throws IOException
    {
        // acquire searcher should NOT fail if it is closed when it comes to search
        // not: we were HOLDING read (shared) lock, so it is not possible to have it closed
        // between acquireIndexSearcher() and this method call
        if ( closed )
        {
            DefaultIndexerManager.EMPTY_CTX.releaseIndexSearcher( s );
        }
        else
        {
            try
            {
                // we must release even if closed 1st, since refCounting needs to be notified
                // imple of Lucene refMgr will just decrement the counter
                delegate.releaseIndexSearcher( s );
                // after that above, we can fail to show an apparent bug: this method is invoked when it should not have
                // been
                failIfClosedOrInterrupted();
            }
            finally
            {
                lock.readLock().unlock();
            }
        }
    }

    @Override
    public IndexWriter getIndexWriter()
        throws IOException
    {
        failIfClosedOrInterrupted();
        return delegate.getIndexWriter();
    }

    @Override
    public void commit()
        throws IOException
    {
        failIfClosedOrInterrupted();
        delegate.commit();
    }

    @Override
    public void rollback()
        throws IOException
    {
        failIfClosedOrInterrupted();
        delegate.rollback();
    }

    @Override
    public void optimize()
        throws IOException
    {
        failIfClosedOrInterrupted();
        lock.readLock().lock();
        try
        {
            delegate.optimize();
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    @Override
    public void setAllGroups( Collection<String> groups )
        throws IOException
    {
        failIfClosedOrInterrupted();
        lock.readLock().lock();
        try
        {
            delegate.setAllGroups( groups );
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<String> getAllGroups()
        throws IOException
    {
        failIfClosedOrInterrupted();
        lock.readLock().lock();
        try
        {
            return delegate.getAllGroups();
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    @Override
    public void setRootGroups( Collection<String> groups )
        throws IOException
    {
        failIfClosedOrInterrupted();
        lock.readLock().lock();
        try
        {
            delegate.setRootGroups( groups );
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<String> getRootGroups()
        throws IOException
    {
        failIfClosedOrInterrupted();
        lock.readLock().lock();
        try
        {
            return delegate.getRootGroups();
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    @Override
    public void rebuildGroups()
        throws IOException
    {
        failIfClosedOrInterrupted();
        lock.readLock().lock();
        try
        {
            delegate.rebuildGroups();
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    // these below do not affect indexer persisted state nor internal state (lucene indexes)
    // so are not protected

    @Override
    public String getId()
    {
        return delegate.getId();
    }

    @Override
    public String getRepositoryId()
    {
        return delegate.getRepositoryId();
    }

    @Override
    public File getRepository()
    {
        return delegate.getRepository();
    }

    @Override
    public String getRepositoryUrl()
    {
        return delegate.getRepositoryUrl();
    }

    @Override
    public String getIndexUpdateUrl()
    {
        return delegate.getIndexUpdateUrl();
    }

    @Override
    public boolean isSearchable()
    {
        return delegate.isSearchable();
    }

    @Override
    public void setSearchable( boolean searchable )
    {
        delegate.setSearchable( searchable );
    }

    @Override
    public List<IndexCreator> getIndexCreators()
    {
        return delegate.getIndexCreators();
    }

    @Override
    public Analyzer getAnalyzer()
    {
        return delegate.getAnalyzer();
    }

    @Override
    public File getIndexDirectoryFile()
    {
        return delegate.getIndexDirectoryFile();
    }

    @Override
    public Directory getIndexDirectory()
    {
        return delegate.getIndexDirectory();
    }

    @Override
    public GavCalculator getGavCalculator()
    {
        return delegate.getGavCalculator();
    }

    @Override
    public boolean isReceivingUpdates()
    {
        return delegate.isReceivingUpdates();
    }
}
