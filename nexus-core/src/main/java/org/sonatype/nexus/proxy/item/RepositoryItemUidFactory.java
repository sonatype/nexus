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
package org.sonatype.nexus.proxy.item;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.repository.Repository;

public interface RepositoryItemUidFactory
{
    /**
     * Creates an UID based on a Repository reference and a path.
     * 
     * @param repository
     * @param path
     * @return
     */
    RepositoryItemUid createUid( Repository repository, String path );

    /**
     * Parses an "uid string representation" and creates an UID for it. Uid String representation is of form '<repoId> +
     * ':' + <path>'.
     * 
     * @param uidStr
     * @return
     * @throws IllegalArgumentException
     * @throws NoSuchRepositoryException
     */
    public RepositoryItemUid createUid( String uidStr )
        throws IllegalArgumentException, NoSuchRepositoryException;

    /**
     * Creates a shared UIDLock based on a uid reference.
     * 
     * @param uid
     * @return
     * @deprecated In case of need use {@link Repository#createUidLock(String)} instead if needed, otherwise
     *             {@link RepositoryItemUid#getLock()} is preferred to obtain the lock.
     */
    RepositoryItemUidLock createUidLock( RepositoryItemUid uid );

    /**
     * Creates a shared attribute UIDLock based on a uid reference.
     * 
     * @param uid
     * @return
     * @deprecated In case of need use {@link Repository#createUidAttributeLock(String)} instead if needed, otherwise
     *             {@link RepositoryItemUid#getAttributeLock()} is preferred to obtain the lock.
     */
    RepositoryItemUidLock createUidAttributeLock( RepositoryItemUid uid );

    /**
     * Creates a shared UIDLock based on a key.
     * 
     * @param key
     * @return lock instance, never {@code null}.
     * @since 2.4
     */
    RepositoryItemUidLock createUidLock( String key );

    /**
     * Creates a shared attribute UIDLock based on a key.
     * 
     * @param key
     * @return lock instance, never {@code null}.
     * @since 2.4
     */
    RepositoryItemUidLock createUidAttributeLock( String key );
}
