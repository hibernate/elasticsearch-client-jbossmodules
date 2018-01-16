package org.hibernate.elasticsearch.client.jbossmodules;

import com.google.common.base.Joiner;
import org.apache.http.HttpHost;
import org.apache.http.entity.StringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.json.JSONException;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Elasticsearch Rest Client module testing inside Wildfly
 */
@RunWith(Arquillian.class)
public class ElasticsearchRestClientModuleIT {

	private static final VersionsHelper helper = new VersionsHelper();

	private static final int NODE_1_PORT = 9200;

	@Deployment
	public static Archive<?> deployment() {
		String dependencies = Joiner.on(", ").join(
				"org.elasticsearch.client.elasticsearch-rest-client:" + helper.getRestClientModuleSlot(),
				"org.apache.httpcomponents:main"
		);
		StringAsset manifest = new StringAsset(Descriptors.create(ManifestDescriptor.class)
				.attribute("Dependencies", dependencies)
				.exportAsString());
		return ShrinkWrap.create(WebArchive.class, ElasticsearchRestClientModuleIT.class.getSimpleName() + ".war")
				.addClasses(ElasticsearchRestClientModuleIT.class, VersionsHelper.class)
				.addAsLibraries(Maven.resolver()
						.resolve("org.skyscreamer:jsonassert:" + helper.getJsonAssertVersion())
						.withTransitivity()
						.as(JavaArchive.class))
				.add(manifest, "META-INF/MANIFEST.MF");
	}

	private RestClient restClient;

	@Rule
	public TemporaryFolder tmpFolder = new TemporaryFolder();

	@Before
	public void setup() {
		restClient = RestClient.builder(new HttpHost("localhost", NODE_1_PORT)).build();
	}

	@After
	public void tearDown() throws IOException {
		restClient.close();
	}

	@Test
	public void createThenGet() throws IOException, JSONException {
		String endpoint = "/myindex/mytype/1";
		String document = "{\"someField\": \"someValue\"}";

		Map<String, String> params = new HashMap<>();
		params.put("refresh", "true");
		Response response = restClient.performRequest(
				"PUT", endpoint, params, new StringEntity(document)
		);

		Assert.assertEquals(201, response.getStatusLine().getStatusCode());

		response = restClient.performRequest("GET", endpoint);
		Assert.assertEquals(200, response.getStatusLine().getStatusCode());
		String responseBody = getBody(response);
		JSONAssert.assertEquals("{\"_id\":\"1\",\"_source\":" + document + "}",
				responseBody, JSONCompareMode.LENIENT);
	}

	private String getBody(Response response) throws IOException {
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			response.getEntity().writeTo(outputStream);
			return outputStream.toString(StandardCharsets.UTF_8.name());
		}
	}

}
