package org.hibernate.elasticsearch.client.jbossmodules;

import com.google.common.base.Joiner;
import org.apache.http.HttpHost;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.sniff.Sniffer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Elasticsearch Rest Client module testing inside Wildfly
 */
@RunWith(Arquillian.class)
public class ElasticsearchRestClientSnifferModuleIT {

	private static final VersionsHelper helper = new VersionsHelper();

	private static final int NODE_1_PORT = 9200;
	private static final int NODE_2_PORT = 9201;
	private static final Set<Integer> ALL_NODE_PORTS;

	static {
		Set<Integer> set = new HashSet<>();
		set.add(NODE_1_PORT);
		set.add(NODE_2_PORT);
		ALL_NODE_PORTS = Collections.unmodifiableSet(set);
	}

	private static final int TEST_TIME_LIMIT_MS = 10_000;
	private static final int TEST_STEP_SLEEP_MS = 500;

	// Must be small compared to TEST_TIME_LIMIT_MS
	private static final int SNIFF_INTERVAL_MS = 100;

	@Deployment
	public static Archive<?> deployment() {
		String dependencies = Joiner.on(", ").join(
				"org.elasticsearch.client.elasticsearch-rest-client:" + helper.getRestClientModuleSlot(),
				"org.apache.httpcomponents:main",
				"org.elasticsearch.client.elasticsearch-rest-client-sniffer:" + helper.getRestClientSnifferModuleSlot()
		);
		StringAsset manifest = new StringAsset(Descriptors.create(ManifestDescriptor.class)
				.attribute("Dependencies", dependencies)
				.exportAsString());
		return ShrinkWrap.create(WebArchive.class, ElasticsearchRestClientSnifferModuleIT.class.getSimpleName() + ".war")
				.addClasses(ElasticsearchRestClientSnifferModuleIT.class, VersionsHelper.class)
				.add(manifest, "META-INF/MANIFEST.MF");
	}

	private RestClient restClient;
	private Sniffer sniffer;

	@Rule
	public TemporaryFolder tmpFolder = new TemporaryFolder();

	@Before
	public void setup() {
		/*
		 * Intentionally do not mention the second node running on another port:
		 * we expect it to be discovered automatically.
		 */
		restClient = RestClient.builder(new HttpHost("localhost", NODE_1_PORT)).build();
		sniffer = Sniffer.builder(restClient).setSniffIntervalMillis(SNIFF_INTERVAL_MS).build();
	}

	@After
	public void tearDown() throws IOException {
		sniffer.close();
		restClient.close();
	}

	@Test
	public void discoverSecondNode() throws IOException, InterruptedException {
		Set<Integer> targetedPorts = new HashSet<>();

		long timeLimit = System.currentTimeMillis() + TEST_TIME_LIMIT_MS;
		while (!targetedPorts.equals(ALL_NODE_PORTS) && System.currentTimeMillis() < timeLimit) {
			Response response = restClient.performRequest("GET", "/");
			targetedPorts.add(response.getHost().getPort());
			Thread.sleep(TEST_STEP_SLEEP_MS);
		}

		Assert.assertEquals(
				"Expected the sniffer to have discovered the second node running on a different port after "
						+ TEST_TIME_LIMIT_MS + "ms",
				ALL_NODE_PORTS, targetedPorts
		);
	}

}
