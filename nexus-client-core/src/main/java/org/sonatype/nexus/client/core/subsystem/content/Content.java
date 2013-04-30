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
package org.sonatype.nexus.client.core.subsystem.content;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @since 2.1
 */
public interface Content
{
    enum Directive
    {
        LOCAL_ONLY, REMOTE_ONLY, GROUP_ONLY, AS_EXPIRED;
    }

    boolean exists( Location location )
        throws IOException;

    boolean existsWith( Location location, Directive directive )
        throws IOException;

    void download( Location location, File target )
        throws IOException;

    void downloadWith( Location location, Directive directive, File target )
        throws IOException;

    /**
     * @since 2.4 
     */
    void downloadWith( Location location, Directive directive, OutputStream target )
                    throws IOException;

    void upload( Location location, File target )
        throws IOException;

    void delete( Location location )
        throws IOException;

    // describe
}
