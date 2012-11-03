/*
 * Copyright (c) 2007-2012 Sonatype, Inc.  All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/central-secure/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package org.sonatype.nexus.plugins.siesta;

import org.sonatype.sisu.siesta.common.Resource;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Date;

// FIXME: Remove eventually, and/or replace with a more meaningful resource

/**
 * Siesta testing resource.
 *
 * @since 2.3
 */
@Named
@Singleton
@Path("/test")
public class TestResource
    implements Resource
{
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() {
        return String.format("Hello %s", new Date());
    }
}
