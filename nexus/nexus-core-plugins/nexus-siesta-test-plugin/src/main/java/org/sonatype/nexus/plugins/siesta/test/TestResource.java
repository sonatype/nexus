/*
 * Copyright (c) 2007-2012 Sonatype, Inc.  All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/central-secure/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package org.sonatype.nexus.plugins.siesta.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.plugins.siesta.test.model.UserXO;
import org.sonatype.sisu.siesta.common.Resource;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.Date;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;

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
    private final Logger log = LoggerFactory.getLogger(getClass());

    @GET
    @Produces({APPLICATION_XML, APPLICATION_JSON})
    public UserXO get() {
        log.info("GET");

        return new UserXO()
            .withName("jdillon")
            .withDescription("avid crack smoker")
            .withCreated(new Date());
    }

    @PUT
    @Consumes({APPLICATION_XML, APPLICATION_JSON})
    public void put(final UserXO user) {
        log.info("PUT name='{}' description='{}' created='{}'",
            user.getName(), user.getDescription(), user.getCreated());
    }
}
