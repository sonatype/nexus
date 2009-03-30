package org.sonatype.nexus.restlight.stage;

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
    
    public String toString()
    {
        return ( url == null ? "(No URI)" : url ) + "\n[profile: '" + profileId + "', repository: '" + repositoryId + "', open? " + isOpen + "]";
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    public void setUser( String user )
    {
        this.user = user;
    }
    
    public String getUser()
    {
        return user;
    }

}
