Elasticsearch clients JBoss Module as WildFly feature pack
==========================================================

This creates a set of [JBoss Module](https://jboss-modules.github.io/jboss-modules/manual/)s containing [Elasticsearch Clients](https://github.com/elastic/elasticsearch/tree/master/client) as
and packages the libraries into a WildFly *feature pack* for easy provisioning into your custom [WildFly](http://wildfly.org/) server or [WildFly Swarm](http://wildfly-swarm.io/).

Currently only includes the low-level REST client and its sniffer.

Historically the Hibernate Search project has been releasing such modules,
for convenience of Hibernate Search users running applications on WildFly or JBoss EAP.

We now moved these artifacts to a dedicated bundle so that other projects using the Elasticsearch clients
don't have to download the Hibernate Search specific modules,
and to attempt to have a single consistent distribution of such modules.

This should also make it easier to release a new bundle of these modules
as soon as a new Elasticsearch version is released,
without necessarily waiting for Hibernate Search to have adopted the new version.

## Version conventions

We will use an `X.Y.Z.qualifier` pattern as recommended by
[JBoss Project Versioning](https://developer.jboss.org/wiki/JBossProjectVersioning),
wherein the `X.Y.Z` section will match the version of the Elasticsearch version included in the modules,
possibly using a `0` for the last figure when this is missing in the Elasticsearch version scheme.

We will add an additional qualifier, lexicographically increasing with further releases,
to distinguish different releases of this package when still containing the same version
of the Elasticsearch clients.
This might be useful to address problems in the package structure, or any other reason to have
to release a new version of these modules containing the same Elasticsearch version as a previously
released copy of these modules.

An example version could be `5.6.4.hibernate02` to contain Elasticsearch clients version `5.6.4`.

## Usage: server provisioning via Maven

Maven users can use the `wildfly-server-provisioning-maven-plugin` to create a custom WildFly server including these modules:

	<plugins>
		<plugin>
		<groupId>org.wildfly.build</groupId>
		<artifactId>wildfly-server-provisioning-maven-plugin</artifactId>
		<version>1.2.0.Final</version>
		<executions>
			<execution>
			<id>server-provisioning</id>
			<goals>
				<goal>build</goal>
			</goals>
			<phase>compile</phase>
			<configuration>
				<config-file>server-provisioning.xml</config-file>
				<server-name>minimal-wildfly-with-elasticsearch-client</server-name>
			</configuration>
			</execution>
		</executions>
	</plugin>

You will also need a `server-provisioning.xml` in the root of your project:

	<server-provisioning xmlns="urn:wildfly:server-provisioning:1.1">
		<feature-packs>
	
			<feature-pack
				groupId="org.hibernate.elasticsearch-client-jbossmodules"
				artifactId="elasticsearch-client-jbossmodules"
				version="${elasticsearch-client-modules.version}"/>
	
			<feature-pack
				groupId="org.wildfly"
				artifactId="wildfly-servlet-feature-pack"
				version="${your-preferred-wildfly.version}" />
	
		</feature-packs>
	</server-provisioning>

This will make Elasticsearch clients available as an opt-in dependency to any application deployed on WildFly.
To enable the dependency there are various options, documented in [Class Loading in WildFly](https://docs.jboss.org/author/display/WFLY/Class+Loading+in+WildFly).

N.B. the current version of these modules has been tested with `WildFly 11.0.0.Final`.

## Non-Maven users

Plugins for other build tools have not been implemented yet, but this should be quite straight forward to do: the above Maven plugin is just a thin wrapper invoking other libraries; these other libraries are build agnostic and are responsible for performing most of the work.

See also [WildFly provisioning build tools](https://github.com/wildfly/wildfly-build-tools).

The feature packs are also available for downloads as zip files on [JBoss Nexus](https://repository.jboss.org/nexus/index.html#welcome).

## How to Release

    mvn release:prepare
    mvn release:perform

This will produce two local commits and a local tag, then upload the artifacts to a staging repository on [JBoss Nexus](https://repository.jboss.org/nexus/index.html#welcome).

If it all works fine, don't forget to:

 * release the staging repository on Nexus
 * push the commits
 * push the tag

