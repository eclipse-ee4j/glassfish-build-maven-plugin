/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * Copyright (c) 2017, 2022 Oracle and/or its affiliates. All rights reserved.
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
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.StringUtils;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.components.io.fileselectors.IncludeExcludeFileSelector;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

/**
 * Resolves and unpack corresponding sources of project dependencies.
 */
@Mojo(
    name = "featuresets-dependencies",
    requiresProject = true,
    threadSafe = true,
    requiresDependencyResolution = ResolutionScope.COMPILE,
    defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public final class FeatureSetsDependenciesMojo extends AbstractMojo {

    /**
     * Parameters property prefix.
     */
    private static final String PROPERTY_PREFIX = "gfbuild.featuresets.dependencies.";

    /**
     * The entry point to Aether.
     */
    @Component
    private RepositorySystem repoSystem;

    /**
     * The current repository/network configuration of Maven.
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    /**
     * The project remote repositories to use.
     */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    private List<RemoteRepository> remoteRepos;

    /**
     * Manager used to look up Archiver/UnArchiver implementations.
     */
    @Component
    private ArchiverManager archiverManager;

    /**
     * The maven project.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * The directory where the files will be copied.
     */
    @Parameter(property = PROPERTY_PREFIX + "stageDirectory", defaultValue = "${project.build.directory}/stage")
    private File stageDirectory;

    /**
     * Comma separated list of file extensions to include for copy.
     */
    @Parameter(property = PROPERTY_PREFIX + "copyTypes", defaultValue = "jar,war,rar")
    private String copyTypes;

    /**
     * Comma separated list of (g:)a(:v) to excludes for unpack.
     */
    @Parameter(property = PROPERTY_PREFIX + "copyExcludes")
    private final List<String> copyExcludes = Collections.emptyList();

    /**
     * Comma separated list of file extensions to include for unpack.
     */
    @Parameter(property = PROPERTY_PREFIX + "unpackTypes", defaultValue = "zip")
    private String unpackTypes;

    /**
     * Comma separated list of (g:)a(:v) to excludes for unpack.
     */
    @Parameter(property = PROPERTY_PREFIX + "unpackExcludes")
    private final List<String> unpackExcludes = Collections.emptyList();

    /**
     * Comma separated list of include patterns.
     */
    @Parameter(property = PROPERTY_PREFIX + "includes", defaultValue = "")
    private String includes;

    /**
     * Comma separated list of exclude patterns.
     */
    @Parameter(property = PROPERTY_PREFIX + "excludes", defaultValue = "")
    private String excludes;

    /**
     * Scope to include.
     * An Empty string indicates all scopes.
     */
    @Parameter(property = PROPERTY_PREFIX + "includeScope", defaultValue = "compile", required = false)
    private String includeScope;

    /**
     * Scope to exclude.
     * An Empty string indicates no scopes.
     */
    @Parameter(property = PROPERTY_PREFIX + "excludeScope", defaultValue = "test,system")
    private String excludeScope;

    /**
     * The groupId of the feature sets to include.
     */
    @Parameter(property = PROPERTY_PREFIX + "featureset.groupid.includes")
    private final List<String> featureSetGroupIdIncludes = Collections.emptyList();

    /**
     * Custom mappings.
     */
    @Parameter
    private List<DependencyMapping> mappings;

    /**
     * Skip this mojo.
     */
    @Parameter(property = PROPERTY_PREFIX + "skip", defaultValue = "false")
    private boolean skip;


    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping featuresets-dependencies");
            return;
        }

        final List<String> includeScopeList = stringAsList(includeScope, ",");
        final List<String> excludeScopeList = stringAsList(excludeScope, ",");
        final List<String> copyTypesList = stringAsList(copyTypes, ",");
        final List<String> unpackTypesList = stringAsList(unpackTypes, ",");

        // get all direct featureset dependencies's direct dependencies
        final Set<Dependency> dependencies = new HashSet<>();
        for (final org.apache.maven.artifact.Artifact artifact : project.getArtifacts()) {
            if (featureSetGroupIdIncludes.contains(artifact.getGroupId())) {
                final ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
                descriptorRequest.setArtifact(new DefaultArtifact(artifact.getGroupId(),
                    artifact.getArtifactId(), artifact.getClassifier(), artifact.getType(), artifact.getVersion()));
                descriptorRequest.setRepositories(remoteRepos);
                try {
                    final ArtifactDescriptorResult result = repoSystem.readArtifactDescriptor(repoSession,
                        descriptorRequest);
                    dependencies.addAll(result.getDependencies());
                } catch (final ArtifactDescriptorException ex) {
                    throw new MojoExecutionException(ex.getMessage(), ex);
                }
            }
        }

        // build a request to resolve all dependencies
        final Set<ArtifactRequest> dependenciesRequest = new HashSet<>();
        for (final Dependency dependency : dependencies) {
            final String depScope = dependency.getScope();
            if (includeScopeList.contains(depScope) && !excludeScopeList.contains(depScope)) {
                final ArtifactRequest request = new ArtifactRequest();
                request.setArtifact(dependency.getArtifact());
                request.setRepositories(remoteRepos);
                dependenciesRequest.add(request);
            }
        }

        // add project direct dependency
        for (final org.apache.maven.model.Dependency dependency : project.getDependencies()) {
            // if the dependency is a feature set or not of proper scope skip
            if (featureSetGroupIdIncludes.contains(dependency.getGroupId())
                || !isScopeIncluded(dependency.getScope())) {
                continue;
            }

            final ArtifactRequest request = new ArtifactRequest();
            request.setArtifact(new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(),
                dependency.getClassifier(), dependency.getType(), dependency.getVersion()));
            request.setRepositories(remoteRepos);
            dependenciesRequest.add(request);
        }

        // resolve all
        List<ArtifactResult> resolvedDependencies;
        try {
            resolvedDependencies = repoSystem.resolveArtifacts(repoSession, dependenciesRequest);
        } catch (final ArtifactResolutionException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }

        stageDirectory.mkdir();

        for (final ArtifactResult dependency : resolvedDependencies) {

            final File sourceFile = dependency.getArtifact().getFile();
            if (sourceFile == null) {
                getLog().error("dependency " + dependency.getArtifact().toString() + ", file is null");
                continue;
            }

            if (sourceFile.getName().isEmpty()) {
                getLog().info("dependency " + dependency.getArtifact().toString() + ": empty file name");
                continue;
            }

            // copy trumps unpack,
            // (but only if artifact is not excluded from copying already)
            if (isArtifactActionable(dependency, copyTypesList, copyExcludes)) {
                final String mapping = getMapping(dependency.getArtifact());
                final File destFile = new File(stageDirectory, mapping + "." + dependency.getArtifact().getExtension());
                final String relativeDestFile = destFile.getPath()
                    .substring(project.getBasedir().getPath().length() + 1);
                getLog().info("Copying " + dependency.getArtifact() + " to " + relativeDestFile);
                try {
                    Files.copy(sourceFile.toPath(), destFile.toPath());
                } catch (final IOException ex) {
                    getLog().error(ex.getMessage(), ex);
                }
            } else if (isArtifactActionable(dependency, unpackTypesList, unpackExcludes)) {
                final String mapping = getMapping(dependency.getArtifact());
                final File destDir = new File(stageDirectory, mapping);
                final String relativeDestDir = destDir.getPath().substring(project.getBasedir().getPath().length() + 1);
                getLog().info("Unpacking " + dependency.getArtifact() + " to " + relativeDestDir);
                unpack(sourceFile, destDir);
            }
        }
    }


    /**
     * Match the given scope with the includeScope and excludeScope parameters.
     *
     * @param scope the scope to match
     * @return {@code true} if the scope is included and not excluded,
     *         {@code false} otherwise
     */
    private boolean isScopeIncluded(final String scope) {
        return includeScope.contains(scope) && !excludeScope.contains(scope);
    }


    private boolean isArtifactActionable(final ArtifactResult dependency, final List<String> actionTypesList,
        final List<String> actionExcludes) {
        final boolean typeIncluded = actionTypesList.contains(dependency.getArtifact().getExtension());
        final boolean artifactExcluded = isArtifactExcluded(actionExcludes, dependency.getArtifact());
        if (artifactExcluded) {
            getLog().debug("Excluded: " + dependency.getArtifact());
        }
        return typeIncluded && !artifactExcluded;
    }


    /**
     * Match the given artifact against the exclusion list.
     *
     * @param excludes the exclusion list
     * @param artifact the artifact to match
     * @return {@code true} if the artifact is included, {@code false}
     *         otherwise
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    private static boolean isArtifactExcluded(final List<String> excludes, final Artifact artifact) {
        for (final String exclude : excludes) {
            final String[] gav = exclude.split(":");
            if (gav == null || gav.length == 0) {
                continue;
            }
            switch (gav.length) {
                // gav == artifactId
                case 1:
                    if (artifact.getArtifactId().equals(gav[0])) {
                        return true;
                    }
                    break;
                // gav == groupId:artifactId
                case 2:
                    if (artifact.getGroupId().equals(gav[0]) && artifact.getArtifactId().equals(gav[1])) {
                        return true;
                    }
                    break;
                // gav == groupId:artifactId:version
                case 3:
                    if (artifact.getGroupId().equals(gav[0]) && artifact.getArtifactId().equals(gav[1])
                        && artifact.getVersion().equals(gav[2])) {
                        return true;
                    }
                    break;
                default:
                    throw new IllegalArgumentException("invalid exclude entry");
            }
        }
        return false;
    }


    /**
     * Get the mapping for a given artifact.
     * Lookup the configured mapping for a custom mapping, otherwise return the
     * artifactId
     *
     * @param artifact the artifact to be mapped
     * @return the mapped name for the artifact
     */
    private String getMapping(final Artifact artifact) {
        if (artifact == null) {
            throw new IllegalArgumentException("artifact must be non null");
        }

        if (mappings != null && !mappings.isEmpty()) {
            for (final DependencyMapping mapping : mappings) {
                // if groupId is supplied, filter groupId
                if (mapping.getGroupId() != null && !mapping.getGroupId().isEmpty()) {
                    if (!artifact.getGroupId().equals(mapping.getGroupId())) {
                        continue;
                    }
                }
                if (artifact.getArtifactId().equals(mapping.getArtifactId()) && mapping.getName() != null
                    && !mapping.getName().isEmpty()) {
                    return mapping.getName();
                }
            }
        }
        return artifact.getArtifactId();
    }


    private void unpack(final File file, final File location) throws MojoExecutionException {
        final Log log = getLog();
        if (log.isDebugEnabled()) {
            log.debug(toLogMessage(file, location));
        }
        location.mkdirs();
        try {
            UnArchiver unArchiver = archiverManager.getUnArchiver(file);
            unArchiver.setSourceFile(file);
            unArchiver.setDestDirectory(location);

            if (StringUtils.isNotEmpty(excludes) || StringUtils.isNotEmpty(includes)) {
                IncludeExcludeFileSelector selector = new IncludeExcludeFileSelector();
                if (StringUtils.isNotEmpty(excludes)) {
                    selector.setExcludes(excludes.split(","));
                }
                if (StringUtils.isNotEmpty(includes)) {
                    selector.setIncludes(includes.split(","));
                }
                unArchiver.setFileSelectors(new IncludeExcludeFileSelector[] {selector});
            }
            unArchiver.extract();
        } catch (NoSuchArchiverException e) {
            throw new MojoExecutionException("Unknown archiver type", e);
        } catch (ArchiverException e) {
            throw new MojoExecutionException("Error unpacking file: " + file + " to: " + location, e);
        }
    }


    /**
     * Create the logging message for an unpack invocation.
     *
     * @param file the file being unpacked
     * @param location the target directory for the unpack
     * @return the created {@code String}
     */
    private String toLogMessage(final File file, final File location) {
        StringBuilder msg = new StringBuilder();
        msg.append("Unpacking ");
        msg.append(file);
        msg.append(" to ");
        msg.append(location);

        if (includes != null && excludes != null) {
            msg.append(" with includes \"").append(includes);
            msg.append("\" and excludes \"").append(excludes).append('"');
        } else if (includes != null) {
            msg.append(" with includes \"").append(includes).append('"');
        } else if (excludes != null) {
            msg.append(" with excludes \"").append(excludes).append('"');
        }
        return msg.toString();
    }


    /**
     * Convert a {@code String} to a {@code List}.
     *
     * @param str the {@code String} to convert
     * @param c the character used as separated in the {@code String}
     * @return the converted {@code List}
     */
    private static List<String> stringAsList(final String str, final String c) {
        if (str != null && !str.isEmpty()) {
            return Arrays.asList(str.split(c));
        }
        return Collections.emptyList();
    }
}
