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
package org.sonatype.nexus.restlight.stage;

/**
 * Simple object to represent a Staging or Build Promotion Profile.
 * 
 * @author Brian Demers
 */
@Deprecated
public class StageProfile
{
    /**
     * Id of the profile.
     */
    private String profileId;

    /**
     * Display name of the profile.
     */
    private String name;

    /**
     * The profile mode
     */
    private String mode;

    public StageProfile( String profileId, String name )
    {
        this( profileId, name, null );
    }

    public StageProfile( String profileId, String name, String mode )
    {
        super();
        this.profileId = profileId;
        this.name = name;
        this.mode = mode;
    }

    public String getProfileId()
    {
        return profileId;
    }

    public String getName()
    {
        return name;
    }

    public String getMode()
    {
        return mode;
    }

}
