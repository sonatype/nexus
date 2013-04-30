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
package org.sonatype.nexus.configuration.model;

import java.util.Collection;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.configuration.RevertableConfiguration;

import com.thoughtworks.xstream.XStream;

public abstract class AbstractRevertableConfiguration
    implements RevertableConfiguration
{
    private final XStream xstream = new XStream();

    private volatile Object originalConfiguration;

    private volatile Object changedConfiguration;

    public Object getConfiguration( boolean forWrite )
    {
        if ( forWrite )
        {
            synchronized ( this )
            {
                if ( getOriginalConfiguration() != null && getChangedConfiguration() == null )
                {
                    // copy it
                    final Object copy = copyObject( getOriginalConfiguration(), null );

                    copyTransients( getOriginalConfiguration(), copy );

                    setChangedConfiguration( copy );
                }
            }

            return getChangedConfiguration();

        }
        else
        {
            return getOriginalConfiguration();
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
        return isThisDirty();
    }

    public void validateChanges()
        throws ConfigurationException
    {
        if ( isThisDirty() )
        {
            checkValidationResponse( doValidateChanges( getChangedConfiguration() ) );
        }
    }

    public synchronized void commitChanges()
        throws ConfigurationException
    {
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
            copyObject( getChangedConfiguration(),  getOriginalConfiguration() );

            copyTransients( getChangedConfiguration(), getOriginalConfiguration() );

            setChangedConfiguration( null );
        }
    }

    public void rollbackChanges()
    {
        if ( isThisDirty() )
        {
            setChangedConfiguration( null );
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
