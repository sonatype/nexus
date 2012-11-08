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
package org.sonatype.nexus.proxy.mirror;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sonatype.nexus.configuration.model.CMirror;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.proxy.repository.AbstractProxyRepository;
import org.sonatype.nexus.proxy.repository.Mirror;

public class DefaultDownloadMirrors
    implements DownloadMirrors
{
    private static final long NO_EXPIRATION = -1;

    private static final long DEFAULT_EXPIRATION = 30 * 60 * 1000L; // 30 minutes

    private final CRepositoryCoreConfiguration configuration;

    private Map<String, BlacklistEntry> blacklist = new LinkedHashMap<String, BlacklistEntry>();

    private long blacklistExpiration = DEFAULT_EXPIRATION;

    public DefaultDownloadMirrors( CRepositoryCoreConfiguration configuration )
    {
        this.configuration = configuration;
    }

    /**
     * Maximum number of mirror url to consider before operation falls back to canonical url.
     * 
     * @see AbstractProxyRepository#doRetrieveRemoteItem
     */
    private int maxMirrors = 1;

    private static class BlacklistEntry
    {
        private String id;

        private long timestamp;

        public BlacklistEntry( String id, long timestamp )
        {
            this.id = id;

            this.timestamp = timestamp;
        }

        public String getId()
        {
            return id;
        }

        public long getTimestamp()
        {
            return timestamp;
        }
    }

    public DownloadMirrorSelector openSelector( String mirrorOfUrl )
    {
        return new DefaultDownloadMirrorSelector( this );
    }

    public void setMirrors( List<Mirror> mirrors )
    {
        if ( mirrors == null || mirrors.isEmpty() )
        {
            getConfiguration( true ).getRemoteStorage().getMirrors().clear();

            blacklist.clear();
        }
        else
        {
            List<CMirror> modelMirrors = new ArrayList<CMirror>( mirrors.size() );

            for ( Mirror mirror : mirrors )
            {
                CMirror model = new CMirror();

                model.setId( mirror.getId() );

                model.setUrl( mirror.getUrl() );

                modelMirrors.add( model );
            }

            getConfiguration( true ).getRemoteStorage().setMirrors( modelMirrors );

            // remove blacklist entries for removed mirrors, but retain others
            Iterator<Entry<String, BlacklistEntry>> i = blacklist.entrySet().iterator();

            while ( i.hasNext() )
            {
                String id = i.next().getKey();

                if ( !existsMirrorWithId( true, id ) )
                {
                    i.remove();
                }
            }
        }
    }

    public List<Mirror> getMirrors()
    {
        List<CMirror> modelMirrors = getConfiguration( false ).getRemoteStorage().getMirrors();

        List<Mirror> mirrors = new ArrayList<Mirror>( modelMirrors.size() );

        for ( CMirror model : modelMirrors )
        {
            Mirror mirror = new Mirror( model.getId(), model.getUrl() );

            mirrors.add( mirror );
        }

        return Collections.unmodifiableList( mirrors );
    }

    public boolean isBlacklisted( Mirror mirror )
    {
        BlacklistEntry entry = blacklist.get( mirror.getId() );

        if ( entry == null )
        {
            return false;
        }

        if ( blacklistExpiration == NO_EXPIRATION )
        {
            return true;
        }

        if ( entry.getTimestamp() + blacklistExpiration > System.currentTimeMillis() )
        {
            return true;
        }

        blacklist.remove( mirror.getId() );

        return false;
    }

    /**
     * Adds specified mirror URLs to the black list.
     */
    public void blacklist( Set<Mirror> mirrors )
    {
        for ( Mirror mirror : mirrors )
        {
            blacklist.remove( mirror.getId() );

            if ( existsMirrorWithId( false, mirror.getId() ) )
            {
                blacklist.put( mirror.getId(), new BlacklistEntry( mirror.getId(), System.currentTimeMillis() ) );
            }
        }
    }

    public void setBlacklistExpiration( long blacklistExpiration )
    {
        this.blacklistExpiration = blacklistExpiration;
    }

    public long getBlacklistExpiration()
    {
        return blacklistExpiration;
    }

    public int getMaxMirrors()
    {
        return maxMirrors;
    }

    public void setMaxMirrors( int maxMirrors )
    {
        this.maxMirrors = maxMirrors;
    }

    protected CRepository getConfiguration( boolean forWrite )
    {
        return (CRepository) configuration.getConfiguration( forWrite );
    }

    protected boolean existsMirrorWithId( boolean forWrite, String id )
    {
        List<CMirror> modelMirrors = getConfiguration( forWrite ).getRemoteStorage().getMirrors();

        for ( CMirror modelMirror : modelMirrors )
        {
            if ( modelMirror.getId().equals( id ) )
            {
                return true;
            }
        }

        return false;
    }
}
