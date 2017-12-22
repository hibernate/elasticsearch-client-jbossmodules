package org.hibernate.elasticsearch.client.jbossmodules;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class VersionsHelper {

	public String getRestClientModuleSlot() {
		return getPropertiesVariable("elasticsearch-rest-client.slot.id");
	}

	public String getRestClientSnifferModuleSlot() {
		return getPropertiesVariable("elasticsearch-rest-client-sniffer.slot.id");
	}

	public String getJsonAssertVersion() {
		return getPropertiesVariable("jsonassert.fullversion");
	}

	private static String getPropertiesVariable(String key) {
		Properties projectCompilationProperties = new Properties();
		final InputStream resourceAsStream = VersionsHelper.class.getClassLoader()
				.getResourceAsStream("module-versions.properties");
		try {
			projectCompilationProperties.load(resourceAsStream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				resourceAsStream.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return projectCompilationProperties.getProperty(key);
	}

}
