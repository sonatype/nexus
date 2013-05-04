package org.sonatype.nexus.client.rest.jersey;

import com.sun.jersey.core.provider.AbstractMessageReaderWriterProvider;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static org.codehaus.jackson.map.DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.codehaus.jackson.map.SerializationConfig.Feature.INDENT_OUTPUT;
import static org.codehaus.jackson.map.SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS;

/**
 * Jackson JSON provider which uses custom {@link #MEDIA_TYPE}.
 *
 * @since 2.5
 */
@Consumes({JacksonProvider.MEDIA_TYPE_NAME})
@Produces({JacksonProvider.MEDIA_TYPE_NAME})
public class JacksonProvider
    extends AbstractMessageReaderWriterProvider<Object>
{
    public static final String MEDIA_TYPE_NAME = "application/vnd.json-jackson";

    public static final MediaType MEDIA_TYPE = MediaType.valueOf(MEDIA_TYPE_NAME);

    private final ObjectMapper mapper;

    public JacksonProvider(final ObjectMapper mapper) {
        this.mapper = mapper == null ? createObjectMapper() : mapper;
    }

    public JacksonProvider() {
        this(null);
    }

    protected ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();

        DeserializationConfig dconfig = mapper.getDeserializationConfig();
        SerializationConfig sconfig = mapper.getSerializationConfig();

        // Configure Jackson annotations only, JAXB annotations can confuse and produce improper content
        dconfig.withAnnotationIntrospector(new JacksonAnnotationIntrospector());
        sconfig.withAnnotationIntrospector(new JacksonAnnotationIntrospector());

        // Do not include null values
        sconfig.withSerializationInclusion(Inclusion.NON_NULL);

        // Write dates as ISO-8601
        mapper.configure(WRITE_DATES_AS_TIMESTAMPS, false);

        // Make the output look more readable
        mapper.configure(INDENT_OUTPUT, true);

        // Ignore unknown data
        mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

        return mapper;
    }

    @Override
    public boolean isReadable(final Class<?> type,
                              final Type genericType,
                              final Annotation[] annotations,
                              final MediaType mediaType)
    {
        return MEDIA_TYPE.isCompatible(mediaType);
    }

    @Override
    public Object readFrom(final Class<Object> type,
                           final Type genericType,
                           final Annotation[] annotations,
                           final MediaType mediaType,
                           final MultivaluedMap<String, String> httpHeaders,
                           final InputStream entityStream)
        throws IOException, WebApplicationException
    {
        return mapper.readValue(entityStream, type);
    }

    @Override
    public boolean isWriteable(final Class<?> type,
                               final Type genericType,
                               final Annotation[] annotations,
                               final MediaType mediaType)
    {
        return MEDIA_TYPE.isCompatible(mediaType);
    }

    @Override
    public void writeTo(final Object o,
                        final Class<?> type,
                        final Type genericType,
                        final Annotation[] annotations,
                        final MediaType mediaType,
                        final MultivaluedMap<String, Object> httpHeaders,
                        final OutputStream entityStream)
        throws IOException, WebApplicationException
    {
        mapper.writeValue(entityStream, o);
    }
}
