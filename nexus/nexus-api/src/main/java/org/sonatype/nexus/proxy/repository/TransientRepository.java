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
package org.sonatype.nexus.proxy.repository;

import java.util.List;

import org.sonatype.nexus.plugins.RepositoryType;

/**
 * A transient repository marker interface. A transient repository is a repository that is known that have "transient"
 * nature. They are usually programtically (by a plugin) added and removed, and they are not intended to have long
 * lifespan (as opposed to "normal" user configured repositories). An example of such are Staging repositories.
 *
 * @author cstamas
 * @since 2.3
 */
public interface TransientRepository
    extends Repository
{

}
