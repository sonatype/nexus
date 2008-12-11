package org.sonatype.nexus.proxy.repository;

import org.sonatype.nexus.proxy.registry.ContentClass;

/**
 * Default group repository implementation.
 */
public class DefaultGroupRepository
    extends AbstractGroupRepository
{

    private final ContentClass contentClass;

    public DefaultGroupRepository( ContentClass contentClass )
    {
        this.contentClass = contentClass;
    }

    public ContentClass getRepositoryContentClass()
    {
        return contentClass;
    }

}
