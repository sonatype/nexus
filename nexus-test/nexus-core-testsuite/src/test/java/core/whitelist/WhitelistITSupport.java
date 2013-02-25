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
package core.whitelist;

import org.sonatype.nexus.client.core.subsystem.whitelist.Status;
import org.sonatype.nexus.client.core.subsystem.whitelist.Whitelist;
import org.sonatype.nexus.client.core.subsystem.whitelist.Status.Outcome;
import core.NexusCoreITSupport;

/**
 * Support class for Whitelist Core feature (NEXUS-5472), aka "proxy404".
 * 
 * @author cstamas
 * @since 2.4
 */
public abstract class WhitelistITSupport
    extends NexusCoreITSupport
{
    protected WhitelistITSupport( final String nexusBundleCoordinates )
    {
        super( nexusBundleCoordinates );
    }

    /**
     * Returns {@link Whitelist} client subsystem.
     * 
     * @return client for whitelist.
     */
    public Whitelist whitelist()
    {
        return client().getSubsystem( Whitelist.class );
    }

    /**
     * Waits for a remote discovery outcome. The passed in repository ID must correspond to a Maven2 proxy repository,
     * otherwise {@link IllegalArgumentException} is thrown.
     * 
     * @param proxyRepositoryId
     * @throws IllegalArgumentException if repository ID is not a maven2 proxy.
     * @throws InterruptedException
     */
    public void waitForWLDiscoveryOutcome( final String proxyRepositoryId )
        throws IllegalArgumentException, InterruptedException
    {
        // status
        Status status = whitelist().getWhitelistStatus( proxyRepositoryId );
        if ( status.getDiscoveryStatus() == null )
        {
            throw new IllegalArgumentException( "Repository with ID=" + proxyRepositoryId
                + " is not a Maven2 proxy repository!" );
        }
        while ( status.getDiscoveryStatus().isDiscoveryEnabled()
            && status.getDiscoveryStatus().getDiscoveryLastStatus() == Outcome.UNDECIDED )
        {
            Thread.sleep( 1000 );
            status = whitelist().getWhitelistStatus( proxyRepositoryId );
        }
    }
}
