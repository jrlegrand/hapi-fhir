package ca.uhn.fhir.rest.server;

import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.base.resource.BaseOperationOutcome;
import ca.uhn.fhir.model.dstu2.resource.OperationOutcome;
import ca.uhn.fhir.model.dstu2.resource.Organization;
import ca.uhn.fhir.model.dstu2.resource.Parameters;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Validate;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.ValidationModeEnum;
import ca.uhn.fhir.util.PortUtil;

/**
 * Created by dsotnikov on 2/25/2014.
 */
public class ValidateDstu2Test {
	private static CloseableHttpClient ourClient;
	private static EncodingEnum ourLastEncoding;
	private static String ourLastResourceBody;
	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(ValidateDstu2Test.class);
	private static int ourPort;
	private static Server ourServer;
	private static BaseOperationOutcome ourOutcomeToReturn;
	private static ValidationModeEnum ourLastMode;
	private static String ourLastProfile;

	@Before()
	public void before() {
		ourLastResourceBody = null;
		ourLastEncoding = null;
		ourOutcomeToReturn = null;
		ourLastMode = null;
		ourLastProfile = null;
	}

	@Test
	public void testValidateWithOptions() throws Exception {

		Patient patient = new Patient();
		patient.addIdentifier().setValue("001");
		patient.addIdentifier().setValue("002");

		Parameters params = new Parameters();
		params.addParameter().setName("resource").setResource(patient);
		params.addParameter().setName("profile").setValue(new StringDt("http://foo"));
		params.addParameter().setName("mode").setValue(new StringDt(ValidationModeEnum.CREATE.name()));

		HttpPost httpPost = new HttpPost("http://localhost:" + ourPort + "/Patient/$validate");
		httpPost.setEntity(new StringEntity(new FhirContext().newXmlParser().encodeResourceToString(params), ContentType.create(Constants.CT_FHIR_XML, "UTF-8")));

		HttpResponse status = ourClient.execute(httpPost);
		String resp = IOUtils.toString(status.getEntity().getContent());
		IOUtils.closeQuietly(status.getEntity().getContent());

		assertEquals(200, status.getStatusLine().getStatusCode());

		assertThat(resp, stringContainsInOrder("<OperationOutcome"));
		assertEquals("http://foo", ourLastProfile);
		assertEquals(ValidationModeEnum.CREATE, ourLastMode);
	}

	@Test
	public void testValidate() throws Exception {

		Patient patient = new Patient();
		patient.addIdentifier().setValue("001");
		patient.addIdentifier().setValue("002");

		Parameters params = new Parameters();
		params.addParameter().setName("resource").setResource(patient);

		HttpPost httpPost = new HttpPost("http://localhost:" + ourPort + "/Patient/$validate");
		httpPost.setEntity(new StringEntity(new FhirContext().newXmlParser().encodeResourceToString(params), ContentType.create(Constants.CT_FHIR_XML, "UTF-8")));

		HttpResponse status = ourClient.execute(httpPost);
		String resp = IOUtils.toString(status.getEntity().getContent());
		IOUtils.closeQuietly(status.getEntity().getContent());

		assertEquals(200, status.getStatusLine().getStatusCode());

		assertThat(resp, stringContainsInOrder("<OperationOutcome"));
	}

	@Test
	public void testValidateWithResults() throws Exception {

		ourOutcomeToReturn = new OperationOutcome();
		ourOutcomeToReturn.addIssue().setDetails("FOOBAR");

		Patient patient = new Patient();
		patient.addIdentifier().setValue("001");
		patient.addIdentifier().setValue("002");

		Parameters params = new Parameters();
		params.addParameter().setName("resource").setResource(patient);

		HttpPost httpPost = new HttpPost("http://localhost:" + ourPort + "/Patient/$validate");
		httpPost.setEntity(new StringEntity(new FhirContext().newXmlParser().encodeResourceToString(params), ContentType.create(Constants.CT_FHIR_XML, "UTF-8")));

		HttpResponse status = ourClient.execute(httpPost);
		String resp = IOUtils.toString(status.getEntity().getContent());
		IOUtils.closeQuietly(status.getEntity().getContent());

		assertEquals(200, status.getStatusLine().getStatusCode());

		assertThat(resp, stringContainsInOrder("<OperationOutcome", "FOOBAR"));
	}

	@Test
	public void testValidateWithNoParsed() throws Exception {

		Organization org = new Organization();
		org.addIdentifier().setValue("001");
		org.addIdentifier().setValue("002");

		Parameters params = new Parameters();
		params.addParameter().setName("resource").setResource(org);

		HttpPost httpPost = new HttpPost("http://localhost:" + ourPort + "/Organization/$validate");
		httpPost.setEntity(new StringEntity(new FhirContext().newJsonParser().encodeResourceToString(params), ContentType.create(Constants.CT_FHIR_JSON, "UTF-8")));

		HttpResponse status = ourClient.execute(httpPost);
		assertEquals(200, status.getStatusLine().getStatusCode());

		assertThat(ourLastResourceBody, stringContainsInOrder("\"resourceType\":\"Organization\"", "\"identifier\"", "\"value\":\"001"));
		assertEquals(EncodingEnum.JSON, ourLastEncoding);

	}

	@AfterClass
	public static void afterClass() throws Exception {
		ourServer.stop();
	}

	@BeforeClass
	public static void beforeClass() throws Exception {
		ourPort = PortUtil.findFreePort();
		ourServer = new Server(ourPort);

		PatientProvider patientProvider = new PatientProvider();

		ServletHandler proxyHandler = new ServletHandler();
		RestfulServer servlet = new RestfulServer();
		servlet.setResourceProviders(patientProvider, new OrganizationProvider());
		ServletHolder servletHolder = new ServletHolder(servlet);
		proxyHandler.addServletWithMapping(servletHolder, "/*");
		ourServer.setHandler(proxyHandler);
		ourServer.start();

		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(5000, TimeUnit.MILLISECONDS);
		HttpClientBuilder builder = HttpClientBuilder.create();
		builder.setConnectionManager(connectionManager);
		ourClient = builder.build();

	}

	public static class PatientProvider implements IResourceProvider {

		@Validate()
		public MethodOutcome validatePatient(@ResourceParam Patient thePatient, @Validate.Mode ValidationModeEnum theMode, @Validate.Profile String theProfile) {
			IdDt id = new IdDt(thePatient.getIdentifier().get(0).getValue());
			if (thePatient.getId().isEmpty() == false) {
				id = thePatient.getId();
			}

			ourLastMode = theMode;
			ourLastProfile = theProfile;

			MethodOutcome outcome = new MethodOutcome(id.withVersion("002"));
			outcome.setOperationOutcome(ourOutcomeToReturn);
			return outcome;
		}

		@Override
		public Class<? extends IResource> getResourceType() {
			return Patient.class;
		}

	}

	public static class OrganizationProvider implements IResourceProvider {

		@Validate()
		public MethodOutcome validate(@ResourceParam String theResourceBody, @ResourceParam EncodingEnum theEncoding) {
			ourLastResourceBody = theResourceBody;
			ourLastEncoding = theEncoding;

			return new MethodOutcome(new IdDt("001"));
		}

		@Override
		public Class<? extends IResource> getResourceType() {
			return Organization.class;
		}

	}

}
