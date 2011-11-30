/**
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions
 *
 * This program is free software: you can redistribute it and/or modify it only under the terms of the GNU Affero General
 * Public License Version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License Version 3
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License Version 3 along with this program.  If not, see
 * http://www.gnu.org/licenses.
 *
 * Sonatype Nexus (TM) Open Source Version is available from Sonatype, Inc. Sonatype and Sonatype Nexus are trademarks of
 * Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation. M2Eclipse is a trademark of the Eclipse Foundation.
 * All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.integrationtests.nexus2692;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.DefaultStorageCollectionItem;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.DefaultStorageLinkItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.tasks.descriptors.EvictUnusedItemsTaskDescriptor;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;

import com.thoughtworks.xstream.XStream;

public class AbstractEvictTaskIt
    extends AbstractNexusIntegrationTest
{

    private static final long A_DAY = 24L * 60L * 60L * 1000L;

    private Map<String, Double> pathMap = new HashMap<String, Double>();

    private List<String> neverDeleteFiles = new ArrayList<String>();

    private File storageWorkDir;

    @BeforeMethod
    public void setupStorageAndAttributes()
        throws Exception
    {
        File workDir = new File( AbstractNexusIntegrationTest.nexusWorkDir );

        this.storageWorkDir = new File( workDir, "storage" );

        FileUtils.copyDirectoryStructure( this.getTestResourceAsFile( "storage/" ), storageWorkDir );
        copyAttributes();

        // now setup all the attributes
        File attributesInfo = this.getTestResourceAsFile( "attributes.info" );
        BufferedReader reader = null;
        FileInputStream fis = null;
        FileOutputStream fos = null;

        XStream xstream = new XStream();
        xstream.alias( "file", DefaultStorageFileItem.class );
        xstream.alias( "collection", DefaultStorageCollectionItem.class );
        xstream.alias( "link", DefaultStorageLinkItem.class );

        long timestamp = System.currentTimeMillis();

        try
        {
            reader = new BufferedReader( new FileReader( attributesInfo ) );

            String line = reader.readLine();
            while ( line != null )
            {
                String[] parts = line.split( " " );
                String filePart = parts[0];
                long offset = (long) ( Double.parseDouble( parts[1] ) * A_DAY );

                // get the file
                File itemFile = new File( storageWorkDir, filePart );
                if ( itemFile.isFile() )
                {
                    this.pathMap.put( filePart, Double.parseDouble( parts[1] ) );

                    /*
                     * groups are not checked, so the hashes are left behind, see: NEXUS-3026
                     */
                    if ( filePart.startsWith( "releases/" ) || filePart.startsWith( "releases-m1/" )
                        || filePart.startsWith( "public/" ) || filePart.startsWith( "snapshots/" )
                        || filePart.startsWith( "thirdparty/" ) || filePart.contains( ".meta" )
                        || filePart.contains( ".index" ) )
                    {
                        neverDeleteFiles.add( filePart );
                    }

                    // modify the file corresponding attribute
                    File attributeFile = getAttributeFile( filePart );
                    fis = new FileInputStream( attributeFile );
                    StorageItem storageItem = (StorageItem) xstream.fromXML( fis );
                    IOUtil.close( fis );

                    // get old value, update it and set it, but all this is done using reflection
                    // Direct method access will work, since we mangle an item that will be persisted using "old" format
                    // Once item "upgraded", there is no backward way to downgrade it, to persist it using old format
                    final Field field = AbstractStorageItem.class.getDeclaredField( "lastRequested" );
                    field.setAccessible( true );

                    // get old value
                    final long itemLastRequested = (Long) field.get( storageItem );

                    // calculate the variation
                    long variation = ( 1258582311671l - itemLastRequested ) + timestamp;

                    // and set the value with reflection, since we will again persist it using XStream in "old" format
                    field.set( storageItem, variation + offset );

                    // write it out in "old" format
                    fos = new FileOutputStream( attributeFile );
                    xstream.toXML( storageItem, fos );
                    IOUtil.close( fos );
                }

                line = reader.readLine();
            }
        }
        finally
        {
            IOUtil.close( fos );
            IOUtil.close( fis );
            IOUtil.close( reader );
        }
    }

    protected void copyAttributes()
        throws IOException
    {
        File srcDir = getTestResourceAsFile( "attributes/" );

        // old location
        FileUtils.copyDirectoryStructure( srcDir, new File( new File( nexusWorkDir ), "proxy/attributes" ) );
        
        // new location will need path mangling, see getAttributeFile()
    }

    protected File getAttributeFile( String filePart )
    {
        return new File(new File( new File( nexusWorkDir ), "proxy/attributes" ), filePart);
        /* This is NEW layout!
        String[] parts = filePart.split( "/" );

        // repoId
        StringBuilder sb = new StringBuilder( parts[0] );

        // "sneak in" the ".nexus/attributes"
        sb.append( "/.nexus/attributes" );

        // the rest
        for ( int i = 1; i < parts.length; i++ )
        {
            sb.append( "/" ).append( parts[i] );
        }

        return new File( storageWorkDir, sb.toString() );
        */
    }

    protected void runTask( int days, String repoId )
        throws Exception
    {
        TaskScheduleUtil.waitForAllTasksToStop(); // be sure nothing else is locking tasks

        ScheduledServicePropertyResource prop = new ScheduledServicePropertyResource();
        prop.setKey( "repositoryId" );
        prop.setValue( repoId );

        ScheduledServicePropertyResource age = new ScheduledServicePropertyResource();
        age.setKey( "evictOlderCacheItemsThen" );
        age.setValue( String.valueOf( days ) );

        TaskScheduleUtil.runTask( EvictUnusedItemsTaskDescriptor.ID, EvictUnusedItemsTaskDescriptor.ID, prop, age );

        getEventInspectorsUtil().waitForCalmPeriod();
    }

    protected SortedSet<String> buildListOfExpectedFilesForAllRepos( int days )
    {
        SortedSet<String> expectedFiles = new TreeSet<String>();

        expectedFiles.addAll( this.getNeverDeleteFiles() );

        for ( Entry<String, Double> entry : this.getPathMap().entrySet() )
        {
            if ( entry.getValue() > ( days * -1 ) )
            {
                expectedFiles.add( entry.getKey() );
            }
        }

        List<String> expectedShadows = new ArrayList<String>();

        // loop once more to look for the shadows (NOTE: the shadow id must be in the format of targetId-*
        for ( String expectedFile : expectedFiles )
        {
            String prefix = expectedFile.substring( 0, expectedFile.indexOf( "/" ) ) + "-";
            String fileName = new File( expectedFile ).getName();

            for ( String originalFile : this.getPathMap().keySet() )
            {
                if ( originalFile.startsWith( prefix ) && originalFile.endsWith( fileName ) )
                {
                    expectedShadows.add( originalFile );
                    break;
                }
            }
        }

        expectedFiles.addAll( expectedShadows );

        return expectedFiles;
    }

    protected SortedSet<String> buildListOfExpectedFiles( int days, List<String> otherNotChangedRepoids )
    {
        SortedSet<String> expectedFiles = this.buildListOfExpectedFilesForAllRepos( days );

        for ( String path : this.getPathMap().keySet() )
        {
            String repoId = path.substring( 0, path.indexOf( "/" ) );
            if ( otherNotChangedRepoids.contains( repoId ) )
            {
                log.debug( "found it:" + path );
                expectedFiles.add( path );
            }
        }
        return expectedFiles;
    }

    protected void checkForEmptyDirectories()
        throws IOException
    {
        // make sure we don't have any empty directories
        Set<String> emptyDirectories = new HashSet<String>();

        SortedSet<String> resultDirectories = this.getDirectoryPaths( this.getStorageWorkDir() );
        for ( String itemPath : resultDirectories )
        {
            if ( itemPath.split( Pattern.quote( File.separator ) ).length != 1 )
            {
                File directory = new File( this.getStorageWorkDir(), itemPath );
                if ( directory.list().length == 0 )
                {
                    emptyDirectories.add( itemPath );
                }
            }
        }

        Assert.assertTrue( emptyDirectories.size() == 0, "Found empty directories: " + emptyDirectories );
    }

    protected String prettyList( Set<String> list )
    {
        StringBuffer buffer = new StringBuffer();
        for ( String string : list )
        {
            buffer.append( string ).append( "\n" );
        }

        return buffer.toString();
    }

    protected SortedSet<String> getItemFilePaths()
        throws IOException
    {
        SortedSet<String> result = new TreeSet<String>();

        SortedSet<String> paths = getFilePaths( getStorageWorkDir() );

        for ( String path : paths )
        {
            if ( !path.contains( "/.nexus" ) )
            {
                result.add( path );
            }
        }

        return result;
    }

    @SuppressWarnings( "unchecked" )
    protected SortedSet<String> getFilePaths( File basedir )
        throws IOException
    {
        SortedSet<String> result = new TreeSet<String>();
        List<String> paths = FileUtils.getFileNames( basedir, null, null, false, true );
        for ( String path : paths )
        {
            result.add( path.replaceAll( Pattern.quote( "\\" ), "/" ) );
        }
        return result;
    }

    @SuppressWarnings( "unchecked" )
    protected SortedSet<String> getDirectoryPaths( File basedir )
        throws IOException
    {
        SortedSet<String> result = new TreeSet<String>();
        List<String> paths = FileUtils.getDirectoryNames( basedir, null, null, false, true );
        for ( String path : paths )
        {
            result.add( path.replaceAll( Pattern.quote( "\\" ), "/" ) );
        }
        return result;
    }

    public File getStorageWorkDir()
    {
        return storageWorkDir;
    }

    public Map<String, Double> getPathMap()
    {
        return pathMap;
    }

    public Collection<String> getNeverDeleteFiles()
    {
        DirectoryScanner scan = new DirectoryScanner();
        scan.setBasedir( new File( nexusWorkDir, "storage" ) );
        scan.setIncludes( new String[] { "**/.index/", "**/.meta/", "*/archetype-catalog.xml" } );
        scan.setExcludes( new String[] { "**/.nexus/", "**/.svn", "**/.svn/**" } );

        scan.scan();

        Collection<String> files = new LinkedHashSet<String>();
        files.addAll( neverDeleteFiles );

        String[] includes = scan.getIncludedFiles();
        for ( String file : includes )
        {
            files.add( file.replace( '\\', '/' ) );
        }

        return files;
    }

}
