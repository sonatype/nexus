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
package org.apache.commons.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.util.IdleConnectionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages a set of HttpConnections for various HostConfigurations.
 *
 * @author <a href="mailto:becke@u.washington.edu">Michael Becke</a>
 * @author Eric Johnson
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author Carl A. Dunham
 * @author Brian Demers
 *
 * @deprecated Use httpclient4 components instead
 */
@Deprecated
public class CustomMultiThreadedHttpConnectionManager implements HttpConnectionManager {

    // -------------------------------------------------------- Class Variables

    /** Log object for this class. */
    private static final Logger LOG = LoggerFactory.getLogger(CustomMultiThreadedHttpConnectionManager.class);

    /** The default maximum number of connections allowed per host */
    public static final int DEFAULT_MAX_HOST_CONNECTIONS = 2;   // Per RFC 2616 sec 8.1.4

    /** The default maximum number of connections allowed overall */
    public static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 20;
    
    public static final String NON_PROXY_HOSTS_PATTERNS_KEY = "http.nonProxyHosts.patterns";

    /**
     * A mapping from Reference to ConnectionSource.  Used to reclaim resources when connections
     * are lost to the garbage collector.
     */
    private static final Map REFERENCE_TO_CONNECTION_SOURCE = new HashMap();
    
    /**
     * The reference queue used to track when HttpConnections are lost to the
     * garbage collector
     */
    private static final ReferenceQueue REFERENCE_QUEUE = new ReferenceQueue();    

    /**
     * The thread responsible for handling lost connections.
     */
    private static ReferenceQueueThread REFERENCE_QUEUE_THREAD;
    
    /**
     * Holds references to all active instances of this class.
     */    
    private static WeakHashMap ALL_CONNECTION_MANAGERS = new WeakHashMap();
    

    // ---------------------------------------------------------- Class Methods

    /**
     * Shuts down and cleans up resources used by all instances of 
     * MultiThreadedHttpConnectionManager. All static resources are released, all threads are 
     * stopped, and {@link #shutdown()} is called on all live instances of 
     * MultiThreadedHttpConnectionManager.
     *
     * @see #shutdown()
     */
    public static void shutdownAll() {

        synchronized (REFERENCE_TO_CONNECTION_SOURCE) {
            // shutdown all connection managers
            synchronized (ALL_CONNECTION_MANAGERS) {
                // Don't use an iterator here. Iterators on WeakHashMap can
                // get ConcurrentModificationException on garbage collection.
                CustomMultiThreadedHttpConnectionManager[]
                    connManagers = (CustomMultiThreadedHttpConnectionManager[])
                    ALL_CONNECTION_MANAGERS.keySet().toArray(
                        new CustomMultiThreadedHttpConnectionManager
                            [ALL_CONNECTION_MANAGERS.size()]
                        );

                // The map may shrink after size() is called, or some entry
                // may get GCed while the array is built, so expect null.
                for (int i=0; i<connManagers.length; i++) {
                    if (connManagers[i] != null)
                        connManagers[i].shutdown();
                }
            }
            
            // shutdown static resources
            if (REFERENCE_QUEUE_THREAD != null) {
                REFERENCE_QUEUE_THREAD.shutdown();
                REFERENCE_QUEUE_THREAD = null;
            }
            REFERENCE_TO_CONNECTION_SOURCE.clear();
        }        
    }    
    
    /**
     * Stores the reference to the given connection along with the host config and connection pool.  
     * These values will be used to reclaim resources if the connection is lost to the garbage 
     * collector.  This method should be called before a connection is released from the connection 
     * manager.
     * 
     * <p>A static reference to the connection manager will also be stored.  To ensure that
     * the connection manager can be GCed {@link #removeReferenceToConnection(HttpConnection)}
     * should be called for all connections that the connection manager is storing a reference
     * to.</p>
     * 
     * @param connection the connection to create a reference for
     * @param hostConfiguration the connection's host config
     * @param connectionPool the connection pool that created the connection
     * 
     * @see #removeReferenceToConnection(HttpConnection)
     */
    private static void storeReferenceToConnection(
        HttpConnectionWithReference connection,
        HostConfiguration hostConfiguration,
        ConnectionPool connectionPool
    ) {
        
        ConnectionSource source = new ConnectionSource();
        source.connectionPool = connectionPool;
        source.hostConfiguration = hostConfiguration;
        
        synchronized (REFERENCE_TO_CONNECTION_SOURCE) {
            
            // start the reference queue thread if needed
            if (REFERENCE_QUEUE_THREAD == null) {
                REFERENCE_QUEUE_THREAD = new ReferenceQueueThread();
                REFERENCE_QUEUE_THREAD.start();
            }
            
            REFERENCE_TO_CONNECTION_SOURCE.put(
                connection.reference,
                source
            );
        }
    }
    
    /**
     * Closes and releases all connections currently checked out of the given connection pool.
     * @param connectionPool the connection pool to shutdown the connections for
     */
    private static void shutdownCheckedOutConnections(ConnectionPool connectionPool) {

        // keep a list of the connections to be closed
        ArrayList connectionsToClose = new ArrayList(); 
        
        synchronized (REFERENCE_TO_CONNECTION_SOURCE) {
            
            Iterator referenceIter = REFERENCE_TO_CONNECTION_SOURCE.keySet().iterator();
            while (referenceIter.hasNext()) {
                Reference ref = (Reference) referenceIter.next();
                ConnectionSource source = 
                    (ConnectionSource) REFERENCE_TO_CONNECTION_SOURCE.get(ref);
                if (source.connectionPool == connectionPool) {
                    referenceIter.remove();
                    HttpConnection connection = (HttpConnection) ref.get();
                    if (connection != null) {
                        connectionsToClose.add(connection);
                    }
                }
            }
        }

        // close and release the connections outside of the synchronized block to
        // avoid holding the lock for too long
        for (Iterator i = connectionsToClose.iterator(); i.hasNext();) {
            HttpConnection connection = (HttpConnection) i.next();
            connection.close();
            // remove the reference to the connection manager. this ensures
            // that the we don't accidentally end up here again
            connection.setHttpConnectionManager(null);
            connection.releaseConnection();
        }
    }
    
    /**
     * Removes the reference being stored for the given connection.  This method should be called
     * when the connection manager again has a direct reference to the connection.
     * 
     * @param connection the connection to remove the reference for
     * 
     * @see #storeReferenceToConnection(HttpConnection, HostConfiguration, ConnectionPool)
     */
    private static void removeReferenceToConnection(HttpConnectionWithReference connection) {
        
        synchronized (REFERENCE_TO_CONNECTION_SOURCE) {
            REFERENCE_TO_CONNECTION_SOURCE.remove(connection.reference);
        }
    }    
    

    // ----------------------------------------------------- Instance Variables

    /**
     * Collection of parameters associated with this connection manager.
     */
    private HttpConnectionManagerParams params = new HttpConnectionManagerParams(); 

    /** Connection Pool */
    private ConnectionPool connectionPool;

    private volatile boolean shutdown = false;
    

    // ----------------------------------------------------------- Constructors

    /**
     * No-args constructor
     */
    public CustomMultiThreadedHttpConnectionManager() {
        this.connectionPool = new ConnectionPool();
        synchronized(ALL_CONNECTION_MANAGERS) {
            ALL_CONNECTION_MANAGERS.put(this, null);
        }
    }


    // ------------------------------------------------------- Instance Methods

    /**
     * Shuts down the connection manager and releases all resources.  All connections associated 
     * with this class will be closed and released. 
     * 
     * <p>The connection manager can no longer be used once shut down.  
     * 
     * <p>Calling this method more than once will have no effect.
     */
    public synchronized void shutdown() {
        synchronized (connectionPool) {
            if (!shutdown) {
                shutdown = true;
                connectionPool.shutdown();
            }
        }
    }
    
    /**
     * Gets the staleCheckingEnabled value to be set on HttpConnections that are created.
     * 
     * @return <code>true</code> if stale checking will be enabled on HttpConnections
     * 
     * @see HttpConnection#isStaleCheckingEnabled()
     * 
     * @deprecated Use {@link HttpConnectionManagerParams#isStaleCheckingEnabled()},
     * {@link HttpConnectionManager#getParams()}.
     */
    public boolean isConnectionStaleCheckingEnabled() {
        return this.params.isStaleCheckingEnabled();
    }

    /**
     * Sets the staleCheckingEnabled value to be set on HttpConnections that are created.
     * 
     * @param connectionStaleCheckingEnabled <code>true</code> if stale checking will be enabled 
     * on HttpConnections
     * 
     * @see HttpConnection#setStaleCheckingEnabled(boolean)
     * 
     * @deprecated Use {@link HttpConnectionManagerParams#setStaleCheckingEnabled(boolean)},
     * {@link HttpConnectionManager#getParams()}.
     */
    public void setConnectionStaleCheckingEnabled(boolean connectionStaleCheckingEnabled) {
        this.params.setStaleCheckingEnabled(connectionStaleCheckingEnabled);
    }

    /**
     * Sets the maximum number of connections allowed for a given
     * HostConfiguration. Per RFC 2616 section 8.1.4, this value defaults to 2.
     *
     * @param maxHostConnections the number of connections allowed for each
     * hostConfiguration
     * 
     * @deprecated Use {@link HttpConnectionManagerParams#setDefaultMaxConnectionsPerHost(int)},
     * {@link HttpConnectionManager#getParams()}.
     */
    public void setMaxConnectionsPerHost(int maxHostConnections) {
        this.params.setDefaultMaxConnectionsPerHost(maxHostConnections);
    }

    /**
     * Gets the maximum number of connections allowed for a given
     * hostConfiguration.
     *
     * @return The maximum number of connections allowed for a given
     * hostConfiguration.
     * 
     * @deprecated Use {@link HttpConnectionManagerParams#getDefaultMaxConnectionsPerHost()},
     * {@link HttpConnectionManager#getParams()}.
     */
    public int getMaxConnectionsPerHost() {
        return this.params.getDefaultMaxConnectionsPerHost();
    }

    /**
     * Sets the maximum number of connections allowed for this connection manager.
     *
     * @param maxTotalConnections the maximum number of connections allowed
     * 
     * @deprecated Use {@link HttpConnectionManagerParams#setMaxTotalConnections(int)},
     * {@link HttpConnectionManager#getParams()}.
     */
    public void setMaxTotalConnections(int maxTotalConnections) {
        this.params.setMaxTotalConnections(maxTotalConnections);
    }

    /**
     * Gets the maximum number of connections allowed for this connection manager.
     *
     * @return The maximum number of connections allowed
     * 
     * @deprecated Use {@link HttpConnectionManagerParams#getMaxTotalConnections()},
     * {@link HttpConnectionManager#getParams()}.
     */
    public int getMaxTotalConnections() {
        return this.params.getMaxTotalConnections();
    }

    /**
     * @see HttpConnectionManager#getConnection(HostConfiguration)
     */
    public HttpConnection getConnection(HostConfiguration hostConfiguration) {

        while (true) {
            try {
                return getConnectionWithTimeout(hostConfiguration, 0);
            } catch (ConnectionPoolTimeoutException e) {
                // we'll go ahead and log this, but it should never happen. HttpExceptions
                // are only thrown when the timeout occurs and since we have no timeout
                // it should never happen.
                LOG.debug(
                    "Unexpected exception while waiting for connection",
                    e
                );
            }
        }
    }

    /**
     * Gets a connection or waits if one is not available.  A connection is
     * available if one exists that is not being used or if fewer than
     * maxHostConnections have been created in the connectionPool, and fewer
     * than maxTotalConnections have been created in all connectionPools.
     *
     * @param hostConfiguration The host configuration specifying the connection
     *        details.
     * @param timeout the number of milliseconds to wait for a connection, 0 to
     * wait indefinitely
     *
     * @return HttpConnection an available connection
     *
     * @throws HttpException if a connection does not become available in
     * 'timeout' milliseconds
     * 
     * @since 3.0
     */
    public HttpConnection getConnectionWithTimeout(HostConfiguration hostConfiguration, 
        long timeout) throws ConnectionPoolTimeoutException {

        LOG.trace("enter HttpConnectionManager.getConnectionWithTimeout(HostConfiguration, long)");

        if (hostConfiguration == null) {
            throw new IllegalArgumentException("hostConfiguration is null");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("HttpConnectionManager.getConnection:  config = "
                + hostConfiguration + ", timeout = " + timeout);
        }

        final HttpConnection conn = doGetConnection(hostConfiguration, timeout);

        // wrap the connection in an adapter so we can ensure it is used 
        // only once
        return new HttpConnectionAdapter(conn);
    }

	/**
	 * @see HttpConnectionManager#getConnection(HostConfiguration, long)
	 * 
	 * @deprecated Use #getConnectionWithTimeout(HostConfiguration, long)
	 */
	public HttpConnection getConnection(HostConfiguration hostConfiguration, 
		long timeout) throws HttpException {

		LOG.trace("enter HttpConnectionManager.getConnection(HostConfiguration, long)");
		try {
			return getConnectionWithTimeout(hostConfiguration, timeout);
		} catch(ConnectionPoolTimeoutException e) {
			throw new HttpException(e.getMessage());
		}
	}

    private HttpConnection doGetConnection(HostConfiguration hostConfiguration, 
        long timeout) throws ConnectionPoolTimeoutException {

        HttpConnection connection = null;

        int maxHostConnections = this.params.getMaxConnectionsPerHost(hostConfiguration);
        int maxTotalConnections = this.params.getMaxTotalConnections();
        
        synchronized (connectionPool) {

            // we clone the hostConfiguration
            // so that it cannot be changed once the connection has been retrieved
            hostConfiguration = new HostConfiguration(hostConfiguration);
            if( this.isNonProxyHost( hostConfiguration ))
            {
                hostConfiguration.setProxyHost( null );
            }
            HostConnectionPool hostPool = connectionPool.getHostPool(hostConfiguration, true);
            WaitingThread waitingThread = null;

            boolean useTimeout = (timeout > 0);
            long timeToWait = timeout;
            long startWait = 0;
            long endWait = 0;

            while (connection == null) {

                if (shutdown) {
                    throw new IllegalStateException("Connection factory has been shutdown.");
                }
                
                // happen to have a free connection with the right specs
                //
                if (hostPool.freeConnections.size() > 0) {
                    connection = connectionPool.getFreeConnection(hostConfiguration);

                // have room to make more
                //
                } else if ((hostPool.numConnections < maxHostConnections) 
                    && (connectionPool.numConnections < maxTotalConnections)) {

                    connection = connectionPool.createConnection(hostConfiguration);

                // have room to add host connection, and there is at least one free
                // connection that can be liberated to make overall room
                //
                } else if ((hostPool.numConnections < maxHostConnections) 
                    && (connectionPool.freeConnections.size() > 0)) {

                    connectionPool.deleteLeastUsedConnection();
                    connection = connectionPool.createConnection(hostConfiguration);

                // otherwise, we have to wait for one of the above conditions to
                // become true
                //
                } else {
                    // TODO: keep track of which hostConfigurations have waiting
                    // threads, so they avoid being sacrificed before necessary

                    try {
                        
                        if (useTimeout && timeToWait <= 0) {
                            throw new ConnectionPoolTimeoutException("Timeout waiting for connection");
                        }
                        
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Unable to get a connection, waiting..., hostConfig=" + hostConfiguration);
                        }
                        
                        if (waitingThread == null) {
                            waitingThread = new WaitingThread();
                            waitingThread.hostConnectionPool = hostPool;
                            waitingThread.thread = Thread.currentThread();
                        } else {
                            waitingThread.interruptedByConnectionPool = false;
                        }
                                    
                        if (useTimeout) {
                            startWait = System.currentTimeMillis();
                        }
                        
                        hostPool.waitingThreads.addLast(waitingThread);
                        connectionPool.waitingThreads.addLast(waitingThread);
                        connectionPool.wait(timeToWait);
                    } catch (InterruptedException e) {
                        if (!waitingThread.interruptedByConnectionPool) {
                            LOG.debug("Interrupted while waiting for connection", e);
                            throw new IllegalThreadStateException(
                                "Interrupted while waiting in MultiThreadedHttpConnectionManager");
                        }
                        // Else, do nothing, we were interrupted by the connection pool
                        // and should now have a connection waiting for us, continue
                        // in the loop and let's get it.
                    } finally {
                        if (!waitingThread.interruptedByConnectionPool) {
                            // Either we timed out, experienced a "spurious wakeup", or were
                            // interrupted by an external thread.  Regardless we need to 
                            // cleanup for ourselves in the wait queue.
                            hostPool.waitingThreads.remove(waitingThread);
                            connectionPool.waitingThreads.remove(waitingThread);
                        }
                        
                        if (useTimeout) {
                            endWait = System.currentTimeMillis();
                            timeToWait -= (endWait - startWait);
                        }
                    }
                }
            }
        }
        return connection;
    }

    /**
     * Gets the total number of pooled connections for the given host configuration.  This 
     * is the total number of connections that have been created and are still in use 
     * by this connection manager for the host configuration.  This value will
     * not exceed the {@link #getMaxConnectionsPerHost() maximum number of connections per
     * host}.
     * 
     * @param hostConfiguration The host configuration
     * @return The total number of pooled connections
     */
    public int getConnectionsInPool(HostConfiguration hostConfiguration) {
        synchronized (connectionPool) {
            HostConnectionPool hostPool = connectionPool.getHostPool(hostConfiguration, false);
            return (hostPool != null) ? hostPool.numConnections : 0;
        }
    }

    /**
     * Gets the total number of pooled connections.  This is the total number of 
     * connections that have been created and are still in use by this connection 
     * manager.  This value will not exceed the {@link #getMaxTotalConnections() 
     * maximum number of connections}.
     * 
     * @return the total number of pooled connections
     */
    public int getConnectionsInPool() {
        synchronized (connectionPool) {
            return connectionPool.numConnections;
        }
    }
    
    /**
     * Gets the number of connections in use for this configuration.
     *
     * @param hostConfiguration the key that connections are tracked on
     * @return the number of connections in use
     * 
     * @deprecated Use {@link #getConnectionsInPool(HostConfiguration)}
     */
    public int getConnectionsInUse(HostConfiguration hostConfiguration) {
        return getConnectionsInPool(hostConfiguration);
    }

    /**
     * Gets the total number of connections in use.
     * 
     * @return the total number of connections in use
     * 
     * @deprecated Use {@link #getConnectionsInPool()}
     */
    public int getConnectionsInUse() {
        return getConnectionsInPool();
    }

    /**
     * Deletes all closed connections.  Only connections currently owned by the connection
     * manager are processed.
     * 
     * @see HttpConnection#isOpen()
     * 
     * @since 3.0
     */
    public void deleteClosedConnections() {
        connectionPool.deleteClosedConnections();
    }
    
    /**
     * @since 3.0
     */
    public void closeIdleConnections(long idleTimeout) {
        connectionPool.closeIdleConnections(idleTimeout);
        deleteClosedConnections();
    }
    
    /**
     * Make the given HttpConnection available for use by other requests.
     * If another thread is blocked in getConnection() that could use this
     * connection, it will be woken up.
     *
     * @param conn the HttpConnection to make available.
     */
    public void releaseConnection(HttpConnection conn) {
        LOG.trace("enter HttpConnectionManager.releaseConnection(HttpConnection)");

        if (conn instanceof HttpConnectionAdapter) {
            // connections given out are wrapped in an HttpConnectionAdapter
            conn = ((HttpConnectionAdapter) conn).getWrappedConnection();
        } else {
            // this is okay, when an HttpConnectionAdapter is released
            // is releases the real connection
        }

        // make sure that the response has been read.
        SimpleHttpConnectionManager.finishLastResponse(conn);

        connectionPool.freeConnection(conn);
    }

    /**
     * Gets the host configuration for a connection.
     * @param conn the connection to get the configuration of
     * @return a new HostConfiguration
     */
    private HostConfiguration configurationForConnection(HttpConnection conn) {
        
        HostConfiguration connectionConfiguration = new HostConfiguration();
        
        connectionConfiguration.setHost(
            conn.getHost(), 
            conn.getPort(), 
            conn.getProtocol()
        );
        if (conn.getLocalAddress() != null) {
            connectionConfiguration.setLocalAddress(conn.getLocalAddress());
        }
        if (conn.getProxyHost() != null) {
            connectionConfiguration.setProxy(conn.getProxyHost(), conn.getProxyPort());
        }

        return connectionConfiguration;
    }
    
    @SuppressWarnings( "unchecked" )
    private boolean isNonProxyHost( HostConfiguration hostConfiguration ) {
        
        if( hostConfiguration == null || hostConfiguration.getHost() == null ) {
            return false;
        }
        
        Collection<Pattern> nonProxyHostPatterns = (Collection<Pattern>) hostConfiguration.getParams().getParameter( NON_PROXY_HOSTS_PATTERNS_KEY );
        if( nonProxyHostPatterns != null ) {
            
            for ( Pattern hostPattern : nonProxyHostPatterns ) {
                
                if( hostPattern.matcher( hostConfiguration.getHost() ).matches() ) {
                    
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns {@link HttpConnectionManagerParams parameters} associated 
     * with this connection manager.
     * 
     * @since 3.0
     * 
     * @see HttpConnectionManagerParams
     */
    public HttpConnectionManagerParams getParams() {
        return this.params;
    }

    /**
     * Assigns {@link HttpConnectionManagerParams parameters} for this 
     * connection manager.
     * 
     * @since 3.0
     * 
     * @see HttpConnectionManagerParams
     */
    public void setParams(final HttpConnectionManagerParams params) {
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null");
        }
        this.params = params;
    }
    
    /**
     * Global Connection Pool, including per-host pools
     */
    private class ConnectionPool {
        
        /** The list of free connections */
        private LinkedList freeConnections = new LinkedList();

        /** The list of WaitingThreads waiting for a connection */
        private LinkedList waitingThreads = new LinkedList();

        /**
         * Map where keys are {@link HostConfiguration}s and values are {@link
         * HostConnectionPool}s
         */
        private final Map mapHosts = new HashMap();

        private IdleConnectionHandler idleConnectionHandler = new IdleConnectionHandler();        
        
        /** The number of created connections */
        private int numConnections = 0;

        /**
         * Cleans up all connection pool resources.
         */
        public synchronized void shutdown() {
            
            // close all free connections
            Iterator iter = freeConnections.iterator();
            while (iter.hasNext()) {
                HttpConnection conn = (HttpConnection) iter.next();
                iter.remove();
                conn.close();
            }
            
            // close all connections that have been checked out
            shutdownCheckedOutConnections(this);
            
            // interrupt all waiting threads
            iter = waitingThreads.iterator();
            while (iter.hasNext()) {
                WaitingThread waiter = (WaitingThread) iter.next();
                iter.remove();
                waiter.interruptedByConnectionPool = true;
                waiter.thread.interrupt();
            }
            
            // clear out map hosts
            mapHosts.clear();
            
            // remove all references to connections
            idleConnectionHandler.removeAll();
        }
        
        /**
         * Creates a new connection and returns it for use of the calling method.
         *
         * @param hostConfiguration the configuration for the connection
         * @return a new connection or <code>null</code> if none are available
         */
        public synchronized HttpConnection createConnection(HostConfiguration hostConfiguration) {
            HostConnectionPool hostPool = getHostPool(hostConfiguration, true);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Allocating new connection, hostConfig=" + hostConfiguration);
            }
            HttpConnectionWithReference connection = new HttpConnectionWithReference(
                    hostConfiguration);
            connection.getParams().setDefaults(CustomMultiThreadedHttpConnectionManager.this.params);
            connection.setHttpConnectionManager(CustomMultiThreadedHttpConnectionManager.this);
            numConnections++;
            hostPool.numConnections++;
    
            // store a reference to this connection so that it can be cleaned up
            // in the event it is not correctly released
            storeReferenceToConnection(connection, hostConfiguration, this);
            return connection;
        }
    
        /**
         * Handles cleaning up for a lost connection with the given config.  Decrements any 
         * connection counts and notifies waiting threads, if appropriate.
         * 
         * @param config the host configuration of the connection that was lost
         */
        public synchronized void handleLostConnection(HostConfiguration config) {
            HostConnectionPool hostPool = getHostPool(config, true);
            hostPool.numConnections--;
            if ((hostPool.numConnections == 0) &&
                hostPool.waitingThreads.isEmpty()) {

                mapHosts.remove(config);
            }
            
            numConnections--;
            notifyWaitingThread(config);
        }

        /**
         * Get the pool (list) of connections available for the given hostConfig.
         *
         * @param hostConfiguration the configuraton for the connection pool
         * @param create <code>true</code> to create a pool if not found,
         *               <code>false</code> to return <code>null</code>
         *
         * @return a pool (list) of connections available for the given config,
         *         or <code>null</code> if neither found nor created
         */
        public synchronized HostConnectionPool getHostPool(HostConfiguration hostConfiguration, boolean create) {
            LOG.trace("enter HttpConnectionManager.ConnectionPool.getHostPool(HostConfiguration)");

            // Look for a list of connections for the given config
            HostConnectionPool listConnections = (HostConnectionPool) 
                mapHosts.get(hostConfiguration);
            if ((listConnections == null) && create) {
                // First time for this config
                listConnections = new HostConnectionPool();
                listConnections.hostConfiguration = hostConfiguration;
                mapHosts.put(hostConfiguration, listConnections);
            }
            
            return listConnections;
        }

        /**
         * If available, get a free connection for this host
         *
         * @param hostConfiguration the configuraton for the connection pool
         * @return an available connection for the given config
         */
        public synchronized HttpConnection getFreeConnection(HostConfiguration hostConfiguration) {

            HttpConnectionWithReference connection = null;
            
            HostConnectionPool hostPool = getHostPool(hostConfiguration, false);

            if ((hostPool != null) && (hostPool.freeConnections.size() > 0)) {
                connection = (HttpConnectionWithReference) hostPool.freeConnections.removeLast();
                freeConnections.remove(connection);
                // store a reference to this connection so that it can be cleaned up
                // in the event it is not correctly released
                storeReferenceToConnection(connection, hostConfiguration, this);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Getting free connection, hostConfig=" + hostConfiguration);
                }

                // remove the connection from the timeout handler
                idleConnectionHandler.remove(connection);
            } else if (LOG.isDebugEnabled()) {
                LOG.debug("There were no free connections to get, hostConfig=" 
                    + hostConfiguration);
            }
            return connection;
        }
        
        /**
         * Deletes all closed connections.
         */        
        public synchronized void deleteClosedConnections() {
            
            Iterator iter = freeConnections.iterator();
            
            while (iter.hasNext()) {
                HttpConnection conn = (HttpConnection) iter.next();
                if (!conn.isOpen()) {
                    iter.remove();
                    deleteConnection(conn);
                }
            }
        }

        /**
         * Closes idle connections.
         * @param idleTimeout
         */
        public synchronized void closeIdleConnections(long idleTimeout) {
            idleConnectionHandler.closeIdleConnections(idleTimeout);
        }
        
        /**
         * Deletes the given connection.  This will remove all reference to the connection
         * so that it can be GCed.
         * 
         * <p><b>Note:</b> Does not remove the connection from the freeConnections list.  It
         * is assumed that the caller has already handled this case.</p>
         * 
         * @param connection The connection to delete
         */
        private synchronized void deleteConnection(HttpConnection connection) {
            
            HostConfiguration connectionConfiguration = configurationForConnection(connection);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Reclaiming connection, hostConfig=" + connectionConfiguration);
            }

            connection.close();

            HostConnectionPool hostPool = getHostPool(connectionConfiguration, true);
            
            hostPool.freeConnections.remove(connection);
            hostPool.numConnections--;
            numConnections--;
            if ((hostPool.numConnections == 0) &&
                hostPool.waitingThreads.isEmpty()) {

                mapHosts.remove(connectionConfiguration);
            }
            
            // remove the connection from the timeout handler
            idleConnectionHandler.remove(connection);            
        }
        
        /**
         * Close and delete an old, unused connection to make room for a new one.
         */
        public synchronized void deleteLeastUsedConnection() {

            HttpConnection connection = (HttpConnection) freeConnections.removeFirst();

            if (connection != null) {
                deleteConnection(connection);
            } else if (LOG.isDebugEnabled()) {
                LOG.debug("Attempted to reclaim an unused connection but there were none.");
            }
        }

        /**
         * Notifies a waiting thread that a connection for the given configuration is 
         * available.
         * @param configuration the host config to use for notifying
         * @see #notifyWaitingThread(HostConnectionPool)
         */
        public synchronized void notifyWaitingThread(HostConfiguration configuration) {
            notifyWaitingThread(getHostPool(configuration, true));
        }

        /**
         * Notifies a waiting thread that a connection for the given configuration is 
         * available.  This will wake a thread waiting in this host pool or if there is not
         * one a thread in the connection pool will be notified.
         * 
         * @param hostPool the host pool to use for notifying
         */
        public synchronized void notifyWaitingThread(HostConnectionPool hostPool) {

            // find the thread we are going to notify, we want to ensure that each
            // waiting thread is only interrupted once so we will remove it from 
            // all wait queues before interrupting it
            WaitingThread waitingThread = null;
                
            if (hostPool.waitingThreads.size() > 0) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Notifying thread waiting on host pool, hostConfig=" 
                        + hostPool.hostConfiguration);
                }
                waitingThread = (WaitingThread) hostPool.waitingThreads.removeFirst();
                waitingThreads.remove(waitingThread);
            } else if (waitingThreads.size() > 0) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No-one waiting on host pool, notifying next waiting thread.");
                }
                waitingThread = (WaitingThread) waitingThreads.removeFirst();
                waitingThread.hostConnectionPool.waitingThreads.remove(waitingThread);
            } else if (LOG.isDebugEnabled()) {
                LOG.debug("Notifying no-one, there are no waiting threads");
            }
                
            if (waitingThread != null) {
                waitingThread.interruptedByConnectionPool = true;
                waitingThread.thread.interrupt();
            }
        }

        /**
         * Marks the given connection as free.
         * @param conn a connection that is no longer being used
         */
        public void freeConnection(HttpConnection conn) {

            HostConfiguration connectionConfiguration = configurationForConnection(conn);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Freeing connection, hostConfig=" + connectionConfiguration);
            }

            synchronized (this) {
                
                if (shutdown) {
                    // the connection manager has been shutdown, release the connection's
                    // resources and get out of here
                    conn.close();
                    return;
                }
                
                HostConnectionPool hostPool = getHostPool(connectionConfiguration, true);

                // Put the connect back in the available list and notify a waiter
                hostPool.freeConnections.add(conn);
                if (hostPool.numConnections == 0) {
                    // for some reason this connection pool didn't already exist
                    LOG.error("Host connection pool not found, hostConfig=" 
                              + connectionConfiguration);
                    hostPool.numConnections = 1;
                }

                freeConnections.add(conn);
                // we can remove the reference to this connection as we have control over
                // it again.  this also ensures that the connection manager can be GCed
                removeReferenceToConnection((HttpConnectionWithReference) conn);
                if (numConnections == 0) {
                    // for some reason this connection pool didn't already exist
                    LOG.error("Host connection pool not found, hostConfig=" 
                              + connectionConfiguration);
                    numConnections = 1;
                }

                // register the connection with the timeout handler
                idleConnectionHandler.add(conn);

                notifyWaitingThread(hostPool);
            }
        }
    }

    /**
     * A simple struct-like class to combine the objects needed to release a connection's
     * resources when claimed by the garbage collector.
     */
    private static class ConnectionSource {
        
        /** The connection pool that created the connection */
        public ConnectionPool connectionPool;

        /** The connection's host configuration */
        public HostConfiguration hostConfiguration;
    }
    
    /**
     * A simple struct-like class to combine the connection list and the count
     * of created connections.
     */
    private static class HostConnectionPool {
        /** The hostConfig this pool is for */
        public HostConfiguration hostConfiguration;
        
        /** The list of free connections */
        public LinkedList freeConnections = new LinkedList();
        
        /** The list of WaitingThreads for this host */
        public LinkedList waitingThreads = new LinkedList();

        /** The number of created connections */
        public int numConnections = 0;
    }
    
    /**
     * A simple struct-like class to combine the waiting thread and the connection 
     * pool it is waiting on.
     */
    private static class WaitingThread {
        /** The thread that is waiting for a connection */
        public Thread thread;
        
        /** The connection pool the thread is waiting for */
        public HostConnectionPool hostConnectionPool;
        
        /** Flag to indicate if the thread was interrupted by the ConnectionPool. Set
         * to true inside {@link ConnectionPool#notifyWaitingThread(HostConnectionPool)} 
         * before the thread is interrupted. */
        public boolean interruptedByConnectionPool = false;
    }

    /**
     * A thread for listening for HttpConnections reclaimed by the garbage
     * collector.
     */
    private static class ReferenceQueueThread extends Thread {

        private volatile boolean shutdown = false;
        
        /**
         * Create an instance and make this a daemon thread.
         */
        public ReferenceQueueThread() {
            setDaemon(true);
            setName("MultiThreadedHttpConnectionManager cleanup");
        }

        public void shutdown() {
            this.shutdown = true;
            this.interrupt();
        }
        
        /**
         * Handles cleaning up for the given connection reference.
         * 
         * @param ref the reference to clean up
         */
        private void handleReference(Reference ref) {
            
            ConnectionSource source = null;
            
            synchronized (REFERENCE_TO_CONNECTION_SOURCE) {
                source = (ConnectionSource) REFERENCE_TO_CONNECTION_SOURCE.remove(ref);
            }
            // only clean up for this reference if it is still associated with 
            // a ConnectionSource
            if (source != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(
                        "Connection reclaimed by garbage collector, hostConfig=" 
                        + source.hostConfiguration);
                }
                
                source.connectionPool.handleLostConnection(source.hostConfiguration);
            }
        }

        /**
         * Start execution.
         */
        public void run() {
            while (!shutdown) {
                try {
                    // remove the next reference and process it
                    Reference ref = REFERENCE_QUEUE.remove();
                    if (ref != null) {
                        handleReference(ref);
                    }
                } catch (InterruptedException e) {
                    LOG.debug("ReferenceQueueThread interrupted", e);
                }
            }
        }

    }
    
    /**
     * A connection that keeps a reference to itself.
     */
    private static class HttpConnectionWithReference extends HttpConnection {
        
        public WeakReference reference = new WeakReference(this, REFERENCE_QUEUE);
        
        /**
         * @param hostConfiguration
         */
        public HttpConnectionWithReference(HostConfiguration hostConfiguration) {
            super(hostConfiguration);
        }

    }
    
    /**
     * An HttpConnection wrapper that ensures a connection cannot be used
     * once released.
     */
    private static class HttpConnectionAdapter extends HttpConnection {

        // the wrapped connection
        private HttpConnection wrappedConnection;

        /**
         * Creates a new HttpConnectionAdapter.
         * @param connection the connection to be wrapped
         */
        public HttpConnectionAdapter(HttpConnection connection) {
            super(connection.getHost(), connection.getPort(), connection.getProtocol());
            this.wrappedConnection = connection;
        }

        /**
         * Tests if the wrapped connection is still available.
         * @return boolean
         */
        protected boolean hasConnection() {
            return wrappedConnection != null;
        }

        /**
         * @return HttpConnection
         */
        HttpConnection getWrappedConnection() {
            return wrappedConnection;
        }
        
        public void close() {
            if (hasConnection()) {
                wrappedConnection.close();
            } else {
                // do nothing
            }
        }

        public InetAddress getLocalAddress() {
            if (hasConnection()) {
                return wrappedConnection.getLocalAddress();
            } else {
                return null;
            }
        }

        /**
         * @deprecated
         */
        public boolean isStaleCheckingEnabled() {
            if (hasConnection()) {
                return wrappedConnection.isStaleCheckingEnabled();
            } else {
                return false;
            }
        }

        public void setLocalAddress(InetAddress localAddress) {
            if (hasConnection()) {
                wrappedConnection.setLocalAddress(localAddress);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }
    
        /**
         * @deprecated 
         */
        public void setStaleCheckingEnabled(boolean staleCheckEnabled) {
            if (hasConnection()) {
                wrappedConnection.setStaleCheckingEnabled(staleCheckEnabled);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public String getHost() {
            if (hasConnection()) {
                return wrappedConnection.getHost();
            } else {
                return null;
            }
        }

        public HttpConnectionManager getHttpConnectionManager() {
            if (hasConnection()) {
                return wrappedConnection.getHttpConnectionManager();
            } else {
                return null;
            }
        }

        public InputStream getLastResponseInputStream() {
            if (hasConnection()) {
                return wrappedConnection.getLastResponseInputStream();
            } else {
                return null;
            }
        }

        public int getPort() {
            if (hasConnection()) {
                return wrappedConnection.getPort();
            } else {
                return -1;
            }
        }

        public Protocol getProtocol() {
            if (hasConnection()) {
                return wrappedConnection.getProtocol();
            } else {
                return null;
            }
        }

        public String getProxyHost() {
            if (hasConnection()) {
                return wrappedConnection.getProxyHost();
            } else {
                return null;
            }
        }

        public int getProxyPort() {
            if (hasConnection()) {
                return wrappedConnection.getProxyPort();
            } else {
                return -1;
            }
        }

        public OutputStream getRequestOutputStream()
            throws IOException, IllegalStateException {
            if (hasConnection()) {
                return wrappedConnection.getRequestOutputStream();
            } else {
                return null;
            }
        }

        public InputStream getResponseInputStream()
            throws IOException, IllegalStateException {
            if (hasConnection()) {
                return wrappedConnection.getResponseInputStream();
            } else {
                return null;
            }
        }

        public boolean isOpen() {
            if (hasConnection()) {
                return wrappedConnection.isOpen();
            } else {
                return false;
            }
        }

        public boolean closeIfStale() throws IOException {
            if (hasConnection()) {
                return wrappedConnection.closeIfStale();
            } else {
                return false;
            }
        }

        public boolean isProxied() {
            if (hasConnection()) {
                return wrappedConnection.isProxied();
            } else {
                return false;
            }
        }

        public boolean isResponseAvailable() throws IOException {
            if (hasConnection()) {
                return  wrappedConnection.isResponseAvailable();
            } else {
                return false;
            }
        }

        public boolean isResponseAvailable(int timeout) throws IOException {
            if (hasConnection()) {
                return  wrappedConnection.isResponseAvailable(timeout);
            } else {
                return false;
            }
        }

        public boolean isSecure() {
            if (hasConnection()) {
                return wrappedConnection.isSecure();
            } else {
                return false;
            }
        }

        public boolean isTransparent() {
            if (hasConnection()) {
                return wrappedConnection.isTransparent();
            } else {
                return false;
            }
        }

        public void open() throws IOException {
            if (hasConnection()) {
                wrappedConnection.open();
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        /**
         * @deprecated
         */
        public void print(String data)
            throws IOException, IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.print(data);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void printLine()
            throws IOException, IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.printLine();
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        /**
         * @deprecated
         */
        public void printLine(String data)
            throws IOException, IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.printLine(data);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        /**
         * @deprecated
         */
        public String readLine() throws IOException, IllegalStateException {
            if (hasConnection()) {
                return wrappedConnection.readLine();
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public String readLine(String charset) throws IOException, IllegalStateException {
            if (hasConnection()) {
                return wrappedConnection.readLine(charset);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void releaseConnection() {
            if (!isLocked() && hasConnection()) {
                HttpConnection wrappedConnection = this.wrappedConnection;
                this.wrappedConnection = null;
                wrappedConnection.releaseConnection();
            } else {
                // do nothing
            }
        }

        /**
         * @deprecated
         */
        public void setConnectionTimeout(int timeout) {
            if (hasConnection()) {
                wrappedConnection.setConnectionTimeout(timeout);
            } else {
                // do nothing
            }
        }

        public void setHost(String host) throws IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.setHost(host);
            } else {
                // do nothing
            }
        }

        public void setHttpConnectionManager(HttpConnectionManager httpConnectionManager) {
            if (hasConnection()) {
                wrappedConnection.setHttpConnectionManager(httpConnectionManager);
            } else {
                // do nothing
            }
        }

        public void setLastResponseInputStream(InputStream inStream) {
            if (hasConnection()) {
                wrappedConnection.setLastResponseInputStream(inStream);
            } else {
                // do nothing
            }
        }

        public void setPort(int port) throws IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.setPort(port);
            } else {
                // do nothing
            }
        }

        public void setProtocol(Protocol protocol) {
            if (hasConnection()) {
                wrappedConnection.setProtocol(protocol);
            } else {
                // do nothing
            }
        }

        public void setProxyHost(String host) throws IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.setProxyHost(host);
            } else {
                // do nothing
            }
        }

        public void setProxyPort(int port) throws IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.setProxyPort(port);
            } else {
                // do nothing
            }
        }

        /**
         * @deprecated
         */
        public void setSoTimeout(int timeout)
            throws SocketException, IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.setSoTimeout(timeout);
            } else {
                // do nothing
            }
        }

        /**
         * @deprecated
         */
        public void shutdownOutput() {
            if (hasConnection()) {
                wrappedConnection.shutdownOutput();
            } else {
                // do nothing
            }
        }

        public void tunnelCreated() throws IllegalStateException, IOException {
            if (hasConnection()) {
                wrappedConnection.tunnelCreated();
            } else {
                // do nothing
            }
        }

        public void write(byte[] data, int offset, int length)
            throws IOException, IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.write(data, offset, length);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void write(byte[] data)
            throws IOException, IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.write(data);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void writeLine()
            throws IOException, IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.writeLine();
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void writeLine(byte[] data)
            throws IOException, IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.writeLine(data);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void flushRequestOutputStream() throws IOException {
            if (hasConnection()) {
                wrappedConnection.flushRequestOutputStream();
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        /**
         * @deprecated
         */
        public int getSoTimeout() throws SocketException {
            if (hasConnection()) {
                return wrappedConnection.getSoTimeout();
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        /**
         * @deprecated
         */
        public String getVirtualHost() {
            if (hasConnection()) {
                return wrappedConnection.getVirtualHost();
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        /**
         * @deprecated
         */
        public void setVirtualHost(String host) throws IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.setVirtualHost(host);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public int getSendBufferSize() throws SocketException {
            if (hasConnection()) {
                return wrappedConnection.getSendBufferSize();
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        /**
         * @deprecated
         */
        public void setSendBufferSize(int sendBufferSize) throws SocketException {
            if (hasConnection()) {
                wrappedConnection.setSendBufferSize(sendBufferSize);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public HttpConnectionParams getParams() {
            if (hasConnection()) {
                return wrappedConnection.getParams();
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void setParams(final HttpConnectionParams params) {
            if (hasConnection()) {
                wrappedConnection.setParams(params);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        /* (non-Javadoc)
         * @see org.apache.commons.httpclient.HttpConnection#print(java.lang.String, java.lang.String)
         */
        public void print(String data, String charset) throws IOException, IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.print(data, charset);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        /* (non-Javadoc)
         * @see org.apache.commons.httpclient.HttpConnection#printLine(java.lang.String, java.lang.String)
         */
        public void printLine(String data, String charset)
            throws IOException, IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.printLine(data, charset);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        /* (non-Javadoc)
         * @see org.apache.commons.httpclient.HttpConnection#setSocketTimeout(int)
         */
        public void setSocketTimeout(int timeout) throws SocketException, IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.setSocketTimeout(timeout);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

    }

}

