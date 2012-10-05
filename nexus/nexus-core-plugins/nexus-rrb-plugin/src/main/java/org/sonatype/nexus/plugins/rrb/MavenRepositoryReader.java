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
package org.sonatype.nexus.plugins.rrb;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.plugins.rrb.parsers.ArtifactoryRemoteRepositoryParser;
import org.sonatype.nexus.plugins.rrb.parsers.HtmlRemoteRepositoryParser;
import org.sonatype.nexus.plugins.rrb.parsers.RemoteRepositoryParser;
import org.sonatype.nexus.plugins.rrb.parsers.S3RemoteRepositoryParser;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.repository.ProxyRepository;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class for retrieving directory data from remote repository. This class is not thread-safe!
 */
public class MavenRepositoryReader
{

    private final Logger logger = LoggerFactory.getLogger( MavenRepositoryReader.class );

    private final ProxyRepository proxyRepository;

    private String remotePath;

    private String localUrl;

    private String id;

    public MavenRepositoryReader( final ProxyRepository proxyRepository )
    {
        this.proxyRepository = checkNotNull(proxyRepository);
    }

    /**
     * @param remotePath remote path added to the URL
     * @param localUrl url to the local resource service
     * @return a list containing the remote data
     */
    public List<RepositoryDirectory> extract( String remotePath, String localUrl, String id )
    {
        logger.debug( "remotePath: {}", remotePath );
        this.remotePath = remotePath;
        this.localUrl = localUrl;
        this.id = id;

        StringBuilder html = getContent();
        if ( logger.isDebugEnabled() )
        {
            logger.trace( html.toString() );
        }
        return parseResult( html );
    }

    private ArrayList<RepositoryDirectory> parseResult( StringBuilder indata )
    {
        RemoteRepositoryParser parser = null;
        String baseUrl = "";
        if ( proxyRepository != null )
        {
            baseUrl = proxyRepository.getRemoteUrl();
        }

        if ( indata.indexOf( "<html " ) != -1 )
        {
            // if title="Artifactory" then it is an Artifactory repo...
            if ( indata.indexOf( "title=\"Artifactory\"" ) != -1 )
            {
                logger.debug( "is Artifactory repository" );
                parser = new ArtifactoryRemoteRepositoryParser( remotePath, localUrl, id, baseUrl );
            }
            else
            {
                logger.debug( "is html repository" );
                parser = new HtmlRemoteRepositoryParser( remotePath, localUrl, id, baseUrl );
            }
        }
        else if ( indata.indexOf( "xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"" ) != -1
            || ( indata.indexOf( "<?xml" ) != -1 && responseContainsError( indata ) ) )
        {
            logger.debug( "is S3 repository" );
            if ( responseContainsError( indata ) && !responseContainsAccessDenied( indata ) )
            {
                logger.debug( "response from S3 repository contains error, need to find rootUrl" );
                remotePath = findcreateNewUrl( indata );
                indata = getContent();
            }
            else if ( responseContainsError( indata ) && responseContainsAccessDenied( indata ) )
            {
                logger.debug( "response from S3 repository contains access denied response" );
                indata = new StringBuilder();
            }

            parser =
                new S3RemoteRepositoryParser( remotePath, localUrl, id, baseUrl.replace( findRootUrl( indata ), "" ) );
        }
        else
        {
            logger.debug( "Found no matching parser, using default html parser" );

            parser = new HtmlRemoteRepositoryParser( remotePath, localUrl, id, baseUrl );
        }
        return parser.extractLinks(indata);
    }

    private String findcreateNewUrl( StringBuilder indata )
    {
        logger.debug( "indata={}", indata.toString() );
        String key = extracktKey( indata );
        String newUrl = "";
        if ( !key.equals( "" ) )
        {
            newUrl = findRootUrl( indata );
            newUrl += "?prefix=" + key;
        }
        if ( !newUrl.endsWith( "/" ) )
        {
            newUrl += "/";
        }
        logger.debug( "newUrl={}", newUrl );
        return newUrl;
    }

    private String findRootUrl( StringBuilder indata )
    {
        int end = remotePath.indexOf( extracktKey( indata ) );
        if ( end > 0 )
        {
            String newUrl = remotePath.substring( 0, end );
            if ( newUrl.indexOf( '?' ) != -1 )
            {
                newUrl = newUrl.substring( 0, newUrl.indexOf( '?' ) );
            }
            return newUrl;
        }
        return remotePath;
    }

    private String extracktKey( StringBuilder indata )
    {
        String key = "";
        int start = indata.indexOf( "<Key>" );
        int end = indata.indexOf( "</Key>" );
        if ( start > 0 && end > start )
        {
            key = indata.substring( start + 5, end );
        }
        return key;
    }

    /**
     * Used to detect error in S3 response.
     *
     * @param indata
     * @return
     */
    private boolean responseContainsError( StringBuilder indata )
    {
        if ( indata.indexOf( "<Error>" ) != -1 || indata.indexOf( "<error>" ) != -1 )
        {
            return true;
        }
        return false;
    }

    /**
     * Used to detect access denied in S3 response.
     *
     * @param indata
     * @return
     */
    private boolean responseContainsAccessDenied( StringBuilder indata )
    {
        if ( indata.indexOf( "<Code>AccessDenied</Code>" ) != -1 || indata.indexOf( "<code>AccessDenied</code>" ) != -1 )
        {
            return true;
        }
        return false;
    }

    private StringBuilder getContent()
    {
        StringBuilder buff = new StringBuilder();

        String sep = remotePath.contains("?prefix") ? "&": "?";
        String path = remotePath + sep + "delimiter=/";

        try {
            ResourceStoreRequest req = new ResourceStoreRequest(path);
            req.setRequestRemoteOnly(true);
            StorageFileItem item = (StorageFileItem) proxyRepository.getRemoteStorage()
                .retrieveItem(proxyRepository, req, proxyRepository.getRemoteUrl());

            BufferedReader reader = new BufferedReader(new InputStreamReader(item.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                buff.append(line).append("\n");
            }
        }
        catch (Exception e) {
            logger.warn("Failed to fetch remote directory index", e);
        }

        // HACK: Deal with S3 edge-case
        // here is the deal, For reasons I do not understand, S3 comes back with an empty response (and a 200),
        // stripping off the last '/' returns the error we are looking for (so we can do a query)
        //Header serverHeader = response.getFirstHeader(HttpHeaders.SERVER);
        //if (buff.length() == 0 && serverHeader != null &&
        //    serverHeader.getValue().equalsIgnoreCase("AmazonS3") &&
        //    remoteUrl.endsWith("/")) {
        //    remoteUrl = remoteUrl.substring(0, remoteUrl.length() - 1);
        //    return getContent();
        //}

        return buff;
    }
}
