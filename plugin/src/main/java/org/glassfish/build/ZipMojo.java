/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * Copyright (c) 2012, 2023 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.build;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.types.ZipFileSet;

import static org.glassfish.build.utils.MavenHelper.createZip;
import static org.glassfish.build.utils.MavenHelper.createZipFileSet;

/**
 * Creates a zip file.
 */
@Mojo(name = "zip",
        requiresDependencyResolution = ResolutionScope.RUNTIME,
        defaultPhase = LifecyclePhase.PACKAGE,
        requiresProject = true)
public final class ZipMojo extends AbstractMojo {

    /**
     * Parameters property prefix.
     */
    private static final String PROPERTY_PREFIX = "gfzip.outputDirectory";

    /**
     * The maven project.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * The directory where the zip will be created.
     */
    @Parameter(property = PROPERTY_PREFIX + "outputDirectory",
            defaultValue = "${project.build.directory}")
    private File outputDirectory;

    /**
     * The file name of the created zip.
     */
    @Parameter(property = PROPERTY_PREFIX + "finalName",
            defaultValue = "${project.build.finalName}")
    private String finalName;

    /**
     * behavior when a duplicate file is found.
     * Valid values are "add", "preserve", and "fail" ; default value is "add"
     */
    @Parameter(property = PROPERTY_PREFIX + "duplicate",
            defaultValue = "add")
    private String duplicate;

    /**
     * Content to include in the zip.
     */
    @Parameter(property = PROPERTY_PREFIX + "filesets")
    private ZipFileSet[] filesets;

    /**
     * The root directory of the default FileSet.
     * Only when no fileset(s) provided.
     */
    @Parameter(property = PROPERTY_PREFIX + "dir",
            defaultValue = "${project.build.directory}")
    private File dir;

    /**
     * Comma or space separated list of include patterns.
     * all files are included when omitted ; Only when no fileset provided.
     */
    @Parameter(property = PROPERTY_PREFIX + "includes")
    private String includes;

    /**
     * Comma or space separated list of exclude patterns.
     * all files are included when omitted ; Only when no fileset provided.
     */
    @Parameter(property = PROPERTY_PREFIX + "excludes")
    private String excludes;

    /**
     * The extension of the generated file.
     */
    @Parameter(property = PROPERTY_PREFIX + "extension",
            defaultValue = "zip")
    private String extension;

    /**
     * Attach the produced artifact.
     */
    @Parameter(property = PROPERTY_PREFIX + "attach",
            defaultValue = "true")
    private Boolean attach;

    /**
     * Timestamp for reproducible output archive entries, either formatted as ISO 8601
     * <code>yyyy-MM-dd'T'HH:mm:ssXXX</code> or as an int representing seconds since the epoch (like
     * <a href="https://reproducible-builds.org/docs/source-date-epoch/">SOURCE_DATE_EPOCH</a>).
     */
    @Parameter(defaultValue = "${project.build.outputTimestamp}")
    private String outputTimestamp;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        this.project.addCompileSourceRoot(null);
        List<ZipFileSet> fsets;
        if (filesets != null && filesets.length > 0) {
            fsets = Arrays.asList(filesets);
        } else {
            fsets = new ArrayList<>();
            fsets.add(createZipFileSet(dir, includes, excludes));
        }

        // configure for Reproducible Builds based on outputTimestamp value
        Optional<Instant> timestamp = MavenArchiver.parseBuildOutputTimestamp(outputTimestamp);

        File target = createZip(project.getProperties(), getLog(),
                duplicate, fsets, new File(outputDirectory,
                        finalName + '.' + extension), timestamp);

        if (attach) {
            project.getArtifact().setFile(target);
            project.getArtifact().setArtifactHandler(
                    new DistributionArtifactHandler(extension,
                            project.getPackaging()));
        }
    }

    /**
     * {@code ArtifactHandler} implementation.
     */
    private static final class DistributionArtifactHandler
            implements ArtifactHandler {

        /**
         * Artifact file extension.
         */
        private final String extension;

        /**
         * Artifact packaging.
         */
        private final String packaging;

        /**
         * Create a new {@code DistributionArtifactHandler} instance.
         * @param ext the artifact extension
         * @param pkg the artifact packaging
         */
        private DistributionArtifactHandler(final String ext,
                final String pkg) {
            this.extension = ext;
            this.packaging = pkg;
        }

        @Override
        public String getExtension() {
            return extension;
        }

        @Override
        public String getDirectory() {
            return null;
        }

        @Override
        public String getClassifier() {
            return null;
        }

        @Override
        public String getPackaging() {
            return packaging;
        }

        @Override
        public boolean isIncludesDependencies() {
            return false;
        }

        @Override
        public String getLanguage() {
            return "java";
        }

        @Override
        public boolean isAddedToClasspath() {
            return false;
        }
    }
}
