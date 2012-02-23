package org.sonatype.nexus.integrationtests.nxcm3133;

import static org.sonatype.nexus.test.utils.NexusRequestMatchers.isSuccessful;
import static org.sonatype.nexus.test.utils.NexusRequestMatchers.respondsWithStatusCode;

import java.io.IOException;

import org.hamcrest.Matcher;
import org.restlet.data.Response;
import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class NXCM3133AdvancedSecurityPlexusResourceIT
    extends AbstractPrivilegeTest
{
    @DataProvider
    public Object[][] privs()
    {
        return new Object[][] { //
            new Object[] { "threepriv-one" },//
            new Object[] { "threepriv-three" }, //
            new Object[] { "threepriv-two" } //
        };
    }

    @Test( dataProvider = "privs" )
    public void access( String priv )
        throws IOException
    {
        TestContainer.getInstance().getTestContext().setSecureTest( true );

        // no privilege
        check( respondsWithStatusCode( 403 ) );

        // give priv
        addPrivilege( TEST_USER_NAME, priv );
        check( isSuccessful() );
        removePrivilege( TEST_USER_NAME, priv );
        check( respondsWithStatusCode( 403 ) );
    }

    protected void check( Matcher<Response> matcher )
        throws IOException
    {
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        RequestFacade.doGet( "service/local/three_privs", matcher );
    }

}
