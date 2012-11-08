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
package org.sonatype.nexus.email;

/**
 * Nexus specific emailer.
 *
 * @author Alin Dreghiciu
 */
public interface NexusPostOffice
{

    /**
     * Sends an alert email about a failing task.
     *
     * @param email    email address to end email to
     * @param taskId   id of the failing task
     * @param taskName name of teh failing task
     * @param cause    falure cause
     */
    void sendNexusTaskFailure( String email, String taskId, String taskName, Throwable cause );

}