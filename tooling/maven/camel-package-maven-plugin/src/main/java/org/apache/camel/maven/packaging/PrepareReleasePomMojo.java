/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.maven.packaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import static org.apache.camel.maven.packaging.PackageHelper.after;
import static org.apache.camel.maven.packaging.PackageHelper.loadText;
import static org.apache.camel.maven.packaging.PackageHelper.writeText;
import static org.apache.camel.maven.packaging.StringHelper.between;

/**
 * Prepares the apache-camel/pom.xml and common-bin to keep the Camel artifacts up-to-date.
 */
@Mojo(name = "prepare-release-pom", threadSafe = true)
public class PrepareReleasePomMojo extends AbstractMojo {

    /**
     * The maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;

    /**
     * The apache-camel/pom
     */
    @Parameter(defaultValue = "${project.build.directory}/../../../apache-camel/pom.xml")
    protected File releasePom;

    /**
     * The apache-camel/descriptors/common-bin.xml
     */
    @Parameter(defaultValue = "${project.build.directory}/../../../apache-camel/src/main/descriptors/common-bin.xml")
    protected File commonBinXml;

    /**
     * The directory for components
     */
    @Parameter(defaultValue = "${project.build.directory}/../../../components")
    protected File componentsDir;

    /**
     * The directory for spring boot starters
     */
    @Parameter(defaultValue = "${project.build.directory}/../../../platforms/spring-boot/components-starter")
    protected File startersDir;

    /**
     * Maven ProjectHelper.
     */
    @Component
    private MavenProjectHelper projectHelper;

    /**
     * Execute goal.
     *
     * @throws MojoExecutionException execution of the main class or one of the
     *                                                        threads it generated failed.
     * @throws MojoFailureException   something bad happened...
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        updatePomAndCommonBin(componentsDir, "camel components");
        updatePomAndCommonBin(startersDir, "camel starters");
    }

    protected void updatePomAndCommonBin(File dir, String token) throws MojoExecutionException, MojoFailureException {
        SortedSet<String> artifactIds = new TreeSet<>();

        try {
            Set<File> poms = new HashSet<>();
            findComponentPoms(dir, poms);
            for (File pom : poms) {
                String aid = asArtifactId(pom);
                if (isValidArtifactId(aid)) {
                    artifactIds.add(aid);
                }
            }
        } catch (IOException e) {
            throw new MojoFailureException("Error due " + e.getMessage(), e);
        }

        getLog().debug("ArtifactIds: " + artifactIds);

        // update pom.xml
        StringBuilder sb = new StringBuilder();
        for (String aid : artifactIds) {
            sb.append("    <dependency>\n");
            sb.append("      <groupId>org.apache.camel</groupId>\n");
            sb.append("      <artifactId>" + aid + "</artifactId>\n");
            sb.append("      <version>${project.version}</version>\n");
            sb.append("    </dependency>\n");
        }
        String changed = sb.toString();
        boolean updated = updateXmlFile(releasePom, token, changed, "    ");

        if (updated) {
            getLog().info("Updated apache-camel/pom.xml file");
        } else {
            getLog().debug("No changes to apache-camel/pom.xml file");
        }
        getLog().info("apache-camel/pom.xml contains " + artifactIds.size() + " " + token + " dependencies");

        // update common-bin.xml
        sb = new StringBuilder();
        for (String aid : artifactIds) {
            sb.append("        <include>org.apache.camel:" + aid + "</include>\n");
        }
        changed = sb.toString();
        updated = updateXmlFile(commonBinXml, token, changed, "        ");

        if (updated) {
            getLog().info("Updated apache-camel/src/main/descriptors/common-bin.xml file");
        } else {
            getLog().debug("No changes to apache-camel/src/main/descriptors/common-bin.xml file");
        }
        getLog().info("apache-camel/src/main/descriptors/common-bin.xml contains " + artifactIds.size() + " " + token + " dependencies");
    }

    private void findComponentPoms(File parentDir, Set<File> components) {
        File[] files = parentDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && file.getName().startsWith("camel-")) {
                    findComponentPoms(file, components);
                } else if (parentDir.getName().startsWith("camel-") && file.getName().equals("pom.xml")) {
                    components.add(file);
                }
            }
        }
    }

    private String asArtifactId(File pom) throws IOException {
        String text = loadText(new FileInputStream(pom));
        text = after(text, "</parent>");
        if (text != null) {
            return between(text, "<artifactId>", "</artifactId>");
        }
        return null;
    }

    private boolean isValidArtifactId(String aid) {
        return aid != null && !aid.endsWith("-maven-plugin") && !aid.endsWith("-parent");
    }

    private boolean updateXmlFile(File file, String token, String changed, String spaces) throws MojoExecutionException {
        String start = "<!-- " + token + ": START -->";
        String end = "<!-- " + token + ": END -->";

        if (!file.exists()) {
            return false;
        }

        try {
            String text = loadText(new FileInputStream(file));

            String existing = between(text, start, end);
            if (existing != null) {
                // remove leading line breaks etc
                existing = existing.trim();
                changed = changed.trim();
                if (existing.equals(changed)) {
                    return false;
                } else {
                    String before = StringHelper.before(text, start);
                    String after = StringHelper.after(text, end);
                    text = before + start + "\n" + spaces + changed + "\n" + spaces + end + after;
                    writeText(file, text);
                    return true;
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error reading file " + file + " Reason: " + e, e);
        }
    }

}
