
/*
 * Copyright 2024 Aristide Cittadino
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.water.implementation.osgi.util.test.karaf;

import org.apache.karaf.itests.KarafTestSupport;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFileExtendOption;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;

import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;


/**
 * Author Aristide Cittadino
 * Helper class for tests
 */
public class WaterTestConfiguration {
    private static final String ADD_OPENS_PARAM = "--add-opens";
    private static final String ACS_MAVEN_REPO = "https://nexus.acsoftware.it/nexus/repository/maven-water";
    private static final String JACOCO_PATH = "jacoco/jacoco-0.8.7";
    private static final String JACOCO_JAR_PATH = JACOCO_PATH + "/lib/jacocoagent.jar";
    private static Logger log = LoggerFactory.getLogger(WaterTestConfiguration.class.getName());
    public static final String MIN_RMI_SERVER_PORT = "44444";
    public static final String MAX_RMI_SERVER_PORT = "65534";
    public static final String MIN_HTTP_PORT = "9080";
    public static final String MAX_HTTP_PORT = "9999";
    public static final String MIN_RMI_REG_PORT = "1099";
    public static final String MAX_RMI_REG_PORT = "9999";
    public static final String MIN_SSH_PORT = "8101";
    public static final String MAX_SSH_PORT = "8888";

    private String testSuiteName;
    private String codeCoverageReportPath;
    private String codeCoverageClassesPath;

    private String distributionGroupId;
    private String distributionArtifactId;
    private String distributionVersion;
    private String distributionRepo;
    private String httpPort;
    private String rmiRegistryPort;
    private String rmiServerPort;
    private String sshPort;
    private String waterRuntimeTestVersion;
    private String karafVersion;
    private boolean enabledJwtFilter = false;
    private Option[] options;

    public WaterTestConfiguration(String karafVersion, String waterRuntimeTestVersion, String testSuiteName) {
        httpPort = Integer.toString(KarafTestSupport.getAvailablePort(
                Integer.parseInt(MIN_HTTP_PORT), Integer.parseInt(MAX_HTTP_PORT)));
        rmiRegistryPort = Integer.toString(KarafTestSupport.getAvailablePort(
                Integer.parseInt(MIN_RMI_REG_PORT), Integer.parseInt(MAX_RMI_REG_PORT)));
        rmiServerPort = Integer.toString(KarafTestSupport.getAvailablePort(
                Integer.parseInt(MIN_RMI_SERVER_PORT), Integer.parseInt(MAX_RMI_SERVER_PORT)));
        sshPort = Integer.toString(KarafTestSupport.getAvailablePort(Integer.parseInt(MIN_SSH_PORT),
                Integer.parseInt(MAX_SSH_PORT)));
        this.waterRuntimeTestVersion = waterRuntimeTestVersion;
        this.karafVersion = karafVersion;
        this.options = getBaseConfig();
        this.testSuiteName = testSuiteName;
        //default saves in target folder
        this.codeCoverageClassesPath = "../../../build/jacoco/classes";
        this.codeCoverageReportPath = "../../../build/jacoco";
        this.distributionGroupId = "it.water.container";
        this.distributionArtifactId = "water-karaf-distribution-test";
        this.distributionRepo = ACS_MAVEN_REPO;
        this.distributionVersion = this.waterRuntimeTestVersion;
        log.info("SSH PORT: {}", sshPort);
    }

    public WaterTestConfiguration(String karafVersion, String waterRuntimeTestVersion) {
        this(karafVersion, waterRuntimeTestVersion, "WaterTest");
    }

    public WaterTestConfiguration withDistribution(String groupId, String artifactId, String version) {
        this.distributionGroupId = groupId;
        this.distributionArtifactId = artifactId;
        this.distributionVersion = version;
        return this;
    }

    public WaterTestConfiguration withDistributionRepo(String distributionRepoUrl) {
        this.distributionRepo = distributionRepoUrl;
        return this;
    }

    public WaterTestConfiguration withDebug(String port, boolean hold) {
        Option[] debugConfig = {debugConfiguration(port, hold)};
        return append(debugConfig);
    }

    public WaterTestConfiguration withHttpPort(String httpPort) {
        this.httpPort = httpPort;
        return this;
    }

    public WaterTestConfiguration withJwtFilterEnabled(boolean value) {
        this.enabledJwtFilter = value;
        return this;
    }

    public WaterTestConfiguration keepRuntime() {
        Option opt = KarafDistributionOption.keepRuntimeFolder();
        return append(new Option[]{opt});
    }

    public WaterTestConfiguration withLogLevel(LogLevelOption.LogLevel level) {
        Option opt = KarafDistributionOption.logLevel(level);
        return append(new Option[]{opt});
    }

    public WaterTestConfiguration withCodeCoverageReportPath(String reportPath) {
        this.codeCoverageReportPath = reportPath;
        return this;
    }

    public WaterTestConfiguration withCodeCoverageClassesPath(String classesPath) {
        this.codeCoverageClassesPath = classesPath;
        return this;
    }

    public WaterTestConfiguration withPropertyUpdated(String configFile, String propName, String value) {
        append(new Option[]{editConfigurationFilePut(configFile,
                propName, value)});
        return this;
    }

    public WaterTestConfiguration withEnvironmentVariable(String name, String value) {
        append(new Option[]{environment(name + "=" + value)});
        return this;
    }

    public WaterTestConfiguration withCodeCoverage(String... packageFilter) {
        StringBuilder sb = new StringBuilder();
        Arrays.asList(packageFilter).stream().forEach(packageStr -> {
            if (sb.length() > 0)
                sb.append(":");
            sb.append(packageStr);
        });
        Option coverage = createCodeCoverageOption(this.testSuiteName, sb.toString(), this.codeCoverageReportPath, this.codeCoverageClassesPath);
        //needed to find coverage on classes invoked by interfaces
        KarafDistributionConfigurationFileExtendOption jacocoOpts = new KarafDistributionConfigurationFileExtendOption("etc/config.properties","org.osgi.framework.bootdelegation","org.jacoco.agent.rt");
        KarafDistributionConfigurationFileExtendOption jacocoOpts1 = new KarafDistributionConfigurationFileExtendOption("etc/config.properties","org.osgi.framework.bootdelegation","org.jacoco.agent.rt.*");
        return append(new Option[]{jacocoOpts,jacocoOpts1,coverage});

    }

    public WaterTestConfiguration withXms(String xms) {
        return append(new Option[]{vmOption("-Xms" + xms)});
    }

    public WaterTestConfiguration withXmx(String xmx) {
        return append(new Option[]{vmOption("-Xmx" + xmx)});
    }

    public WaterTestConfiguration withSkipContainerCreation() {
        append(new Option[]{vmOption("-Dskip-container-creation=true")});
        return this;
    }

    public WaterTestConfiguration append(Option[] customOptions) {
        Option[] config = new Option[this.options.length + customOptions.length];
        System.arraycopy(this.options, 0, config, 0, this.options.length);
        System.arraycopy(customOptions, 0, config, options.length, customOptions.length);
        this.options = config;
        return this;
    }

    public Option[] build() {
        //propagating prop to skip container creation if it is passed to test runner
        if (Boolean.parseBoolean(System.getProperty("skip-container-creation", "false"))) {
            withSkipContainerCreation();
        }

        System.setProperty("org.ops4j.pax.url.mvn.repositories", this.distributionRepo);
        MavenArtifactUrlReference karafUrl = maven().groupId(distributionGroupId)
                .artifactId(distributionArtifactId).version(distributionVersion).type("tar.gz");
        Option[] distributionOption = new Option[]{
                karafDistributionConfiguration().frameworkUrl(karafUrl)
                        .name("Water Karaf Distribution")
                        .unpackDirectory(new File("target/exam"))
                        .useDeployFolder(false)
        };

        Option[] dynamicOptions = new Option[]{
                editConfigurationFilePut("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port", httpPort),
                editConfigurationFilePut("etc/it.water.application.cfg", "water.rest.security.jwt.validate", String.valueOf(enabledJwtFilter)),
        };

        append(dynamicOptions);
        append(distributionOption);
        append(configureVmOptions());
        return this.options;
    }

    //Method used only for java version > 9 in order to avoid pax exam freezing on startup
    protected Option[] configureVmOptions() {
        int javaVersion = Integer.parseInt(System.getProperty("java.version").split("\\.")[0]);
        if (javaVersion >= 9) {
            return options(
                    systemProperty("pax.exam.osgi.`unresolved.fail").value("true"),
                    vmOption("--add-reads=java.xml=java.logging"),
                    vmOption("--add-exports=java.base/org.apache.karaf.specs.locator=java.xml,ALL-UNNAMED"),
                    vmOption("--patch-module"),
                    vmOption(
                            "java.base=lib/endorsed/org.apache.karaf.specs.locator-"
                                    + System.getProperty("karafVersion", karafVersion)
                                    + ".jar"),
                    vmOption("--patch-module"),
                    vmOption(
                            "java.xml=lib/endorsed/org.apache.karaf.specs.java.xml-"
                                    + System.getProperty("karafVersion", karafVersion)
                                    + ".jar"),
                    vmOption(ADD_OPENS_PARAM),
                    vmOption("java.base/java.security=ALL-UNNAMED"),
                    vmOption(ADD_OPENS_PARAM),
                    vmOption("java.base/java.net=ALL-UNNAMED"),
                    vmOption(ADD_OPENS_PARAM),
                    vmOption("java.base/java.lang=ALL-UNNAMED"),
                    vmOption(ADD_OPENS_PARAM),
                    vmOption("java.base/java.util=ALL-UNNAMED"),
                    vmOption(ADD_OPENS_PARAM),
                    vmOption("java.base/jdk.internal.reflect=ALL-UNNAMED"),
                    vmOption(ADD_OPENS_PARAM),
                    vmOption("java.naming/javax.naming.spi=ALL-UNNAMED"),
                    vmOption(ADD_OPENS_PARAM),
                    vmOption("java.rmi/sun.rmi.transport.tcp=ALL-UNNAMED"),
                    vmOption("--add-exports=java.base/sun.net.www.protocol.http=ALL-UNNAMED"),
                    vmOption("--add-exports=java.base/sun.net.www.protocol.https=ALL-UNNAMED"),
                    vmOption("--add-exports=java.base/sun.net.www.protocol.jar=ALL-UNNAMED"),
                    vmOption("--add-exports=jdk.naming.rmi/com.sun.jndi.url.rmi=ALL-UNNAMED"),
                    vmOption("-classpath"),
                    vmOption("lib/jdk9plus/*" + File.pathSeparator + "lib/boot/*"),
                    // avoid integration tests stealing focus on OS X
                    vmOption("-Djava.awt.headless=true"),
                    vmOption("-Dfile.encoding=UTF8"));
        }
        return new Option[]{};
    }


    /**
     * @param name
     * @param packageFilter
     * @param reportFolderPath
     * @param classesFolderPath
     * @return
     */
    private Option createCodeCoverageOption(String name, String packageFilter, String reportFolderPath, String classesFolderPath) {
        reportFolderPath = (reportFolderPath.endsWith("/") || reportFolderPath.endsWith("\\")) ? reportFolderPath : reportFolderPath + File.separator;
        classesFolderPath = (classesFolderPath.endsWith("/") || classesFolderPath.endsWith("\\")) ? classesFolderPath : classesFolderPath + File.separator;
        packageFilter = (packageFilter.endsWith(".*")) ? packageFilter : packageFilter + ".*";
        return new VMOption("-javaagent:" + JACOCO_JAR_PATH + "=destfile=" + reportFolderPath + name + ".exec,includes=" + packageFilter + ",classdumpdir=" + classesFolderPath);
    }

    private Option[] getBaseConfig() {
        return new Option[]{
                propagateSystemProperties("org.ops4j.pax.url.mvn.repositories"),
                // enable JMX RBAC security, thanks to the KarafMBeanServerBuilder
                configureSecurity().disableKarafMBeanServerBuilder(),
                // Setting test mode ON
                mavenBundle().groupId("org.apache.karaf.itests").artifactId("common")
                        .version(this.karafVersion),
                editConfigurationFilePut("etc/it.water.application.cfg",
                        "it.water.testMode", "true"),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort",
                        rmiRegistryPort),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort",
                        rmiServerPort),
                editConfigurationFilePut("etc/org.apache.karaf.shell.cfg", "sshPort", sshPort)};
    }

}
