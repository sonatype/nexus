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
package org.sonatype.nexus.plugins.rest;

import java.util.Map;

import javax.inject.Singleton;

import org.sonatype.plugin.ExtensionPoint;

/**
 * A Resource bundle meant for extending/contributing/spoofing existing resources (JS, CSS, Images, etc) of the Nexus
 * Web Application. This component is able only to contribute to index.html, if you want to extends REST API, please
 * see PlexusResource.
 * <p>
 * The getHeadContribution() and getBodyContribution() methods should return strings that will finally make into
 * "index"html" (if not overridden that too by some other resource!). This enables you to simply add new JS files to the
 * index.html header, or add custom parts to the HEAD and BODY section of the index.html.
 * <p>
 * The returned String of the above mentioned methods will be processed by a Velocity Engine, so you are able to return
 * plain HTML but also arbitrary Velocity template. The following context variables are available:
 * <ul>
 * <li>${serviceBase} - the path to local services base ("service/local" by default)</li>
 * <li>${contentBase} - the path to content base ("content" by default)</li>
 * <li>${nexusRoot} - the root path of Nexus Web Application</li>
 * <li>${bundle} - the instance of this ResourceBundle (the contributor, this instance)</li>
 * <li>${nexusVersion} - the version of Nexus that currently runs, see NEXUS-932</li>
 * </ul>
 * <p>
 * Both methods is getting the context map used to evaluate the templates, so they can easily affect the context if
 * needed.
 * 
 * @author cstamas
 */
@ExtensionPoint
@Singleton
public interface NexusIndexHtmlCustomizer
{
    /**
     * A header contribution is a HTML snippet, that will be injected into the beginning of HEAD section of the
     * index.html. The snippet will be processed by Velocity, so it can be a Velocity template too. The context passed
     * in may be modified by this bundle, and it will be finally used to evaluate the template returned by this call.
     * 
     * @return
     */
    String getPreHeadContribution( Map<String, Object> context );

    /**
     * A header contribution is a HTML snippet, that will be injected into the end of HEAD section of the index.html.
     * The snippet will be processed by Velocity, so it can be a Velocity template too. The context passed in may be
     * modified by this bundle, and it will be finally used to evaluate the template returned by this call.
     * 
     * @return
     */
    String getPostHeadContribution( Map<String, Object> context );

    /**
     * A header contribution is a HTML snippet, that will be injected into the beginning of BODY section of the
     * index.html. The snippet will be processed by Velocity, so it can be a Velocity template too.The context passed in
     * may be modified by this bundle, and it will be finally used to evaluate the template returned by this call.
     * 
     * @return
     */
    String getPreBodyContribution( Map<String, Object> context );

    /**
     * A header contribution is a HTML snippet, that will be injected into the end of BODY section of the index.html.
     * The snippet will be processed by Velocity, so it can be a Velocity template too. The context passed in may be
     * modified by this bundle, and it will be finally used to evaluate the template returned by this call.
     * 
     * @return
     */
    String getPostBodyContribution( Map<String, Object> context );
}
