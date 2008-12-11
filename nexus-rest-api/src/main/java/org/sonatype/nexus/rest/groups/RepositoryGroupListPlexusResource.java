package org.sonatype.nexus.rest.groups;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.nexus.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.model.CRepositoryGroup;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.NoSuchRepositoryGroupException;
import org.sonatype.nexus.proxy.registry.InvalidGroupingException;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.rest.model.RepositoryGroupListResource;
import org.sonatype.nexus.rest.model.RepositoryGroupListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryGroupMemberRepository;
import org.sonatype.nexus.rest.model.RepositoryGroupResource;
import org.sonatype.nexus.rest.model.RepositoryGroupResourceResponse;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.plexus.rest.resource.PlexusResourceException;

/**
 * A resource list for RepositoryGroup list.
 * 
 * @author cstamas
 * @author tstevens
 */
@Component( role = PlexusResource.class, hint = "RepositoryGroupListPlexusResource" )
public class RepositoryGroupListPlexusResource
    extends AbstractRepositoryGroupPlexusResource
{

    public RepositoryGroupListPlexusResource()
    {
        this.setModifiable( true );
    }

    @Override
    public Object getPayloadInstance()
    {
        return new RepositoryGroupResourceResponse();
    }

    @Override
    public String getResourceUri()
    {
        return "/repo_groups";
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        return new PathProtectionDescriptor( getResourceUri(), "authcBasic,perms[nexus:repogroups]" );
    }

    @Override
    public Object get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        RepositoryGroupListResourceResponse result = new RepositoryGroupListResourceResponse();

        Collection<Repository> repositories = getNexus().getRepositories();

        for ( Repository repository : repositories )
        {
            if ( ! ( repository instanceof GroupRepository ) )
            {
                continue;
            }
            
            GroupRepository group = (GroupRepository) repository;

            RepositoryGroupListResource resource = new RepositoryGroupListResource();

            resource.setResourceURI( createChildReference( request, group.getId() ).toString() );

            resource.setId( group.getId() );

            resource.setFormat( group.getRepositoryContentClass().getId() );

            resource.setName( group.getName() );

            // just to trigger list creation, and not stay null coz of XStream serialization
            resource.getRepositories();

            result.addData( resource );

        }

        return result;
    }

    @Override
    public Object post( Context context, Request request, Response response, Object payload )
        throws ResourceException
    {
        RepositoryGroupResourceResponse groupRequest = (RepositoryGroupResourceResponse) payload;

        if ( groupRequest != null )
        {
            RepositoryGroupResource resource = groupRequest.getData();

            if ( resource.getRepositories() == null || resource.getRepositories().size() == 0 )
            {
                getLogger()
                    .info( "The repository group with ID=" + resource.getId() + " have zero repository members!" );

                throw new PlexusResourceException(
                    Status.CLIENT_ERROR_BAD_REQUEST,
                    "The group cannot have zero repository members!",
                    getNexusErrorResponse( "repositories", "The group cannot have zero repository members!" ) );
            }

            try
            {
                CRepositoryGroup group = getNexus().readRepositoryGroup( resource.getId() );

                if ( group != null )
                {
                    getLogger().info( "The repository group with ID=" + group.getGroupId() + " already exists!" );

                    throw new PlexusResourceException( Status.CLIENT_ERROR_BAD_REQUEST, "The repository group with ID="
                        + group.getGroupId() + " already exists!", getNexusErrorResponse(
                        "id",
                        "The repository group with id=" + group.getGroupId() + " already exists!" ) );
                }
            }
            catch ( NoSuchRepositoryGroupException ex )
            {
                CRepositoryGroup group = new CRepositoryGroup();

                group.setGroupId( resource.getId() );

                group.setName( resource.getName() );

                try
                {
                    validateGroup( resource, request );

                    for ( RepositoryGroupMemberRepository member : (List<RepositoryGroupMemberRepository>) resource
                        .getRepositories() )
                    {
                        group.addRepository( member.getId() );
                    }

                    getNexus().createRepositoryGroup( group );
                    //
                    // response.setStatus( Status.SUCCESS_NO_CONTENT );
                }
                catch ( ConfigurationException e )
                {
                    handleConfigurationException( e );
                }
                catch ( NoSuchRepositoryException e )
                {
                    getLogger().warn(
                        "Repository referenced by Repository Group Not Found, GroupId=" + group.getGroupId(),
                        e );

                    throw new PlexusResourceException(
                        Status.CLIENT_ERROR_BAD_REQUEST,
                        "Repository referenced by Repository Group Not Found, GroupId=" + group.getGroupId(),
                        getNexusErrorResponse( "repositories", "Repository referenced by Repository Group Not Found" ) );
                }
                catch ( InvalidGroupingException e )
                {
                    getLogger().warn( "Invalid grouping detected!, GroupId=" + group.getGroupId(), e );

                    throw new PlexusResourceException(
                        Status.CLIENT_ERROR_BAD_REQUEST,
                        "Invalid grouping requested, GroupId=" + group.getGroupId(),
                        getNexusErrorResponse(
                            "repositories",
                            "Repository referenced by Repository Group does not share same content type!" ) );
                }
                catch ( IOException e )
                {
                    getLogger().warn( "Got IO Exception!", e );

                    throw new ResourceException( Status.SERVER_ERROR_INTERNAL );
                }
            }
        }
        // TODO: return the group
        return null;
    }

}
