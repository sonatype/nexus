package org.sonatype.nexus.util.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @since 2.5
 */
public class DefaultConfigurationHelperTest
{

    public static final Charset CHARSET = Charset.forName( "UTF-8" );

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private ConfigurationHelper configurationHelper;

    @Before
    public void setup()
    {
        configurationHelper = new DefaultConfigurationHelper();
    }

    @Test
    public void testSave()
        throws Exception
    {
        File configurationFile = testFolder.newFile();
        Files.write( "foo".getBytes(), configurationFile );
        assertThat( Files.readFirstLine( configurationFile, CHARSET ), is( "foo" ) );

        configurationHelper.save( "bar", configurationFile, new StringConfigurationWriter() );
        List<File> files = Arrays.asList( testFolder.getRoot().listFiles() );
        assertThat( files.toString(), files, hasSize( 1 ) );
        assertThat( files.toString(), files.get( 0 ), is( configurationFile ) );
        assertThat( Files.readFirstLine( configurationFile, CHARSET ), is( "bar" ) );
    }

    @Test
    public void testSaveWithNoInitialFile()
        throws Exception
    {
        File configurationFile = new File( testFolder.getRoot(), "temp" );
        assertThat( configurationFile.exists(), is( false ) );
        configurationHelper.save( "hello", configurationFile, new StringConfigurationWriter() );
        List<File> files = Arrays.asList( testFolder.getRoot().listFiles() );
        assertThat( files, hasSize( 1 ) );
        assertThat( files.get( 0 ), is( configurationFile ) );
    }

    /**
     * If writing the configuration fails we should have the original file and a (undoubtedly corrupt) temp file.
     */
    @Test
    public void testSaveWithException()
        throws Exception
    {
        File configurationFile = testFolder.newFile( "temp" );
        Files.write( "foo".getBytes(), configurationFile );
        try
        {
            configurationHelper.save( "hello", configurationFile, new ExceptionThrowingConfigurationWriter() );
            fail("Should have thrown IOException");
        }
        catch ( IOException e )
        {
            //expected
        }
        List<File> files = Arrays.asList( testFolder.getRoot().listFiles() );
        assertThat( files.toString(), files, hasSize( 2 ) );
        assertThat( files.toString(), files.get( 0 ), is( configurationFile ) );
        assertThat( Files.readFirstLine( configurationFile, CHARSET ), is( "foo" ) );
        assertThat( files.toString(), files.get( 1 ).getName(), is( configurationFile.getName() + ".tmp" ) );
    }

    /**
     * If writing the configuration fails and there wasn't a file to start with we should just have the (undoubtedly corrupt) temp file.
     */
    @Test
    public void testSaveWithNoInitialFileAndException()
        throws Exception
    {
        File configurationFile = new File( testFolder.getRoot(), "temp" );
        assertThat( configurationFile.exists(), is( false ) );
        try
        {
            configurationHelper.save( "hello", configurationFile, new ExceptionThrowingConfigurationWriter() );
            fail("Should have thrown IOException");
        }
        catch ( IOException e )
        {
            //expected
        }
        List<File> files = Arrays.asList( testFolder.getRoot().listFiles() );
        assertThat( files.toString(), files, hasSize( 1 ) );
        assertThat( files.toString(), files.get( 0 ).getName(), is( configurationFile.getName() + ".tmp" ) );
    }

    /**
     * If writing the configuration and a temp file already exists it should be overwritten and then removed.
     */
    @Test
    public void testSaveWithTempFileAlreadyInPlace()
        throws Exception
    {
        File configurationFile = testFolder.newFile();
        Files.write( "foo".getBytes(), configurationFile );
        File tempFile = testFolder.newFile( configurationFile.getName() + ".tmp" );
        Files.write( "bar".getBytes(), tempFile );

        assertThat( Arrays.asList( testFolder.getRoot().listFiles() ), hasSize( 2 ) );

        configurationHelper.save( "hello", configurationFile, new StringConfigurationWriter() );
        List<File> files = Arrays.asList( testFolder.getRoot().listFiles() );
        assertThat( files.toString(), files, hasSize( 1 ) );
        assertThat( files.toString(), files.get( 0 ), is( configurationFile ) );
        assertThat( Files.readFirstLine( configurationFile, CHARSET ), is( "hello" ) );
    }

    private class ExceptionThrowingConfigurationWriter
        implements ConfigurationWriter<String>
    {

        @Override
        public void write( final Writer fr, final String configuration )
            throws IOException
        {
            throw new IOException( "FAIL" );
        }
    }

    private class StringConfigurationWriter
        implements ConfigurationWriter<String>
    {

        @Override
        public void write( final Writer fr, final String configuration )
            throws IOException
        {
            fr.write( configuration );
        }
    }
}
