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
package org.sonatype.nexus.proxy.repository;

public class DefaultRemoteConnectionSettings
    implements RemoteConnectionSettings
{
    private int connectionTimeout = 1000;

    private int retrievalRetryCount = 3;

    private String queryString;

    private String userAgentCustomizationString;

    public int getConnectionTimeout()
    {
        return connectionTimeout;
    }

    public void setConnectionTimeout( int connectionTimeout )
    {
        this.connectionTimeout = connectionTimeout;
    }

    public int getRetrievalRetryCount()
    {
        return retrievalRetryCount;
    }

    public void setRetrievalRetryCount( int retrievalRetryCount )
    {
        this.retrievalRetryCount = retrievalRetryCount;
    }

    public String getQueryString()
    {
        return queryString;
    }

    public void setQueryString( String queryString )
    {
        this.queryString = queryString;
    }

    public String getUserAgentCustomizationString()
    {
        return userAgentCustomizationString;
    }

    public void setUserAgentCustomizationString( String userAgentCustomizationString )
    {
        this.userAgentCustomizationString = userAgentCustomizationString;
    }
}
