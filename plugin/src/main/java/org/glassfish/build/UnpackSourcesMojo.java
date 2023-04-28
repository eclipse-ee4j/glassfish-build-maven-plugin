/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * Copyright (c) 2012, 2018 Oracle and/or its affiliates. All rights reserved.
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
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;

import static org.glassfish.build.utils.MavenHelper.cleanToBeTokenizedString;
import static org.glassfish.build.utils.MavenHelper.filterArtifacts;
import static org.glassfish.build.utils.MavenHelper.resolveArtifact;
import static org.glassfish.build.utils.MavenHelper.unpack;

/**
 * Resolves and unpack corresponding sources of project dependencies.
 */
@Mojo(name = "unpack-sources",
        requiresDependencyResolution = ResolutionScope.RUNTIME,
        defaultPhase = LifecyclePhase.PROCESS_RESOURCES,
        requiresProject = true)
public final class UnpackSourcesMojo extends AbstractMojo {

    /**
     * Parameters property prefix.
     */
    private static final String PROPERTY_PREFIX = "gfbuild.unpack";

    /**
     * The maven project.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * The entry point to Aether.
     */
    @Component
    private RepositorySystem repoSystem;

    /**
     * The current repository/network configuration of Maven.
     */
    @Parameter(defaultValue = "${repositorySystemSession}",
            readonly = true)
    private RepositorySystemSession repoSession;

    /**
     * The project remote repositories to use.
     *
     */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}",
            readonly = true)
    private List<RemoteRepository> remoteRepos;

    /**
     * To look up Archiver/UnArchiver implementations.
     */
    @Component
    private ArchiverManager archiverManager;

    /**
     * Comma separated list of include patterns.
     */
    @Parameter(property = PROPERTY_PREFIX + "includes")
    private String includes;

    /**
     * Comma separated list of include patterns.
     */
    @Parameter(property = PROPERTY_PREFIX + "excludes")
    private String excludes;

    /**
     * If we should exclude transitive dependencies.
     */
    @Parameter(property = PROPERTY_PREFIX + "excludeTransitive",
            defaultValue = "",
            required = false)
    private boolean excludeTransitive;

    /**
     * Comma Separated list of Types to include.
     * Empty String indicates include everything (default).
     */
    @Parameter(property = PROPERTY_PREFIX + "includeTypes",
            defaultValue = "",
            required = false)
    private String includeTypes;

    /**
     * Comma Separated list of Types to exclude.
     * Empty String indicates don't exclude anything (default).
     */
    @Parameter(property = PROPERTY_PREFIX + "excludeTypes",
            defaultValue = "",
            required = false)
    private String excludeTypes;

    /**
     * Scope to include.
     * An Empty string indicates all scopes (default).
     */
    @Parameter(property = PROPERTY_PREFIX + "includeScope",
            defaultValue = "",
            required = false)
    private String includeScope;

    /**
     * Scope to exclude.
     * An Empty string indicates no scopes (default).
     */
    @Parameter(property = PROPERTY_PREFIX + "excludeScope",
            defaultValue = "",
            required = false)
    private String excludeScope;

    /**
     * Comma Separated list of Classifiers to include.
     * Empty String indicates include everything (default).
     */
    @Parameter(property = PROPERTY_PREFIX + "includeClassifiers",
            defaultValue = "",
            required = false)
    private String includeClassifiers;

    /**
     * Comma Separated list of Classifiers to exclude.
     * Empty String indicates don't exclude anything (default).
     */
    @Parameter(property = PROPERTY_PREFIX + "excludeClassifiers",
            defaultValue = "",
            required = false)
    private String excludeClassifiers;

    /**
     * Comma separated list of Artifact names to exclude.
     */
    @Parameter(property = PROPERTY_PREFIX + "excludeArtifactIds",
            defaultValue = "",
            required = false)
    private String excludeArtifactIds;

    /**
     * Comma separated list of Artifact names to include.
     */
    @Parameter(property = PROPERTY_PREFIX + "includeArtifactIds",
            defaultValue = "")
    private String includeArtifactIds;

    /**
     * Comma separated list of GroupId Names to exclude.
     */
    @Parameter(property = PROPERTY_PREFIX + "excludeGroupIds",
            defaultValue = "")
    private String excludeGroupIds;

    /**
     * Comma separated list of GroupIds to include.
     */
    @Parameter(property = PROPERTY_PREFIX + "includeGroupIds",
            defaultValue = "")
    private String includeGroupIds;

    /**
     * Directory where the sources artifacts are unpacked.
     */
    @Parameter(property = PROPERTY_PREFIX + "outputDirectory",
            defaultValue = "${project.build.directory}/sources-dependency")
    private File outputDirectory;

    /**
     * Verbosity.
     */
    @Parameter(property = PROPERTY_PREFIX + "silent",
            defaultValue = "false")
    private boolean silent;

    /**
     * Attach the generated artifact to the maven project.
     */
    @Parameter(property = PROPERTY_PREFIX + "attach-sources",
            defaultValue = "false")
    private boolean attachSources;

    /**
     * Skip this mojo.
     */
    @Parameter(property = PROPERTY_PREFIX + "skip",
            defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping unpack-sources");
            return;
        }

        // get dependencies
        Set<Artifact> filteredDependencies = filterArtifacts(
                project.getArtifacts(), project.getDependencyArtifacts(),
                excludeTransitive, includeScope, excludeScope, excludeTypes,
                includeTypes, includeClassifiers, excludeClassifiers,
                includeGroupIds, excludeGroupIds, includeArtifactIds,
                excludeArtifactIds);

        for (Artifact artifact : filteredDependencies) {

            // resolve sources.jar
            ArtifactResult result = resolveArtifact(artifact.getGroupId(),
                    artifact.getArtifactId(), /* classifier */ "sources",
                    /* type */ "jar", artifact.getVersion(), repoSystem,
                    repoSession, remoteRepos);

            // unpack
            unpack(result.getArtifact().getFile(), outputDirectory,
                    cleanToBeTokenizedString(this.includes),
                    cleanToBeTokenizedString(this.excludes), silent, getLog(),
                    archiverManager);
        }

        if (attachSources) {
            project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
        }
    }
}
