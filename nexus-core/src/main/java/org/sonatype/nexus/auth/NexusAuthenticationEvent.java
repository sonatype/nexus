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
package org.sonatype.nexus.auth;

/**
 * Event fired when authentication validation is performed (someone tries to log in).
 * 
 * @author cstamas
 */
public class NexusAuthenticationEvent
    extends AbstractSecurityEvent
{
    private final boolean successful;

    public NexusAuthenticationEvent( final Object sender, final ClientInfo info, final boolean successful )
    {
        super( sender, info );
        this.successful = successful;
    }

    public boolean isSuccessful()
    {
        return successful;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "userId='" + getClientInfo().getUserid() + "'," +
            "remoteIp='" + getClientInfo().getRemoteIP() + "'," +
            "successful=" + successful +
            '}';
    }
}
