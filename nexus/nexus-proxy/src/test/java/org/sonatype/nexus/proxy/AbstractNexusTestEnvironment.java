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
package org.sonatype.nexus.proxy;

import org.sonatype.nexus.mime.MimeUtil;
import org.sonatype.nexus.proxy.cache.CacheManager;
import org.sonatype.nexus.proxy.item.RepositoryItemUidFactory;
import org.sonatype.nexus.proxy.item.uid.RepositoryItemUidAttributeManager;
import org.sonatype.security.SecuritySystem;

public abstract class AbstractNexusTestEnvironment
    extends AbstractNexusTestCase
{
    /** The cache manager. */
    private CacheManager cacheManager;

    private RepositoryItemUidFactory repositoryItemUidFactory;

    private MimeUtil mimeUtil;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        cacheManager = lookup( CacheManager.class );

        repositoryItemUidFactory = lookup( RepositoryItemUidFactory.class );

        // rebuild cache
        lookup( RepositoryItemUidAttributeManager.class ).reset();

        mimeUtil = lookup( MimeUtil.class );

        this.lookup( SecuritySystem.class ).setSecurityEnabled( false );
    }

    /**
     * Gets the cache manager.
     * 
     * @return the cache manager
     */
    protected CacheManager getCacheManager()
    {
        return cacheManager;
    }

    protected RepositoryItemUidFactory getRepositoryItemUidFactory()
    {
        return repositoryItemUidFactory;
    }

    protected MimeUtil getMimeUtil()
    {
        return mimeUtil;
    }

}
