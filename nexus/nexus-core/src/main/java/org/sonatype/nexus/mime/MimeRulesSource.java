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
package org.sonatype.nexus.mime;

/**
 * Interface to provide "hints" what a given source thinks about MIME type of some items.
 * 
 * @author cstamas
 * @since 2.0
 */
public interface MimeRulesSource
{
    MimeRulesSource NOOP = new MimeRulesSource()
    {
        @Override
        public String getRuleForPath( final String path )
        {
            return null;
        }
    };

    /**
     * Returns the forced MIME type that corresponds (should correspond) to given path in the context of given rule
     * source. Returns {@code null} if no rules found.
     * 
     * @param path
     * @return
     */
    String getRuleForPath( String path );
}
