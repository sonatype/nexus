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
package org.sonatype.nexus.proxy.attributes;

import java.util.HashMap;

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.item.RepositoryItemUid;

/**
 * A HashMap implementation of Attribute Storage. Usable for tests etc, since it actually does not persists anything.
 * Part of NEXUS-4628 "alternate" AttributeStorage implementations.
 */
@Typed( AttributeStorage.class )
@Named( "hashmap" )
@Singleton
public class HashMapAttributeStorage
    extends AbstractAttributeStorage
    implements AttributeStorage
{
    private HashMap<String, Attributes> storageMap = new HashMap<String, Attributes>();

    @Override
    public Attributes getAttributes( final RepositoryItemUid uid )
    {
        return storageMap.get( uid.getKey() );
    }

    @Override
    public void putAttributes( final RepositoryItemUid uid, final Attributes item )
    {
        storageMap.put( uid.getKey(), item );
    }

    @Override
    public boolean deleteAttributes( RepositoryItemUid uid )
    {
        return storageMap.remove( uid.getKey() ) != null;
    }
}
