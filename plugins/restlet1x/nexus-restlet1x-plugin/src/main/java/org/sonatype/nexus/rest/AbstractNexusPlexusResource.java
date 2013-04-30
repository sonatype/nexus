/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2013 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.rest;

import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.lang.StringEscapeUtils;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Requirement;
import org.restlet.Context;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Status;
import org.sonatype.configuration.ConfigurationException;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.configuration.validation.ValidationMessage;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.Nexus;
import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.registry.RepositoryTypeDescriptor;
import org.sonatype.nexus.proxy.registry.RepositoryTypeRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.plexus.rest.ReferenceFactory;
import org.sonatype.plexus.rest.resource.AbstractPlexusResource;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.plexus.rest.resource.PlexusResourceException;
import org.sonatype.plexus.rest.resource.error.ErrorMessage;
import org.sonatype.plexus.rest.resource.error.ErrorResponse;

public abstract class AbstractNexusPlexusResource
    extends AbstractPlexusResource
    implements PlexusResource
{
    public static final String NEXUS_INSTANCE_LOCAL = "local";

    public static final String PASSWORD_PLACE_HOLDER = "|$|N|E|X|U|S|$|";

    public static final String IS_LOCAL_PARAMETER = "isLocal";

    public static final String IS_REMOTE_PARAMETER = "isRemote";

    public static final String AS_EXPIRED_PARAMETER = "asExpired";

    @Requirement
    private Nexus nexus;

    @Requirement
    private NexusConfiguration nexusConfiguration;

    @Requirement( hint = "protected" )
    private RepositoryRegistry repositoryRegistry;

    @Requirement( hint = "default" )
    private RepositoryRegistry defaultRepositoryRegistry;

    @Requirement
    private RepositoryTypeRegistry repoTypeRegistry;

    @Requirement
    private ReferenceFactory referenceFactory;

    protected Nexus getNexus()
    {
        return nexus;
    }

    protected NexusConfiguration getNexusConfiguration()
    {
        return nexusConfiguration;
    }

    protected RepositoryRegistry getRepositoryRegistry()
    {
        return repositoryRegistry;
    }

    protected RepositoryRegistry getUnprotectedRepositoryRegistry()
    {
        return defaultRepositoryRegistry;
    }

    /**
     * For file uploads we are using commons-fileupload integration with restlet.org. We are storing one FileItemFactory
     * instance in context. This method simply encapsulates gettting it from Resource context.
     * 
     * @return
     */
    protected FileItemFactory getFileItemFactory( Context context )
    {
        return (FileItemFactory) context.getAttributes().get( NexusApplication.FILEITEM_FACTORY );
    }

    /**
     * Centralized, since this is the only "dependent" stuff that relies on knowledge where restlet.Application is
     * mounted (we had a /service => / move).
     * 
     * @param request
     * @return
     */
    protected Reference getContextRoot( Request request )
    {
        return this.referenceFactory.getContextRoot( request );
    }

    protected boolean isLocal( Request request, String resourceStorePath )
    {
        // check do we need local only access
        boolean isLocal = request.getResourceRef().getQueryAsForm().getFirst( IS_LOCAL_PARAMETER ) != null;

        if ( resourceStorePath != null )
        {
            // overriding isLocal is we know it will be a collection
            isLocal = isLocal || resourceStorePath.endsWith( RepositoryItemUid.PATH_SEPARATOR );
        }

        return isLocal;
    }

    protected boolean isRemote( Request request, String resourceStorePath )
    {
        // check do we need remote only access
        boolean isRemote = request.getResourceRef().getQueryAsForm().getFirst( IS_REMOTE_PARAMETER ) != null;

        return isRemote;
    }

    protected boolean asExpired( Request request, String resourceStorePath )
    {
        // check do we need expired access
        boolean isRemote = request.getResourceRef().getQueryAsForm().getFirst( AS_EXPIRED_PARAMETER ) != null;

        return isRemote;
    }

    private Reference updateBaseRefPath( Reference reference )
    {
        if ( reference.getBaseRef().getPath() == null )
        {
            reference.getBaseRef().setPath( "/" );
        }
        else if ( !reference.getBaseRef().getPath().endsWith( "/" ) )
        {
            reference.getBaseRef().setPath( reference.getBaseRef().getPath() + "/" );
        }

        return reference;
    }

    protected Reference createReference( Reference base, String relPart )
    {
        Reference ref = new Reference( base, relPart );

        return updateBaseRefPath( ref ).getTargetRef();
    }

    protected Reference createChildReference( Request request, PlexusResource resource, String childPath )
    {
        return this.referenceFactory.createChildReference( request, childPath );
    }

    protected Reference createRootReference( Request request, String relPart )
    {
        Reference ref = new Reference( getContextRoot( request ), relPart );

        if ( !ref.getBaseRef().getPath().endsWith( "/" ) )
        {
            ref.getBaseRef().setPath( ref.getBaseRef().getPath() + "/" );
        }

        return ref.getTargetRef();
    }

    /**
     * Builds a reference to Repository's content root.
     * 
     * @param request
     * @param repoId
     * @return
     * @deprecated Use RepositoryURLBuilder component instead.
     */
    @Deprecated
    protected Reference createRepositoryContentReference( Request request, String repoId )
    {
        try
        {
            Repository repo = repositoryRegistry.getRepository( repoId );

            for ( RepositoryTypeDescriptor desc : repoTypeRegistry.getRegisteredRepositoryTypeDescriptors() )
            {
                if ( NexusCompat.getRepositoryProviderRole( repo ).equals( desc.getRole().getName() ) )
                {
                    return createReference( getContextRoot( request ), "content/" + desc.getPrefix() + "/" + repoId ).getTargetRef();
                }
            }

            getLogger().error(
                "Cannot create reference for repository with id=\"" + repo.getId() + "\" having role of "
                    + NexusCompat.getRepositoryProviderRole( repo ) );
        }
        catch ( NoSuchRepositoryException e )
        {
            getLogger().error( "Cannot create reference for non-existant repository with id=\"" + repoId + "\"" );
        }

        return null;
    }

    protected Reference createRepositoryReference( Request request, String repoId )
    {
        return createReference( getContextRoot( request ), "service/local/repositories/" + repoId ).getTargetRef();
    }

    protected Reference createRepositoryReference( Request request, String repoId, String repoPath )
    {
        Reference repoRootRef = createRepositoryReference( request, repoId );

        if ( repoPath.startsWith( RepositoryItemUid.PATH_SEPARATOR ) )
        {
            repoPath = repoPath.substring( 1 );
        }

        repoPath = "content/" + repoPath;

        return createReference( repoRootRef, repoPath );
    }

    protected Reference createRepositoryGroupReference( Request request, String groupId )
    {
        return createReference( getContextRoot( request ), "service/local/repo_groups/" + groupId ).getTargetRef();
    }

    protected Reference createRepositoryGroupReference( Request request, String groupId, String repoPath )
    {
        Reference groupRootRef = createRepositoryGroupReference( request, groupId );

        if ( repoPath.startsWith( RepositoryItemUid.PATH_SEPARATOR ) )
        {
            repoPath = repoPath.substring( 1 );
        }

        repoPath = "content/" + repoPath;

        return createReference( groupRootRef, repoPath );
    }

    protected Reference createRedirectReference( Request request )
    {
        String uriPart =
            request.getResourceRef().getTargetRef().toString().substring(
                request.getRootRef().getTargetRef().toString().length() );

        // trim leading slash
        if ( uriPart.startsWith( "/" ) )
        {
            uriPart = uriPart.substring( 1 );
        }

        Reference result = updateBaseRefPath( new Reference( getContextRoot( request ), uriPart ) ).getTargetRef();

        return result;
    }

    // ===

    protected PlexusContainer getPlexusContainer( Context context )
    {
        return (PlexusContainer) context.getAttributes().get( PlexusConstants.PLEXUS_KEY );
    }

    protected ErrorResponse getNexusErrorResponse( String id, String msg )
    {
        ErrorResponse ner = new ErrorResponse();
        ErrorMessage ne = new ErrorMessage();
        ne.setId( id );
        ne.setMsg( StringEscapeUtils.escapeHtml( msg ) );
        ner.addError( ne );
        return ner;
    }

    protected void handleInvalidConfigurationException( InvalidConfigurationException e )
        throws PlexusResourceException
    {
        getLogger().warn( "Configuration error!", e );

        ErrorResponse nexusErrorResponse;

        ValidationResponse vr = e.getValidationResponse();

        if ( vr != null && vr.getValidationErrors().size() > 0 )
        {
            org.sonatype.configuration.validation.ValidationMessage vm = vr.getValidationErrors().get( 0 );
            nexusErrorResponse = getNexusErrorResponse( vm.getKey(), vm.getShortMessage() );
        }
        else
        {
            nexusErrorResponse = getNexusErrorResponse( "*", e.getMessage() );
        }

        throw new PlexusResourceException( Status.CLIENT_ERROR_BAD_REQUEST, "Configuration error.", nexusErrorResponse );
    }

    protected void handleConfigurationException( ConfigurationException e )
        throws PlexusResourceException
    {
        getLogger().warn( "Configuration error!", e );

        ErrorResponse nexusErrorResponse;

        if ( InvalidConfigurationException.class.isAssignableFrom( e.getClass() ) )
        {
            ValidationResponse vr = ( (InvalidConfigurationException) e ).getValidationResponse();

            if ( vr != null && vr.getValidationErrors().size() > 0 )
            {
                ValidationMessage vm = vr.getValidationErrors().get( 0 );
                nexusErrorResponse = getNexusErrorResponse( vm.getKey(), vm.getShortMessage() );
            }
            else
            {
                nexusErrorResponse = getNexusErrorResponse( "*", e.getMessage() );
            }
        }
        else
        {
            nexusErrorResponse = getNexusErrorResponse( "*", e.getMessage() );
        }

        throw new PlexusResourceException( Status.CLIENT_ERROR_BAD_REQUEST, "Configuration error.", nexusErrorResponse );
    }

    protected Reference createRedirectBaseRef( Request request )
    {
        return createReference( getContextRoot( request ), "service/local/artifact/maven/redirect" ).getTargetRef();
    }

    protected String getValidRemoteIPAddress( Request request )
    {
        return RemoteIPFinder.findIP( request );
    }

    protected String getActualPassword( String newPassword, String oldPassword )
    {
        if ( PASSWORD_PLACE_HOLDER.equals( newPassword ) )
        {
            return oldPassword;
        }

        return newPassword;
    }
}
