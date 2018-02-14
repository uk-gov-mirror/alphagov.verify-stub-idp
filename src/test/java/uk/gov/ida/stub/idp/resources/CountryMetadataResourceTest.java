package uk.gov.ida.stub.idp.resources;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.squarespace.jersey2.guice.JerseyGuiceUtils;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.security.SecurityException;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.w3c.dom.Document;

import uk.gov.ida.saml.core.IdaSamlBootstrap;
import uk.gov.ida.saml.security.IdaKeyStore;
import uk.gov.ida.stub.idp.builders.CountryMetadataBuilder;

@RunWith(MockitoJUnitRunner.class)
public class CountryMetadataResourceTest {

    @BeforeClass
    public static void doALittleHackToMakeGuicierHappyForSomeReason() {
        JerseyGuiceUtils.reset();
    }

    private CountryMetadataResource resource;
    private static final String VALID_COUNTRY = "stub-country-one";
    private EntityDescriptor entityDescriptor;
    private URI validCountryUri;

    @Mock
    private X509Certificate signingCertificate;

    @Mock
    private IdaKeyStore idaKeyStore;

    @Mock
    private CountryMetadataBuilder countryMetadataBuilder;

    @BeforeClass
    public static void classSetUp() {
        IdaSamlBootstrap.bootstrap();
    }

    @Before
    public void setUp() throws CertificateEncodingException, MarshallingException, SecurityException, SignatureException, URISyntaxException {
        validCountryUri = new URI(String.format("https://stub.test/%s/ServiceMetadata", VALID_COUNTRY));
        resource = new CountryMetadataResource(idaKeyStore, countryMetadataBuilder);
        entityDescriptor = (EntityDescriptor) XMLObjectProviderRegistrySupport.getBuilderFactory()
          .getBuilder(EntityDescriptor.DEFAULT_ELEMENT_NAME).buildObject(EntityDescriptor.DEFAULT_ELEMENT_NAME, EntityDescriptor.TYPE_NAME);
        when(idaKeyStore.getSigningCertificate()).thenReturn(signingCertificate);
        when(countryMetadataBuilder.createEntityDescriptorForProxyNodeService(any(), any(), any(), any())).thenReturn(entityDescriptor);;
    }

    @Test
    public void getShouldReturnADocumentWhenIdpIsKnown() throws URISyntaxException, SecurityException, CertificateEncodingException, SignatureException, MarshallingException {
        final UriInfo requestContext = mock(UriInfo.class);
        when(requestContext.getAbsolutePath()).thenReturn(validCountryUri);
        when(requestContext.getBaseUri()).thenReturn(new URI("https://stub.test"));

        final Response response = resource.getMetadata(requestContext, VALID_COUNTRY);

        URI validCountrySsoUri = new URI(String.format("https://stub.test/eidas/%s/SAML2/SSO", VALID_COUNTRY));
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(Document.class).isAssignableFrom(response.getEntity().getClass());
        verify(countryMetadataBuilder).createEntityDescriptorForProxyNodeService(eq(validCountryUri), eq(validCountrySsoUri), eq(signingCertificate), any());
    }

    @Test
    public void getShouldReturnNotFoundWhenIdpIsNullOrEmpty() throws CertificateEncodingException, MarshallingException, SecurityException, SignatureException {
        final UriInfo requestContext = mock(UriInfo.class);

        Response response = resource.getMetadata(requestContext, null);
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        assertThat(response.getEntity()).isEqualTo(null);

        response = resource.getMetadata(requestContext, "");
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        assertThat(response.getEntity()).isEqualTo(null);

        verify(countryMetadataBuilder, times(0)).createEntityDescriptorForProxyNodeService(any(), any(), any(), any());
    }
}