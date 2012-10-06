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

package org.sonatype.nexus.proxy.repository.management;

import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.sisu.goodies.jmx.StandardMBeanSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link RepositoryMBean} implementation.
 *
 * @since 2.2
 */
public class RepositoryMBeanImpl
    extends StandardMBeanSupport
    implements RepositoryMBean
{
    // FIXME: Really should implement a dynamic mbean to expose all of the exposed details

    private final Repository repository;

    public RepositoryMBeanImpl(final Repository repository) {
        super(RepositoryMBean.class, false);
        this.repository = checkNotNull(repository);
    }

    @Override
    public String getProviderRole() {
        return repository.getProviderRole();
    }

    @Override
    public String getProviderHint() {
        return repository.getProviderHint();
    }

    @Override
    public String getId() {
        return repository.getId();
    }

    @Override
    public String getName() {
        return repository.getName();
    }

    @Override
    public String getLocalUrl() {
        return repository.getLocalUrl();
    }
}
