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
package org.sonatype.nexus.timeline;

import java.io.File;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;

public class LegacyNexusTimelineTest
    extends AbstractTimelineTest
{
    @Test
    public void testMoveLegacyTimeline()
        throws Exception
    {
        File legacyDataDir = new File( getBasedir(), "target/test-classes/timeline/legacy" );

        File legacyTimelineDir = new File( getWorkHomeDir(), "timeline" );

        FileUtils.copyDirectoryStructure( legacyDataDir, legacyTimelineDir );

        NexusTimeline nexusTimeline = this.lookup( NexusTimeline.class );

        final EntryListCallback cb = new EntryListCallback();
        nexusTimeline.retrieve( 0, 10, null, null, null, cb );
        assertTrue( !cb.getEntries().isEmpty() );
    }

    @Test
    public void testDoNotMoveLegacyTimeline()
        throws Exception
    {
        File legacyDataDir = new File( getBasedir(), "target/test-classes/timeline/legacy" );

        File newDataDir = new File( getBasedir(), "target/test-classes/timeline/new" );

        File legacyTimelineDir = new File( getWorkHomeDir(), "timeline" );

        File newTimelineDir = new File( getWorkHomeDir(), "timeline/index" );

        FileUtils.copyDirectoryStructure( legacyDataDir, legacyTimelineDir );

        FileUtils.copyDirectoryStructure( newDataDir, newTimelineDir );

        NexusTimeline nexusTimeline = this.lookup( NexusTimeline.class );

        final EntryListCallback cb = new EntryListCallback();
        nexusTimeline.retrieve( 0, 10, null, null, null, cb );
        assertEquals( 4, cb.getEntries().size() );
    }
}
