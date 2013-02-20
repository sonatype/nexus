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
package org.sonatype.nexus.apachehttpclient;

import java.lang.management.ManagementFactory;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An wrapper {@link Hc4Provider} that automatically registers / unregisters JMX MBeans for each created
 * {@link HttpClient}s and {@link PoolingClientConnectionManager}.
 *
 * @since 2.2
 */
@Named
@Singleton
public class PoolingClientConnectionManagerMBeanInstaller
{

    private static final Logger LOGGER = LoggerFactory.getLogger( PoolingClientConnectionManagerMBeanInstaller.class );

    private static final String JMX_DOMAIN = "org.sonatype.nexus.httpclient";

    /**
     * Registers the connection manager to JMX.
     *
     * @param connectionManager
     */
    public synchronized void register( final PoolingClientConnectionManager connectionManager )
    {
        try
        {
            final ObjectName jmxName = getObjectName( connectionManager );

            final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            server.registerMBean( new PoolingClientConnectionManagerMBeanImpl( connectionManager ), jmxName );
        }
        catch ( final Exception e )
        {
            LOGGER.warn( "Failed to register connection manager MBean due to {}:{}", e.getClass(), e.getMessage() );
        }
    }

    /**
     * Unregisters the connection manager from JMX.
     *
     * @since 2.4
     */
    public synchronized void unregister( final PoolingClientConnectionManager connectionManager )
    {
        try
        {
            final ObjectName jmxName = getObjectName( connectionManager );
            final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            server.unregisterMBean( jmxName );
        }
        catch ( final Exception e )
        {
            LOGGER.warn( "Failed to unregister connection manager MBean due to {}:{}", e.getClass(), e.getMessage() );
        }
    }

    private ObjectName getObjectName( final PoolingClientConnectionManager connectionManager )
        throws MalformedObjectNameException
    {
        return ObjectName.getInstance(
            JMX_DOMAIN,
            "name",
            PoolingClientConnectionManager.class.getSimpleName() + "@" + System.identityHashCode( connectionManager )
        );
    }

}
