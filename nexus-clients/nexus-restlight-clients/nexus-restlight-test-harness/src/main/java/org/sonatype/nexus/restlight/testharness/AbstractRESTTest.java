package org.sonatype.nexus.restlight.testharness;

import static junit.framework.Assert.fail;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class AbstractRESTTest
{

    private static final String TEST_NX_API_VERSION_SYSPROP = "test.nexus.api.version";

    private static final String DEFAULT_TEST_NX_API_VERSION = "1.3.1";

    /**
     * The test fixture MUST NOT change during the test.
     */
    protected abstract RESTTestFixture getTestFixture();

    protected final String getBaseUrl()
    {
        return "http://127.0.0.1:" + getTestFixture().getPort();
    }
    
    protected final Document readTestDocumentResource( String resourcePath )
        throws JDOMException, IOException
    {
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream( resourcePath );
        if ( stream == null )
        {
            fail( "Cannot find test resource: '" + resourcePath + "'" );
        }

        try
        {
            return new SAXBuilder().build( stream );
        }
        finally
        {
            if ( stream != null )
            {
                try
                {
                    stream.close();
                }
                catch ( IOException e )
                {
                    System.out.println( "Failed to close stream to classpath resource: " + resourcePath );
                }
            }
        }
    }

    protected final Document readTestDocumentFile( File file )
        throws JDOMException, IOException
    {
        FileInputStream stream = null;
        try
        {
            stream = new FileInputStream( file );
            
            return new SAXBuilder().build( stream );
        }
        finally
        {
            if ( stream != null )
            {
                try
                {
                    stream.close();
                }
                catch ( IOException e )
                {
                    System.out.println( "Failed to close stream to file: " + file );
                }
            }
        }
    }

    protected final GETFixture getVersionCheckFixture()
    {
        GETFixture fixture = new GETFixture();

        Document doc = new Document().setRootElement( new Element( "status" ) );
        Element data = new Element( "data" );

        data.addContent( new Element( "apiVersion" ).setText( getTestNexusAPIVersion() ) );
        doc.getRootElement().addContent( data );

        fixture.setExactURI( "/service/local/status" );
        fixture.setResponseDocument( doc );

        return fixture;
    }

    protected String getTestNexusAPIVersion()
    {
        return System.getProperty( TEST_NX_API_VERSION_SYSPROP, DEFAULT_TEST_NX_API_VERSION );
    }

    @Before
    public void startServer()
        throws Exception
    {
        getTestFixture().startServer();
    }

    @After
    public void stopServer()
        throws Exception
    {
        getTestFixture().stopServer();
    }

}
