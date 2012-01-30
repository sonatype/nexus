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
package org.sonatype.nexus.proxy.repository;

import org.sonatype.nexus.plugins.RepositoryType;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.events.RepositoryItemEvent;
import org.sonatype.nexus.proxy.registry.ContentClass;

/**
 * A Shadow Repository is a special repository type that usually points to a master repository and transforms it in some
 * way (look at Maven1 to Maven2 layout changing repo).
 * 
 * @author cstamas
 */
@RepositoryType( pathPrefix = "shadows" )
public interface ShadowRepository
    extends Repository
{
    /**
     * The content class that is expected to have the repository set as master for this ShadowRepository.
     * 
     * @return
     */
    ContentClass getMasterRepositoryContentClass();

    /**
     * Gets sync at startup.
     * 
     * @return
     */
    boolean isSynchronizeAtStartup();

    /**
     * Sets sync at start.
     * 
     * @param value
     */
    void setSynchronizeAtStartup( boolean value );

    /**
     * Returns the master repository of this ShadowRepository.
     * 
     * @return
     * @deprecated Use {@link #getMasterRepository()}.getId() instead.
     */
    @Deprecated
    String getMasterRepositoryId();

    /**
     * Sets the master repository of this ShadowRepository.
     * 
     * @param masterRepository
     * @throws NoSuchRepositoryException
     * @throws IncompatibleMasterRepositoryException
     * @deprecated Use {@link #setMasterRepository(Repository)} instead.
     */
    @Deprecated
    void setMasterRepositoryId( String masterRepositoryId )
        throws NoSuchRepositoryException, IncompatibleMasterRepositoryException;

    /**
     * Returns the master.
     * 
     * @return
     */
    Repository getMasterRepository();

    /**
     * Sets the master.
     * 
     * @return
     */
    void setMasterRepository( Repository repository )
        throws IncompatibleMasterRepositoryException;

    /**
     * Triggers syncing with master repository.
     */
    void synchronizeWithMaster();

    /**
     * Performs some activity if the event is coming from it's master repository. Implementation should filter and take
     * care what repository is the origin of the event, and simply discard event if not interested in it.
     * 
     * @param evt
     * @since 2.0
     */
    void onRepositoryItemEvent( RepositoryItemEvent evt );
}
