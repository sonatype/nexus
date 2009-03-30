package org.sonatype.nexus.restlight.stage;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.SimpleLayout;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Text;
import org.jdom.xpath.XPath;
import org.sonatype.nexus.restlight.common.AbstractSimpleRESTClient;
import org.sonatype.nexus.restlight.common.SimpleRESTClientException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class StageClient
    extends AbstractSimpleRESTClient
{

    public static final String PROFILES_PATH = SVC_BASE + "/staging/profiles";

    public static final String PROFILE_REPOS_PATH_PREFIX = SVC_BASE + "/staging/profile_repositories/";

    public static final String STAGE_REPO_FINISH_ACTION = "/finish";

    public static final String STAGE_REPO_DROP_ACTION = "/drop";

    public static final String STAGE_REPO_PROMOTE_ACTION = "/promote";

    private static final String STAGE_REPO_ID_PARAM = "stagedRepositoryId";

    private static final String PROFILE_ID_ELEMENT = "id";

    private static final String REPO_ID_ELEMENT = "repositoryId";

    private static final String REPO_URI_ELEMENT = "repositoryURI";

    private static final String USER_ID_ELEMENT = "userId";

    private static final String OPEN_STAGE_REPOS_XPATH = "//stagingRepositoryIds/string/text()";

    private static final String CLOSED_STAGE_REPOS_XPATH = "//stagedRepositoryIds/string/text()";

    private static final String STAGE_REPO_LIST_XPATH = "//stagingProfile";

    private static final String STAGE_REPO_XPATH = "//stagingProfile";

    private static final String STAGE_REPO_DETAIL_XPATH = "//stagingProfileRepository";

    public StageClient( String baseUrl, String user, String password )
        throws SimpleRESTClientException
    {
        super( baseUrl, user, password );
    }

    public List<StageRepository> getOpenStageRepositoriesForUser()
        throws SimpleRESTClientException
    {
        Document doc = get( PROFILES_PATH );

        return parseStageRepositories( doc, STAGE_REPO_LIST_XPATH, true );
    }

    public StageRepository getOpenStageRepositoryForUser( String groupId, String artifactId, String version )
        throws SimpleRESTClientException
    {
        Map<String, String> params = new HashMap<String, String>();
        mapCoord( groupId, artifactId, version, params );

        Document doc = get( PROFILES_PATH, params );

        List<StageRepository> ids = parseStageRepositories( doc, STAGE_REPO_XPATH, true );
        if ( ids == null || ids.isEmpty() )
        {
            return null;
        }
        else
        {
            return ids.get( 0 );
        }
    }

    public List<StageRepository> getClosedStageRepositoriesForUser()
        throws SimpleRESTClientException
    {
        Document doc = get( PROFILES_PATH );

        return parseStageRepositories( doc, STAGE_REPO_LIST_XPATH, false );
    }

    public List<StageRepository> getClosedStageRepositoriesForUser( String groupId, String artifactId, String version )
        throws SimpleRESTClientException
    {
        Map<String, String> params = new HashMap<String, String>();
        mapCoord( groupId, artifactId, version, params );

        Document doc = get( PROFILES_PATH, params );

        return parseStageRepositories( doc, STAGE_REPO_XPATH, false );
    }

    @SuppressWarnings( "unchecked" )
    private List<StageRepository> parseStageRepositories( Document doc, String profileXpath, Boolean findOpen )
        throws SimpleRESTClientException
    {
        // System.out.println( new XMLOutputter().outputString( doc ) );

        XPath profileXp = newXPath( profileXpath );

        List<Element> profiles;
        try
        {
            profiles = (List<Element>) profileXp.selectNodes( doc.getRootElement() );
        }
        catch ( JDOMException e )
        {
            throw new SimpleRESTClientException( "XPath selection failed: '" + profileXpath + "' (Root node: "
                + doc.getRootElement().getName() + ").", e );
        }

        List<StageRepository> result = new ArrayList<StageRepository>();
        if ( profiles != null )
        {

            XPath openRepoIdXPath = newXPath( OPEN_STAGE_REPOS_XPATH );
            XPath closedRepoIdXPath = newXPath( CLOSED_STAGE_REPOS_XPATH );

            for ( Element profile : profiles )
            {
                // System.out.println( new XMLOutputter().outputString( profile ) );

                String profileId = profile.getChild( PROFILE_ID_ELEMENT ).getText();
                Map<String, StageRepository> matchingRepoStubs = new LinkedHashMap<String, StageRepository>();

                if ( !Boolean.FALSE.equals( findOpen ) )
                {
                    try
                    {
                        List<Text> repoIds = openRepoIdXPath.selectNodes( profile );
                        if ( repoIds != null && !repoIds.isEmpty() )
                        {
                            for ( Text txt : repoIds )
                            {
                                matchingRepoStubs.put( profileId + "/" + txt.getText(),
                                                       new StageRepository( profileId, txt.getText(), findOpen ) );
                            }
                        }
                    }
                    catch ( JDOMException e )
                    {
                        throw new SimpleRESTClientException( "XPath selection failed: '" + OPEN_STAGE_REPOS_XPATH
                            + "' (Node: " + profile.getName() + ").", e );
                    }
                }

                if ( !Boolean.TRUE.equals( findOpen ) )
                {
                    try
                    {
                        List<Text> repoIds = closedRepoIdXPath.selectNodes( profile );
                        if ( repoIds != null && !repoIds.isEmpty() )
                        {
                            for ( Text txt : repoIds )
                            {
                                matchingRepoStubs.put( profileId + "/" + txt.getText(),
                                                       new StageRepository( profileId, txt.getText(), findOpen ) );
                            }
                        }
                    }
                    catch ( JDOMException e )
                    {
                        throw new SimpleRESTClientException( "XPath selection failed: '" + CLOSED_STAGE_REPOS_XPATH
                            + "' (Node: " + profile.getName() + ").", e );
                    }
                }

                if ( !matchingRepoStubs.isEmpty() )
                {
                    parseStageRepositoryDetails( profileId, matchingRepoStubs );

                    result.addAll( matchingRepoStubs.values() );
                }
            }
        }

        return result;
    }

    @SuppressWarnings( "unchecked" )
    private void parseStageRepositoryDetails( String profileId, Map<String, StageRepository> repoStubs )
        throws SimpleRESTClientException
    {
        // System.out.println( repoStubs );

        Document doc = get( PROFILE_REPOS_PATH_PREFIX + profileId );

        // System.out.println( new XMLOutputter().outputString( doc ) );

        XPath repoXPath = newXPath( STAGE_REPO_DETAIL_XPATH );

        List<Element> repoDetails;
        try
        {
            repoDetails = repoXPath.selectNodes( doc.getRootElement() );
        }
        catch ( JDOMException e )
        {
            throw new SimpleRESTClientException( "Failed to select detail sections for staging-profile repositories.",
                                                 e );
        }

        if ( repoDetails != null && !repoDetails.isEmpty() )
        {
            for ( Element detail : repoDetails )
            {
                String repoId = detail.getChild( REPO_ID_ELEMENT ).getText();
                String key = profileId + "/" + repoId;
                
                StageRepository repo = repoStubs.get( key );

                if ( repo == null )
                {
                    continue;
                }
                
                Element uid = detail.getChild( USER_ID_ELEMENT );
                if ( uid != null && getUser() != null && getUser().equals( uid.getText().trim() ) )
                {
                    repo.setUser( uid.getText().trim() );
                }
                else
                {
                    repoStubs.remove( key );
                }
                
                Element url = detail.getChild( REPO_URI_ELEMENT );
                if ( url != null )
                {
                    repo.setUrl( url.getText() );
                }
            }
        }
    }

    private XPath newXPath( String xpath )
        throws SimpleRESTClientException
    {
        try
        {
            return XPath.newInstance( xpath );
        }
        catch ( JDOMException e )
        {
            throw new SimpleRESTClientException( "Failed to build xpath: '" + xpath + "'.", e );
        }
    }

    public void finishRepositoryForUser( String groupId, String artifactId, String version )
        throws SimpleRESTClientException
    {
        StageRepository repo = getOpenStageRepositoryForUser( groupId, artifactId, version );

        finishRepository( repo );
    }

    public void finishRepository( StageRepository repo )
        throws SimpleRESTClientException
    {
        performStagingAction( repo, STAGE_REPO_FINISH_ACTION, null );
    }

    public void dropRepositoryForUser( String groupId, String artifactId, String version )
        throws SimpleRESTClientException
    {
        StageRepository repo = getOpenStageRepositoryForUser( groupId, artifactId, version );

        dropRepository( repo );
    }

    public void dropRepository( StageRepository repo )
        throws SimpleRESTClientException
    {
        performStagingAction( repo, STAGE_REPO_DROP_ACTION, null );
    }

    public void promoteRepositoryForUser( String groupId, String artifactId, String version, String targetRepositoryId )
        throws SimpleRESTClientException
    {
        StageRepository repo = getOpenStageRepositoryForUser( groupId, artifactId, version );

        promoteRepository( repo, targetRepositoryId );
    }

    public void promoteRepository( StageRepository repo, String targetRepositoryId )
        throws SimpleRESTClientException
    {
        Element target = new Element( "targetRepositoryId" ).setText( targetRepositoryId );
        
        performStagingAction( repo, STAGE_REPO_PROMOTE_ACTION, Collections.singletonList( target ) );
    }

    private void performStagingAction( StageRepository repo, String actionSubpath, List<Element> extraData )
        throws SimpleRESTClientException
    {
        Map<String, String> params = new HashMap<String, String>();
        params.put( STAGE_REPO_ID_PARAM, repo.getRepositoryId() );

        String rootElement = getVocabulary().getProperty( VocabularyKeys.PROMOTE_STAGE_REPO_ROOT_ELEMENT );
        Document body = new Document().setRootElement( new Element( rootElement ) );

        Element data = new Element( "data" );
        body.getRootElement().addContent( data );
        
        data.addContent( new Element( "stagedRepositoryId" ).setText( repo.getRepositoryId() ) );

        if ( extraData != null && !extraData.isEmpty() )
        {
            for ( Element extra : extraData )
            {
                data.addContent( extra );
            }
        }

        post( PROFILES_PATH + "/" + repo.getProfileId() + actionSubpath, null, body );
    }

    public static void main( String[] args )
        throws IOException, JDOMException
    {
        LogManager.getRootLogger().setLevel( Level.DEBUG );
        LogManager.getRootLogger().addAppender( new ConsoleAppender( new SimpleLayout() ) );

        String base = "http://localhost:8082/nexus";
        // String base = "https://damian.testing.sonatype.org/nexus";
        try
        {
//            StageClient client = new StageClient( base, "admin", "admin123" );
            StageClient client = new StageClient( base, "testuser", "admin123" );

            String apiVersion = client.getApiVersion();
            System.out.println( "API Version: " + apiVersion );

//            List<StageRepository> all = client.getOpenStageRepositoriesForUser();
//            System.out.println( "ALL OPEN:\n\n" + all + "\n\n" );
//
//            all = client.getClosedStageRepositories();
//            System.out.println( "ALL CLOSED:\n\n" + all + "\n\n" );
//            
//            if ( all != null && !all.isEmpty() )
//            {
//                System.out.println( "Dropping pre-existing closed repos." );
//                
//                for ( StageRepository repo : all )
//                {
//                    client.dropRepository( repo );
//                }
//            }
//
            String groupId = "org.test";
            String artifactId = "test-project";
            String version = "3";

            StageRepository openByGAV = client.getOpenStageRepositoryForUser( groupId, artifactId, version );
            System.out.println( "OPEN:" + openByGAV + "\n\n" );
//
//            List<StageRepository> closedByGAV = client.getClosedStageRepositories( groupId, artifactId, version );
//            System.out.println( "CLOSED:\n\n" + closedByGAV + "\n\n" );
//
            if ( openByGAV != null )
            {
                client.finishRepository( openByGAV );
            }
//
//            openByGAV = client.getOpenStageRepository( groupId, artifactId, version );
//            System.out.println( "OPEN:" + openByGAV + "\n\n" );
//
//            closedByGAV = client.getClosedStageRepositories( groupId, artifactId, version );
//            System.out.println( "CLOSED:\n\n" + closedByGAV + "\n\n" );
//            
//            if ( closedByGAV != null && !closedByGAV.isEmpty() )
//            {
//                System.out.println( "Promoting freshly closed repos to repository 'releases'." );
//                
//                for ( StageRepository repo : closedByGAV )
//                {
//                    client.promoteRepository( repo, "releases" );
//                }
//            }
        }
        catch ( SimpleRESTClientException e )
        {
            e.printStackTrace();
        }
    }

}
