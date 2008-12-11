package org.sonatype.nexus.proxy.maven.maven2;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.nexus.artifact.M2ArtifactRecognizer;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.RepositoryNotAvailableException;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.AbstractGroupRepository;
import org.sonatype.nexus.proxy.repository.GroupRepository;

@Component( role = GroupRepository.class, hint = "maven2", instantiationStrategy = "per-lookup" )
public class M2GroupRepository
    extends AbstractGroupRepository
{

    @Requirement( hint = "maven2" )
    private ContentClass contentClass;

    public ContentClass getRepositoryContentClass()
    {
        return contentClass;
    }

    @Override
    protected StorageItem doRetrieveItem( boolean localOnly, RepositoryItemUid uid, Map<String, Object> context )
        throws RepositoryNotAvailableException,
            ItemNotFoundException,
            StorageException
    {
        if ( M2ArtifactRecognizer.isMetadata( uid.getPath() ) && !M2ArtifactRecognizer.isChecksum( uid.getPath() ) )
        {
            //metadata checksum files are calculate and cached as side-effect
            //of doRetrieveMetadata.

            return doRetriveMetadata( localOnly, uid, context );
        }

        return super.doRetrieveItem( localOnly, uid, context );
    }

    /**
     * Aggregates metadata from all member repositories
     */
    private StorageItem doRetriveMetadata( boolean localOnly, RepositoryItemUid uid, Map<String, Object> context ) throws StorageException
    {
        MetadataXpp3Reader metadataReader = new MetadataXpp3Reader();
        MetadataXpp3Writer metadataWriter = new MetadataXpp3Writer();
        InputStreamReader isr = null;

        Metadata mergedMetadata = null;

        List<StorageItem> listOfStorageItems = doRetrieveItems( localOnly, uid, context );

        // Reversing the result list, so that the most authoritative result
        // will provide fields like lastVersiion
        Collections.reverse( listOfStorageItems );

        for ( StorageItem currentItem : listOfStorageItems )
        {
            try
            {
                DefaultStorageFileItem currentFileItem = (DefaultStorageFileItem) currentItem;
                isr = new InputStreamReader( currentFileItem.getInputStream() );
                Metadata imd = metadataReader.read( isr );
                if ( mergedMetadata == null )
                {
                    mergedMetadata = imd;
                }
                else
                {
                    mergedMetadata.merge( imd );
                }
            }
            catch ( Exception ex )
            {
                getLogger().warn(
                    "Got Exception during merge of M2 metadata: " + currentItem.getRepositoryItemUid(),
                    ex );
            }
            finally
            {
                if ( isr != null )
                {
                    try
                    {
                        isr.close();
                    }
                    catch ( IOException e )
                    {
                        getLogger().warn( "Got IO exception during close of InputStream.", e );
                    }
                }
            }
        }

        try
        {
            // we are not saving the merged metadata, just calculating
            // correct
            // checksum for later retrieval
            // and sending it back with in-memory stream
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            MessageDigest md5alg = MessageDigest.getInstance( "md5" );
            MessageDigest sha1alg = MessageDigest.getInstance( "sha1" );
            OutputStreamWriter osw = new OutputStreamWriter( new DigestOutputStream( new DigestOutputStream(
                bos,
                md5alg ), sha1alg ) );
            metadataWriter.write( osw, mergedMetadata );
            osw.flush();
            osw.close();
            storeDigest( uid, md5alg );
            storeDigest( uid, sha1alg );

            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug(
                    "Item for path " + uid.getPath() + " merged from "
                        + Integer.toString( listOfStorageItems.size() ) + " found items." );
            }

            return createStorageItem( uid, bos.toByteArray() );
        }
        catch ( NoSuchAlgorithmException ex )
        {
            throw new StorageException( "Got NoSuchAlgorithmException during M2 metadata merging.", ex );
        }
        catch ( IOException ex )
        {
            throw new StorageException( "Got IOException during M2 metadata merging.", ex );
        }
    }

    protected void storeDigest( RepositoryItemUid uid, MessageDigest digest )
        throws IOException
    {
        File tmpFile = new File( getApplicationConfiguration().getTemporaryDirectory(),
            uid.getPath().replace( RepositoryItemUid.PATH_SEPARATOR.charAt( 0 ), '_' )
            + "." + digest.getAlgorithm().toLowerCase() );

        tmpFile.deleteOnExit();

        FileWriter fw = null;

        try
        {
            fw = new FileWriter( tmpFile );

            fw.write( new String( Hex.encodeHex( digest.digest() ) ) + "\n" );

            fw.flush();
        }
        finally
        {
            fw.close();
        }
    }

}

