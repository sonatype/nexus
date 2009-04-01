package org.sonatype.nexus.restlight.stage;

/**
 * Simple container for details of a staging repository. This minimal model is used in queries for staging-repository
 * information, and for feeding back into finish/drop/promote actions within the {@link StageClient}.
 */
public class StageRepository
{

    private final String profileId;

    private final String repositoryId;

    private String url;

    private final boolean isOpen;

    private String user;

    public StageRepository( final String profileId, final String repositoryId, final boolean isOpen )
    {
        this.profileId = profileId;
        this.repositoryId = repositoryId;
        this.isOpen = isOpen;
    }

    public boolean isOpen()
    {
        return isOpen;
    }

    public String getProfileId()
    {
        return profileId;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    @Override
    public String toString()
    {
        return ( url == null ? "(No URI)" : url ) + "\n[profile: '" + profileId + "', repository: '" + repositoryId + "', open? " + isOpen + "]";
    }

    /**
     * Return the publicly-available repository URL for this repository. This is the URL that Maven and other clients
     * would use to access artifacts in this repository.
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * Set the publicly-available repository URL for this repository. This is the URL that Maven and other clients would
     * use to access artifacts in this repository.
     */
    public void setUrl( final String url )
    {
        this.url = url;
    }

    public void setUser( final String user )
    {
        this.user = user;
    }

    public String getUser()
    {
        return user;
    }

}
