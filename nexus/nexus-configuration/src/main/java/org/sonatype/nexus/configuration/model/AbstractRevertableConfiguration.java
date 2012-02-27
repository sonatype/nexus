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
package org.sonatype.nexus.configuration.model;

import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.configuration.RevertableConfiguration;

import com.thoughtworks.xstream.XStream;

public abstract class AbstractRevertableConfiguration
    implements RevertableConfiguration
{
    private final XStream xstream = new XStream();

    private Object originalConfiguration;

    private Object changedConfiguration;

    private ReadWriteLock changedConfigurationLock = new ReentrantReadWriteLock();

    public Lock getConfigurationWriteLock()
    {
        return changedConfigurationLock.writeLock();
    }

    public Lock getConfigurationReadLock()
    {
        return changedConfigurationLock.readLock();
    }

    public Object getConfiguration( boolean forWrite )
    {
        if ( forWrite )
        {
            Lock read = getConfigurationReadLock();
            try
            {
                read.lock();

                Lock write = getConfigurationWriteLock();
                if ( getOriginalConfiguration() != null && getChangedConfiguration() == null )
                {
                    read.unlock();
                    try
                    {
                        write.lock();

                        // when lock is released someone else may have executed this
                        if ( getOriginalConfiguration() != null && getChangedConfiguration() == null )
                        {
                            // copy it
                            setChangedConfiguration( copyObject( getOriginalConfiguration(), null ) );

                            copyTransients( getOriginalConfiguration(), getChangedConfiguration() );
                        }
                    }
                    finally
                    {
                        read.lock();
                        write.unlock();
                    }
                }

                return getChangedConfiguration();
            }
            finally
            {
                read.unlock();
            }
        }
        else
        {
            Lock read = getConfigurationReadLock();
            try
            {
                read.lock();
                return getOriginalConfiguration();
            }
            finally
            {
                read.unlock();
            }
        }
    }

    protected XStream getXStream()
    {
        return xstream;
    }

    protected Object getOriginalConfiguration()
    {
        return originalConfiguration;
    }

    public void setOriginalConfiguration( Object originalConfiguration )
    {
        this.originalConfiguration = originalConfiguration;
    }

    protected Object getChangedConfiguration()
    {
        return changedConfiguration;
    }

    public void setChangedConfiguration( Object changedConfiguration )
    {
        this.changedConfiguration = changedConfiguration;
    }

    @SuppressWarnings( "unchecked" )
    protected Object copyObject( Object source, Object target )
    {
        if ( source == null && target == null )
        {
            return null;
        }
        else if ( source instanceof Collection<?> && target != null )
        {
            // one exception is config object is actually a list, we need to keep the same instance
            ( (Collection) target ).clear();

            ( (Collection) target ).addAll( (Collection) source );

            return target;
        }
        else if ( target == null )
        {
            // "clean" deep copy
            return getXStream().fromXML( getXStream().toXML( source ) );
        }
        else
        {
            // "overlay" actually
            return getXStream().fromXML( getXStream().toXML( source ), target );
        }
    }

    protected void copyTransients( Object source, Object destination )
    {
        // usually none, but see CRepository
    }

    protected boolean isThisDirty()
    {
        return getChangedConfiguration() != null;
    }

    public boolean isDirty()
    {
        return isDirty();
    }

    public void validateChanges()
        throws ConfigurationException
    {
        if ( isThisDirty() )
        {
            Lock lock = getConfigurationReadLock();
            try
            {
                lock.lock();

                checkValidationResponse( doValidateChanges( getChangedConfiguration() ) );
            }
            finally
            {
                lock.unlock();
            }
        }
    }

    public void commitChanges()
        throws ConfigurationException
    {
        Lock lock = getConfigurationWriteLock();
        try
        {
            lock.lock();

            if ( isThisDirty() )
            {
                try
                {
                    checkValidationResponse( doValidateChanges( getChangedConfiguration() ) );
                }
                catch ( ConfigurationException e )
                {
                    rollbackChanges();

                    throw e;
                }

                // nice, isn't it?
                setOriginalConfiguration( copyObject( getChangedConfiguration(), getOriginalConfiguration() ) );

                copyTransients( getChangedConfiguration(), getOriginalConfiguration() );

                setChangedConfiguration( null );
            }
        }
        finally
        {
            lock.unlock();
        }
    }

    public void rollbackChanges()
    {
        Lock lock = getConfigurationWriteLock();
        try
        {
            lock.lock();
            if ( isThisDirty() )
            {
                setChangedConfiguration( null );
            }
        }
        finally
        {
            lock.unlock();
        }
    }

    // ==

    protected void checkValidationResponse( ValidationResponse response )
        throws ConfigurationException
    {
        if ( response != null && !response.isValid() )
        {
            throw new InvalidConfigurationException( response );
        }
    }

    public abstract ValidationResponse doValidateChanges( Object changedConfiguration );
}
