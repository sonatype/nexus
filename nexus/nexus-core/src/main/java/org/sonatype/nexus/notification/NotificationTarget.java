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
package org.sonatype.nexus.notification;

import java.util.HashSet;
import java.util.Set;

public class NotificationTarget
{
    private String targetId;

    private final Set<String> targetRoles;

    private final Set<String> targetUsers;

    private final Set<String> externalTargets;

    public NotificationTarget()
    {
        this.targetRoles = new HashSet<String>();

        this.targetUsers = new HashSet<String>();

        this.externalTargets = new HashSet<String>();
    }

    public String getTargetId()
    {
        return targetId;
    }

    public void setTargetId( String targetId )
    {
        this.targetId = targetId;
    }

    public Set<String> getTargetRoles()
    {
        return targetRoles;
    }

    public Set<String> getTargetUsers()
    {
        return targetUsers;
    }

    public Set<String> getExternalTargets()
    {
        return externalTargets;
    }
}
