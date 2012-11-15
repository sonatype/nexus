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
package org.sonatype.nexus.plugins.restlet1x;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.io.ByteStreams;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.plugins.rest.AbstractNexusResourceBundle;
import org.sonatype.nexus.plugins.rest.DefaultStaticResource;
import org.sonatype.nexus.plugins.rest.ExternalStaticResource;
import org.sonatype.nexus.plugins.rest.StaticResource;

@Named
public class NexusWebappResourceBundle
    extends AbstractNexusResourceBundle
{

    private final BuildNumberService buildNumberService;

    private static final Logger logger = LoggerFactory.getLogger( NexusWebappResourceBundle.class );

    @Inject
    public NexusWebappResourceBundle( final BuildNumberService buildNumberService )
    {
        this.buildNumberService = buildNumberService;
    }

    @Override
    public List<StaticResource> getContributedResouces()
    {
        String prefix = buildNumberService.getBuildNumber();

        List<StaticResource> result = new ArrayList<StaticResource>();

        try
        {
            result.add( new CachingClasspathResource( this.getClass().getResource( "/js/sonatype-all.js" ),
                                                   "js/" + prefix + "/sonatype-all.js", "text/javascript" ).cache() );
            result.add( new CachingClasspathResource( this.getClass().getResource( "/style/sonatype-all.css" ),
                                                      "style/" + prefix + "/sonatype-all.css", "text/css" ).cache() );

        }
        catch ( IOException e )
        {
            logger.error( "Could not read from classpath", e );
        }
        return result;
    }

    public static class CachingClasspathResource
        extends DefaultStaticResource
    {

        private byte[] content;

        public CachingClasspathResource( URL url, String path, String contentType )
        {
            super( url, path, contentType );
        }

        @Override
        public InputStream getInputStream()
            throws IOException
        {
            if ( content == null )
            {
                cache();
            }
            return new ByteArrayInputStream( content );
        }

        private CachingClasspathResource cache()
            throws IOException
        {
            content = ByteStreams.toByteArray( super.getInputStream() );
            return this;
        }
    }

}
