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
package org.sonatype.nexus.email;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * The Nexus specific simple default mail type.
 *
 * @since 2.5
 */
@Named( NexusDefaultMailType.ID )
@Singleton
public class NexusDefaultMailType
    extends NexusMailType
{

    public static final String ID = "nexus.default";

    public NexusDefaultMailType()
    {
        setTypeId( ID );
        setBodyIsHtml( false );
    }

}
