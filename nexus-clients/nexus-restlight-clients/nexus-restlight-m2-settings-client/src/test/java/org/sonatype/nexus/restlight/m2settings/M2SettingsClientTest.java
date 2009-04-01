package org.sonatype.nexus.restlight.m2settings;

import static org.junit.Assert.assertEquals;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.Test;
import org.sonatype.nexus.restlight.common.RESTLightClientException;
import org.sonatype.nexus.restlight.m2settings.M2SettingsClient;
import org.sonatype.nexus.restlight.testharness.AbstractRESTTest;
import org.sonatype.nexus.restlight.testharness.ConversationalFixture;
import org.sonatype.nexus.restlight.testharness.GETFixture;
import org.sonatype.nexus.restlight.testharness.RESTTestFixture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;



public class M2SettingsClientTest
    extends AbstractRESTTest
{
    
    private ConversationalFixture fixture = new ConversationalFixture();
    
    @Test
    public void getSettingsTemplateUsingToken()
        throws RESTLightClientException, JDOMException, IOException
    {
        List<RESTTestFixture> conversation = new ArrayList<RESTTestFixture>();
        
        conversation.add( getVersionCheckFixture() );
        
        String token = "testToken";
        
        GETFixture settingsGet = new GETFixture();
        settingsGet.setExactURI( M2SettingsClient.SETTINGS_TEMPLATE_BASE + token + M2SettingsClient.GET_CONTENT_ACTION );
        
        Document testDoc = readTestDocumentResource( "settings-template-" + token + ".xml" );
        
        settingsGet.setResponseDocument( testDoc );
        
        conversation.add( settingsGet );
        
        fixture.setConversation( conversation );
        
        M2SettingsClient client = new M2SettingsClient( getBaseUrl(), "unused", "unused" );
        
        Document doc = client.getSettingsTemplate( token );
        
        XMLOutputter outputter = new XMLOutputter( Format.getCompactFormat() );
        assertEquals( outputter.outputString( testDoc ), outputter.outputString( doc ) );
    }

    @Test
    public void getSettingsTemplateUsingAbsoluteURL()
        throws RESTLightClientException, JDOMException, IOException
    {
        List<RESTTestFixture> conversation = new ArrayList<RESTTestFixture>();
        
        conversation.add( getVersionCheckFixture() );
        
        String token = "testToken";
        
        GETFixture settingsGet = new GETFixture();
        settingsGet.setExactURI( M2SettingsClient.SETTINGS_TEMPLATE_BASE + token + M2SettingsClient.GET_CONTENT_ACTION );
        
        Document testDoc = readTestDocumentResource( "settings-template-" + token + ".xml" );
        
        settingsGet.setResponseDocument( testDoc );
        
        conversation.add( settingsGet );
        
        fixture.setConversation( conversation );
        
        M2SettingsClient client = new M2SettingsClient( getBaseUrl(), "unused", "unused" );
        
        String url = getBaseUrl() + M2SettingsClient.SETTINGS_TEMPLATE_BASE + token + M2SettingsClient.GET_CONTENT_ACTION;
        Document doc = client.getSettingsTemplateAbsolute( url );
        
        XMLOutputter outputter = new XMLOutputter( Format.getCompactFormat() );
        assertEquals( outputter.outputString( testDoc ), outputter.outputString( doc ) );
    }

    @Override
    protected RESTTestFixture getTestFixture()
    {
        return fixture;
    }

}
