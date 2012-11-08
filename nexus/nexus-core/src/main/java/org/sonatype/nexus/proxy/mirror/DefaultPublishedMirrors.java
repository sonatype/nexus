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
import java.util.List;

import org.sonatype.nexus.configuration.model.CMirror;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.proxy.repository.Mirror;

public class DefaultPublishedMirrors
    implements PublishedMirrors
{
    private final CRepositoryCoreConfiguration configuration;

    public DefaultPublishedMirrors( CRepositoryCoreConfiguration configuration )
    {
        this.configuration = configuration;
    }

    public void setMirrors( List<Mirror> mirrors )
    {
        if ( mirrors == null || mirrors.isEmpty() )
        {
            getConfiguration( true ).getMirrors().clear();
        }
        else
        {
            ArrayList<CMirror> modelMirrors = new ArrayList<CMirror>( mirrors.size() );

            for ( Mirror mirror : mirrors )
            {
                CMirror model = new CMirror();

                model.setId( mirror.getId() );

                model.setUrl( mirror.getUrl() );

                modelMirrors.add( model );
            }

            getConfiguration( true ).setMirrors( modelMirrors );
        }
    }

    public List<Mirror> getMirrors()
    {
        List<CMirror> modelMirrors = getConfiguration( false ).getMirrors();

        ArrayList<Mirror> mirrors = new ArrayList<Mirror>( modelMirrors.size() );

        for ( CMirror model : modelMirrors )
        {
            Mirror mirror = new Mirror( model.getId(), model.getUrl() );

            mirrors.add( mirror );
        }

        return Collections.unmodifiableList( mirrors );
    }

    // ==

    protected CRepository getConfiguration( boolean forWrite )
    {
        return (CRepository) configuration.getConfiguration( forWrite );
    }
}
