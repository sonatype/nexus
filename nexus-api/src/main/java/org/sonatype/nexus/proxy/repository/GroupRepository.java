package org.sonatype.nexus.proxy.repository;

import java.util.List;
import java.util.Map;

import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageItem;

/**
 * A group repository is simply as it's name says, a repository that is backed by a group of other repositories. There
 * is one big constraint, they are READ ONLY. Usually, if you try a write/delete operation against this kind of
 * repository, you are doing something wrong. Deploys/writes and deletes should be done directly against the
 * hosted/proxied repositories, not against these "aggregated" ones.
 * 
 * @author cstamas
 */
public interface GroupRepository
    extends Repository
{
    /**
     * Returns the unmodifiable list of Repositories that are group members in this GroupRepository. The repo order
     * within list is repo rank (the order how they will be processed), so processing is possible by simply iterating
     * over resulting list.
     * 
     * @return a List<Repository>
     */
    List<Repository> getMemberRepositories();

    List<StorageItem> doRetrieveItems( boolean localOnly, RepositoryItemUid uid, Map<String, Object> context );
}
