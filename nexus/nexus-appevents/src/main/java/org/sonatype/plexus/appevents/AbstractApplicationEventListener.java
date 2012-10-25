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
package org.sonatype.plexus.appevents;

import javax.inject.Inject;

/**
 * Support for {@link EventListener} implementations.
 *
 * @since 2.3
 *
 * @deprecated Sisu should detect listeners if bound properly (ie. @Named or explicit module binding to EventListener.class).
 */
@Deprecated
public abstract class AbstractApplicationEventListener
    implements EventListener
{
    @Inject
    public AbstractApplicationEventListener(final ApplicationEventMulticaster multicaster) {
        multicaster.addEventListener(this);
    }
}
