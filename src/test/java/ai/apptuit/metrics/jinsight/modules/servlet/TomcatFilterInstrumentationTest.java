/*
 * Copyright 2017 Agilx, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.apptuit.metrics.jinsight.modules.servlet;

import static ai.apptuit.metrics.jinsight.modules.servlet.ContextMetricsHelper.ROOT_CONTEXT_PATH;
import static ai.apptuit.metrics.jinsight.modules.servlet.TomcatRuleHelper.TOMCAT_METRIC_PREFIX;

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.junit.AfterClass;
import org.junit.BeforeClass;


/**
 * @author Rajiv Shivane
 */
public class TomcatFilterInstrumentationTest extends AbstractWebServerTest {

  private static final int SERVER_PORT = 9898;
  private static Tomcat tomcatServer;

  @BeforeClass
  public static void setUp() throws Exception {

    System.out.println("Tomcat [Configuring]");
    tomcatServer = new Tomcat();
    tomcatServer.setPort(SERVER_PORT);

    Path tempDirectory = Files
        .createTempDirectory(TomcatFilterInstrumentationTest.class.getSimpleName());
    File baseDir = new File(tempDirectory.toFile(), "tomcat");
    tomcatServer.setBaseDir(baseDir.getAbsolutePath());

    File applicationDir = new File(baseDir + "/webapps", "/ROOT");
    if (!applicationDir.exists()) {
      applicationDir.mkdirs();
    }

    Context appContext = tomcatServer.addWebapp("", applicationDir.getAbsolutePath());
    registerServlet(appContext, new PingPongServlet());
    registerServlet(appContext, new ExceptionServlet());
    registerServlet(appContext, new AsyncServlet());

    System.out.println("Tomcat [Starting]");
    tomcatServer.start();
    System.out.println("Tomcat [Started]");

  }

  @AfterClass
  public static void tearDown() throws Exception {
    System.out.println("Tomcat [Stopping]");
    tomcatServer.stop();
    tomcatServer.destroy();
    System.out.println("Tomcat [Stopped]");
  }

  private static <T extends BaseTestServlet> T registerServlet(Context context, T servlet) {
    Tomcat.addServlet(context, servlet.getClass().getSimpleName(), servlet);
    context.addServletMappingDecoded(servlet.getPath(), servlet.getClass().getSimpleName());
    return servlet;
  }

  protected URL pathToURL(String path) throws MalformedURLException {
    return new URL("http://localhost:" + SERVER_PORT + path);
  }

  @Override
  protected TagEncodedMetricName getRootMetric() {
    return TOMCAT_METRIC_PREFIX.withTags("context", ROOT_CONTEXT_PATH);
  }
}
