package org.sonatype.nexus.restlight.m2settings;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.SimpleLayout;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.sonatype.nexus.restlight.common.AbstractSimpleRESTClient;
import org.sonatype.nexus.restlight.common.SimpleRESTClientException;

import java.io.IOException;


public class M2SettingsClient
    extends AbstractSimpleRESTClient
{

    public static final String SETTINGS_TEMPLATE_BASE = SVC_BASE + "/templates/settings/";
    public static final String GET_CONTENT_ACTION = "/content";

    public M2SettingsClient( String baseUrl, String user, String password )
        throws SimpleRESTClientException
    {
        super( baseUrl, user, password );
    }

    public Document getSettingsTemplateAbsolute( String url )
        throws SimpleRESTClientException
    {
        return getAbsolute( url );
    }

    public Document getSettingsTemplate( String templateName )
        throws SimpleRESTClientException
    {
        return get( SETTINGS_TEMPLATE_BASE + templateName + GET_CONTENT_ACTION );
    }

    public static void main( String[] args )
        throws IOException, JDOMException
    {
        LogManager.getRootLogger().setLevel( Level.DEBUG );
        LogManager.getRootLogger().addAppender( new ConsoleAppender( new SimpleLayout() ) );

        String base = "http://localhost:8082/nexus";
        String url = base + "/service/local/templates/settings/foo";
        // String base = "https://damian.testing.sonatype.org/nexus";
        try
        {
            M2SettingsClient client = new M2SettingsClient( base, "admin", "admin123" );

            Document doc = client.getSettingsTemplateAbsolute( url );

            System.out.println( new XMLOutputter( Format.getPrettyFormat() ).outputString( doc ) );
        }
        catch ( SimpleRESTClientException e )
        {
            e.printStackTrace();
        }
    }
}
