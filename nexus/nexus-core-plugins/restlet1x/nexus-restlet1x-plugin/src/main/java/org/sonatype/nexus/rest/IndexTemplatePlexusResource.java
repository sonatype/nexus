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
package org.sonatype.nexus.rest;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Configuration;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.util.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.nexus.Nexus;
import org.sonatype.nexus.plugins.rest.NexusIndexHtmlCustomizer;
import org.sonatype.plexus.rest.representation.VelocityRepresentation;
import org.sonatype.plexus.rest.resource.AbstractPlexusResource;
import org.sonatype.plexus.rest.resource.ManagedPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.sisu.velocity.Velocity;

@Component( role = ManagedPlexusResource.class, hint = "indexTemplate" )
public class IndexTemplatePlexusResource
    extends AbstractPlexusResource
    implements ManagedPlexusResource, Initializable
{
    @Requirement
    private Nexus nexus;

    @Requirement( role = NexusIndexHtmlCustomizer.class )
    private Map<String, NexusIndexHtmlCustomizer> bundles;
    
    @Requirement
    private Velocity velocity;

    @Configuration( value = "${index.template.file}" )
    String templateFilename;

    public IndexTemplatePlexusResource()
    {
        super();

        setReadable( true );

        setModifiable( false );
    }

    @Override
    public Object getPayloadInstance()
    {
        // RO resource
        return null;
    }

    @Override
    public String getResourceUri()
    {
        return "/index.html";
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        // unprotected
        return null;
    }

    public List<Variant> getVariants()
    {
        List<Variant> result = super.getVariants();

        result.clear();

        result.add( new Variant( MediaType.APPLICATION_XHTML_XML ) );

        return result;
    }

    public Representation get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        return render( context, request, response, variant );
    }

    protected VelocityRepresentation render( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        Map<String, Object> templatingContext = new HashMap<String, Object>();

        templatingContext.put( "serviceBase", "service/local" );

        templatingContext.put( "contentBase", "content" );

        templatingContext.put( "nexusVersion", nexus.getSystemStatus().getVersion() );

        templatingContext.put( "nexusRoot", request.getRootRef().toString() );

        // gather plugin stuff

        Map<String, Object> topContext = new HashMap<String, Object>( templatingContext );

        Map<String, Object> pluginContext = null;

        List<String> pluginPreHeadContributions = new ArrayList<String>();
        List<String> pluginPostHeadContributions = new ArrayList<String>();

        List<String> pluginPreBodyContributions = new ArrayList<String>();
        List<String> pluginPostBodyContributions = new ArrayList<String>();

        List<String> pluginJsFiles = new ArrayList<String>();

        for ( String key : bundles.keySet() )
        {
            pluginContext = new HashMap<String, Object>( topContext );

            NexusIndexHtmlCustomizer bundle = bundles.get( key );

            pluginContext.put( "bundle", bundle );

            // pre HEAD

            String preHeadTemplate = bundle.getPreHeadContribution( pluginContext );

            evaluateIfNeeded( pluginContext, preHeadTemplate, pluginPreHeadContributions );

            // post HEAD

            String postHeadTemplate = bundle.getPostHeadContribution( pluginContext );

            final Document html = Jsoup.parse( postHeadTemplate );
            final Elements scripts = html.select( "script" );
            for ( Element script : scripts )
            {
                final String src = script.attr( "src" );
                if ( !src.isEmpty() )
                {
                    pluginJsFiles.add( src );
                    script.remove();
                }
            }
            postHeadTemplate = html.head().children().toString();

            evaluateIfNeeded( pluginContext, postHeadTemplate, pluginPostHeadContributions );

            // pre BODY

            String preBodyTemplate = bundle.getPreBodyContribution( pluginContext );

            evaluateIfNeeded( pluginContext, preBodyTemplate, pluginPreBodyContributions );

            // post BODY

            String postBodyTemplate = bundle.getPostBodyContribution( pluginContext );

            evaluateIfNeeded( pluginContext, postBodyTemplate, pluginPostBodyContributions );
        }

        templatingContext.put( "appName", nexus.getSystemStatus().getAppName() );
        templatingContext.put( "formattedAppName", nexus.getSystemStatus().getFormattedAppName() );

        templatingContext.put( "pluginPreHeadContributions", pluginPreHeadContributions );
        templatingContext.put( "pluginPostHeadContributions", pluginPostHeadContributions );

        templatingContext.put( "pluginPreBodyContributions", pluginPreBodyContributions );
        templatingContext.put( "pluginPostBodyContributions", pluginPostBodyContributions );

        templatingContext.put( "pluginJsFiles", pluginJsFiles );

        final String query = request.getResourceRef().getQuery();
        String debug = null;
        if ( query != null && query.contains( "debug" ) )
        {
            debug = "-debug";
        }
        templatingContext.put( "debug", debug );

        return new VelocityRepresentation( context, templateFilename, getClass().getClassLoader(), templatingContext, MediaType.TEXT_HTML );
    }

    protected void evaluateIfNeeded( Map<String, Object> context, String template,
                                     List<String> results )
        throws ResourceException
    {
        if ( !StringUtils.isEmpty( template ) )
        {
            StringWriter result = new StringWriter();

            try
            {
                if ( velocity.getEngine().evaluate( new VelocityContext( context ), result, getClass().getName(), template ) )
                {
                    results.add( result.toString() );
                }
                else
                {
                    throw new ResourceException( Status.SERVER_ERROR_INTERNAL,
                        "Was not able to interpolate (check the logs for Velocity messages about the reason)!" );
                }
            }
            catch ( Exception e )
            {
                throw new ResourceException(
                    Status.SERVER_ERROR_INTERNAL,
                    "Got Exception exception during Velocity invocation!",
                    e );
            }
        }
    }

    public void initialize()
        throws InitializationException
    {
        // Hasn't been interpolated
        if ( "${index.template.file}".equals( templateFilename ) )
        {
            templateFilename = "templates/index.vm";
        }
    }
}
