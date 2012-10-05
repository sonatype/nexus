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
package org.sonatype.nexus.util;

public class SystemPropertiesHelper
{
    public final static int getInteger( final String key, final int defaultValue )
    {
        final String value = System.getProperty( key );

        if ( value == null || value.trim().length() == 0 )
        {
            return defaultValue;
        }

        try
        {
            return Integer.valueOf( value );
        }
        catch ( NumberFormatException e )
        {
            return defaultValue;
        }
    }

    public static final int getInteger( final String prefix, final String suffix, final String designator,
                                        final int defaultValue )
    {
        if ( designator != null && designator.trim().length() > 0 )
        {
            return getInteger( prefix + designator + suffix, getInteger( prefix + suffix, defaultValue ) );
        }
        else
        {
            return getInteger( prefix + suffix, defaultValue );
        }
    }

    public final static long getLong( final String key, final long defaultValue )
    {
        final String value = System.getProperty( key );

        if ( value == null || value.trim().length() == 0 )
        {
            return defaultValue;
        }

        try
        {
            return Long.valueOf( value );
        }
        catch ( NumberFormatException e )
        {
            return defaultValue;
        }
    }

    public static final long getLong( final String prefix, final String suffix, final String designator,
                                      final long defaultValue )
    {
        if ( designator != null && designator.trim().length() > 0 )
        {
            return getLong( prefix + designator + suffix, getLong( prefix + suffix, defaultValue ) );
        }
        else
        {
            return getLong( prefix + suffix, defaultValue );
        }
    }

    public final static boolean getBoolean( final String key, final boolean defaultValue )
    {
        final String value = System.getProperty( key );

        if ( value == null || value.trim().length() == 0 )
        {
            return defaultValue;
        }

        return Boolean.valueOf( value );
    }

    public static final boolean getBoolean( final String prefix, final String suffix, final String designator,
                                            final boolean defaultValue )
    {
        if ( designator != null && designator.trim().length() > 0 )
        {
            return getBoolean( prefix + designator + suffix, getBoolean( prefix + suffix, defaultValue ) );
        }
        else
        {
            return getBoolean( prefix + suffix, defaultValue );
        }
    }

    public final static String getString( final String key, final String defaultValue )
    {
        return System.getProperty( key, defaultValue );
    }
}
