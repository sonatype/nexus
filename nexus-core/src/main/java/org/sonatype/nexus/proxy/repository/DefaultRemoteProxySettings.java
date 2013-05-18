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
package org.sonatype.nexus.proxy.repository;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * Default {@link RemoteProxySettings} implementation.
 */
public class DefaultRemoteProxySettings
    implements RemoteProxySettings
{

    private RemoteHttpProxySettings httpProxySettings;

    private RemoteHttpProxySettings httpsProxySettings;

    private Set<String> nonProxyHosts = Sets.newHashSet();

    public RemoteHttpProxySettings getHttpProxySettings()
    {
        return httpProxySettings;
    }

    public void setHttpProxySettings( final RemoteHttpProxySettings httpProxySettings )
    {
        this.httpProxySettings = httpProxySettings;
    }

    public RemoteHttpProxySettings getHttpsProxySettings()
    {
        return httpsProxySettings;
    }

    public void setHttpsProxySettings( final RemoteHttpProxySettings httpsProxySettings )
    {
        this.httpsProxySettings = httpsProxySettings;
    }

    public Set<String> getNonProxyHosts()
    {
        return nonProxyHosts;
    }

    public void setNonProxyHosts( final Set<String> nonProxyHosts )
    {
        this.nonProxyHosts.clear();
        if ( nonProxyHosts != null && !nonProxyHosts.isEmpty() )
        {
            this.nonProxyHosts.addAll( nonProxyHosts );
        }
    }

}
