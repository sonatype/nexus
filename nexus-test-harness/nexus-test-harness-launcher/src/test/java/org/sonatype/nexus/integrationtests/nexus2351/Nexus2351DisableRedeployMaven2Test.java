package org.sonatype.nexus.integrationtests.nexus2351;

import java.io.File;
import java.io.IOException;

import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.test.utils.DeployUtils;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;

public class Nexus2351DisableRedeployMaven2Test
    extends AbstractNexusIntegrationTest
{

    private RepositoryMessageUtil repoUtil = null;

    private File artifact = this.getTestFile( "artifact.jar" );

    private File artifactMD5 = this.getTestFile( "artifact.jar.md5" );

    public Nexus2351DisableRedeployMaven2Test()
        throws ComponentLookupException
    {
        this.repoUtil = new RepositoryMessageUtil( this.getXMLXStream(), MediaType.APPLICATION_XML, this
            .getRepositoryTypeRegistry() );
    }

    @Test
    public void testM2ReleaseAllowRedeploy()
        throws Exception
    {

        String repoId = this.getTestId() + "-testM2ReleaseAllowRedeploy";

        this.createM2Repo( repoId, RepositoryWritePolicy.ALLOW_WRITE, RepositoryPolicy.RELEASE );

        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseAllowRedeploy-1.0.0.jar" );
        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseAllowRedeploy-1.0.0.jar" );
        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseAllowRedeploy-1.0.0.jar" );

        // now test checksums
        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifactMD5,
            "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseAllowRedeploy-1.0.0.jar.md5" );
        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifactMD5,
            "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseAllowRedeploy-1.0.0.jar.md5" );
        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifactMD5,
            "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseAllowRedeploy-1.0.0.jar.md5" );
    }

    @Test
    public void testM2ReleaseNoRedeploy()
        throws Exception
    {
        String repoId = this.getTestId() + "-testM2ReleaseNoRedeploy";

        this.createM2Repo( repoId, RepositoryWritePolicy.ALLOW_WRITE_ONCE, RepositoryPolicy.RELEASE );

        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseNoRedeploy-1.0.0.jar" );

        // checksum should work
        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifactMD5,
            "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseNoRedeploy-1.0.0.jar.md5" );

        try
        {
            DeployUtils.deployWithWagon(
                this.getContainer(),
                "http",
                this.getRepositoryUrl( repoId ),
                artifact,
                "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseNoRedeploy-1.0.0.jar" );
        }
        catch ( TransferFailedException e )
        {
            // expected
        }

        try
        {
            DeployUtils.deployWithWagon(
                this.getContainer(),
                "http",
                this.getRepositoryUrl( repoId ),
                artifact,
                "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseNoRedeploy-1.0.0.jar" );
        }
        catch ( TransferFailedException e )
        {
            // expected
        }

        try
        {
            DeployUtils.deployWithWagon(
                this.getContainer(),
                "http",
                this.getRepositoryUrl( repoId ),
                artifactMD5,
                "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseNoRedeploy-1.0.0.jar.md5" );
        }
        catch ( TransferFailedException e )
        {
            // expected
        }
    }

    @Test
    public void testM2ReleaseReadOnly()
        throws Exception
    {
        String repoId = this.getTestId() + "-testM2ReleaseReadOnly";

        this.createM2Repo( repoId, RepositoryWritePolicy.READ_ONLY, RepositoryPolicy.RELEASE );

        try
        {

            DeployUtils.deployWithWagon(
                this.getContainer(),
                "http",
                this.getRepositoryUrl( repoId ),
                artifact,
                "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseReadOnly-1.0.0.jar" );

        }
        catch ( TransferFailedException e )
        {
            // expected
        }

        try
        {

            DeployUtils.deployWithWagon(
                this.getContainer(),
                "http",
                this.getRepositoryUrl( repoId ),
                artifactMD5,
                "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseReadOnly-1.0.0.jar.md5" );

        }
        catch ( TransferFailedException e )
        {
            // expected
        }

    }

    @Test
    public void testM2SnapshotAllowRedeploy()
        throws Exception
    {
        String repoId = this.getTestId() + "-testM2SnapshotAllowRedeploy";

        this.createM2Repo( repoId, RepositoryWritePolicy.ALLOW_WRITE, RepositoryPolicy.SNAPSHOT );

        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0-SNAPSHOT/testM2ReleaseAllowRedeploy-20090729.054915-216.jar" );
        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0-SNAPSHOT/testM2ReleaseAllowRedeploy-20090729.054915-217.jar" );
        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0-SNAPSHOT/testM2ReleaseAllowRedeploy-20090729.054915-218.jar" );

        // now for the MD5
        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifactMD5,
            "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0-SNAPSHOT/testM2ReleaseAllowRedeploy-20090729.054915-217.jar.md5" );

        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifactMD5,
            "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0-SNAPSHOT/testM2ReleaseAllowRedeploy-20090729.054915-218.jar.md5" );

        // now for just the -SNAPSHOT

        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0-SNAPSHOT/testM2ReleaseAllowRedeploy-SNAPSHOT.jar" );

        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0-SNAPSHOT/testM2ReleaseAllowRedeploy-SNAPSHOT.jar" );

        // MD5
        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifactMD5,
            "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0-SNAPSHOT/testM2ReleaseAllowRedeploy-SNAPSHOT.jar.md5" );

        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifactMD5,
            "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0-SNAPSHOT/testM2ReleaseAllowRedeploy-SNAPSHOT.jar.md5" );

    }

    @Test
    public void testM2SnapshotNoRedeploy()
        throws Exception
    {
        String repoId = this.getTestId() + "-testM2SnapshotNoRedeploy";

        this.createM2Repo( repoId, RepositoryWritePolicy.ALLOW_WRITE_ONCE, RepositoryPolicy.SNAPSHOT );

        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0-SNAPSHOT/testM2ReleaseNoRedeploy-20090729.054915-218.jar" );

        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0-SNAPSHOT/testM2ReleaseNoRedeploy-20090729.054915-219.jar" );

        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0-SNAPSHOT/testM2ReleaseNoRedeploy-20090729.054915-220.jar" );

        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0-SNAPSHOT/testM2ReleaseNoRedeploy-SNAPSHOT.jar" );

        try
        {
            DeployUtils.deployWithWagon(
                this.getContainer(),
                "http",
                this.getRepositoryUrl( repoId ),
                artifact,
                "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0-SNAPSHOT/testM2ReleaseNoRedeploy-SNAPSHOT.jar" );
        }
        catch ( TransferFailedException e )
        {
            // expected
        }
        
        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifactMD5,
            "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0-SNAPSHOT/testM2ReleaseNoRedeploy-SNAPSHOT.jar.md5" );

        try
        {
            DeployUtils.deployWithWagon(
                this.getContainer(),
                "http",
                this.getRepositoryUrl( repoId ),
                artifactMD5,
                "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0-SNAPSHOT/testM2ReleaseNoRedeploy-SNAPSHOT.jar.md5" );
        }
        catch ( TransferFailedException e )
        {
            // expected
        }
    }

    @Test
    public void testM2SnapshotReadOnly()
        throws Exception
    {

        String repoId = this.getTestId() + "-testM2SnapshotReadOnly";

        this.createM2Repo( repoId, RepositoryWritePolicy.READ_ONLY, RepositoryPolicy.SNAPSHOT );

        try
        {

            DeployUtils.deployWithWagon(
                this.getContainer(),
                "http",
                this.getRepositoryUrl( repoId ),
                artifact,
                "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0-SNAPSHOT/testM2ReleaseReadOnly-20090729.054915-218.jar" );

        }
        catch ( TransferFailedException e )
        {
            // expected
        }
        try
        {

            DeployUtils.deployWithWagon(
                this.getContainer(),
                "http",
                this.getRepositoryUrl( repoId ),
                artifactMD5,
                "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0-SNAPSHOT/testM2ReleaseReadOnly-20090729.054915-218.jar.md5" );

        }
        catch ( TransferFailedException e )
        {
            // expected
        }
        try
        {

            DeployUtils.deployWithWagon(
                this.getContainer(),
                "http",
                this.getRepositoryUrl( repoId ),
                artifactMD5,
                "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0-SNAPSHOT/testM2ReleaseReadOnly-SNAPSHOT.jar.md5" );

        }
        catch ( TransferFailedException e )
        {
            // expected
        }
        try
        {

            DeployUtils.deployWithWagon(
                this.getContainer(),
                "http",
                this.getRepositoryUrl( repoId ),
                artifact,
                "testM1Repo.group/testM2ReleaseAllowRedeploy/1.0.0-SNAPSHOT/testM2ReleaseReadOnly-SNAPSHOT.jar" );

        }
        catch ( TransferFailedException e )
        {
            // expected
        }
    }

    private void createM2Repo( String repoId, RepositoryWritePolicy writePolicy, RepositoryPolicy releasePolicy )
        throws IOException
    {
        RepositoryResource repo = new RepositoryResource();

        repo.setId( repoId );
        repo.setBrowseable( true );
        repo.setExposed( true );
        repo.setRepoType( "hosted" );
        repo.setName( repoId );
        repo.setRepoPolicy( releasePolicy.name() );
        repo.setWritePolicy( writePolicy.name() );
        repo.setProvider( "maven2" );
        repo.setFormat( "maven2" );

        this.repoUtil.createRepository( repo );
    }

}
