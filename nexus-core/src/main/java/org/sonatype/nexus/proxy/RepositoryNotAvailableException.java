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
package org.sonatype.nexus.proxy;

import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;

/**
 * Thrown if the repository involved in processing is not available.
 * 
 * @author cstamas
 */
public class RepositoryNotAvailableException
    extends IllegalOperationException
{
    private static final long serialVersionUID = 6414483658234772613L;

    private final Repository repository;

    public RepositoryNotAvailableException( Repository repository )
    {
        super( "Repository " + RepositoryStringUtils.getHumanizedNameString( repository ) + " is not available!" );
        this.repository = repository;
    }

    public Repository getRepository()
    {
        return repository;
    }
}
