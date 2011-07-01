package org.sonatype.nexus.proxy.repository;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;

/**
 * Builds a URL for a repository.
 */
public interface RepositoryURLBuilder
{
    /**
     * Returns the URL of a repository by Id.
     * @param repositoryId
     * @return the published url.
     */
    String getRepositoryUrl( String repositoryId )
        throws NoSuchRepositoryException;

    /**
     * Returns the URL of a repository.
     * @param repository
     * @return the published url.
     */
    String getRepositoryUrl( Repository repository );
}
