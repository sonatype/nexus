package org.sonatype.nexus.repositories;

import junit.framework.Assert;
import org.junit.Test;
import org.sonatype.nexus.configuration.application.GlobalRestApiSettings;
import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.proxy.AbstractNexusTestCase;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.templates.TemplateProvider;
import org.sonatype.nexus.templates.repository.AbstractRepositoryTemplateProvider;
import org.sonatype.nexus.templates.repository.DefaultRepositoryTemplateProvider;
import org.sonatype.nexus.templates.repository.RepositoryTemplate;

public class M2RepositoryCreationTest
    extends AbstractNexusTestCase
{

    @Test
    public void testCreateRepositoryAsRepositoryNotCRepo()
        throws Exception
    {
        this.lookup( NexusConfiguration.class ).loadConfiguration();

        GlobalRestApiSettings globalRestApiSettings = this.lookup( GlobalRestApiSettings.class );
        globalRestApiSettings.setBaseUrl( "http://localhost:8080/nexus" );
        globalRestApiSettings.commitChanges();

        AbstractRepositoryTemplateProvider repositoryTemplateProvider =
            (AbstractRepositoryTemplateProvider) this.lookup( TemplateProvider.class,
                                                              DefaultRepositoryTemplateProvider.PROVIDER_ID );

        RepositoryTemplate template =
            (RepositoryTemplate) repositoryTemplateProvider.getTemplateById( "default_hosted_release" );

        M2Repository repository = (M2Repository) template.createWithoutCommit();
        repository.setId( "foo" );
        repository.setLocalUrl( "http://local/url" );
        repository.commitChanges();

        // repo is NOT in the repository registry yet
        RepositoryRegistry repositoryRegistry = this.lookup( RepositoryRegistry.class );

        try
        {
            repositoryRegistry.getRepository( "foo" );
            Assert.fail( "expected NoSuchRepositoryException" );
        }
        catch( NoSuchRepositoryException e)
        {
            // expected
        }

        repositoryRegistry.addRepository( repository );
        Repository repoFromReg = repositoryRegistry.getRepository( "foo" );
        Assert.assertEquals( "foo", repoFromReg.getName() ); // name defaults to id
        Assert.assertEquals( "http://local/url", repoFromReg.getLocalUrl() );
    }

    @Test
    public void testBaseURLNotSet() throws Exception
    {
        this.lookup( NexusConfiguration.class ).loadConfiguration();

        AbstractRepositoryTemplateProvider repositoryTemplateProvider =
            (AbstractRepositoryTemplateProvider) this.lookup( TemplateProvider.class,
                                                              DefaultRepositoryTemplateProvider.PROVIDER_ID );

        RepositoryTemplate template =
            (RepositoryTemplate) repositoryTemplateProvider.getTemplateById( "default_hosted_release" );

        M2Repository repository = (M2Repository) template.createWithoutCommit();
        repository.setId( "foo" );
        repository.setLocalUrl( "http://local/url" );
        repository.commitChanges();

    }

}
