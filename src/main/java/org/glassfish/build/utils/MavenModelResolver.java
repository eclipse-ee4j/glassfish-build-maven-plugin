/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * Copyright (c) 2013, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.build.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.repository.internal.ArtifactDescriptorUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;

/**
 * A model resolver that can resolve remote artifacts during model resolution.
 */
final class MavenModelResolver implements ModelResolver {

    /**
     * List of remote repositories.
     */
    private final List<RemoteRepository> repositories;

    /**
     * The repository IDs of the remote repositories.
     */
    private final Set<String> repositoryIds;

    /**
     * The repository system component.
     */
    private final RepositorySystem system;

    /**
     * The repository session component.
     */
    private final RepositorySystemSession session;

    /**
     * Create a new {@code MavenModelResolver} instance.
     * @param repoSystem repository system component
     * @param repoSession repository session component
     * @param remoteRepos remote repositories to use
     */
    MavenModelResolver(final RepositorySystem repoSystem,
            final RepositorySystemSession repoSession,
            final List<RemoteRepository> remoteRepos) {

        this.system = repoSystem;
        this.session = repoSession;
        this.repositories = new ArrayList<>(remoteRepos);
        this.repositoryIds = new HashSet<>();
        for (RemoteRepository repository : repositories) {
            repositoryIds.add(repository.getId());
        }
    }

    /**
     * Copy constructor.
     * @param clone the instance to copy
     */
    private MavenModelResolver(final MavenModelResolver clone) {
        this.system = clone.system;
        this.session = clone.session;
        this.repositories = new ArrayList<>(clone.repositories);
        this.repositoryIds = new HashSet<>(clone.repositoryIds);
    }

    @Override
    public void addRepository(final Repository repository,
            final boolean replace)
            throws InvalidRepositoryException {

        if (!replace && repositoryIds.contains(repository.getId())) {
            return;
        }
        if (!repositoryIds.add(repository.getId())) {
            return;
        }

        List<RemoteRepository> newRepositories =
                Collections.singletonList(
                ArtifactDescriptorUtils.toRemoteRepository(repository));

        repositoryIds.add(repository.getId());
        repositories.addAll(newRepositories);
    }

    @Override
    public void addRepository(final Repository repository)
            throws InvalidRepositoryException {

        addRepository(repository, /* replace */ false);
    }

    @Override
    public ModelResolver newCopy() {
        return new MavenModelResolver(this);
    }

    @Override
    public ModelSource resolveModel(final String groupId,
                  final String artifactId,
            final String version)
            throws UnresolvableModelException {

        Artifact artifact = new DefaultArtifact(groupId, artifactId, "pom",
                version);
        try {
            ArtifactRequest request = new ArtifactRequest(artifact,
                    repositories, /* context */ null);
            artifact = system.resolveArtifact(session, request).getArtifact();
        } catch (ArtifactResolutionException e) {
            throw new UnresolvableModelException(
                    String.format(
                            "Failed to resolve POM for %s:%s:%s due to %s",
                            groupId, artifactId, version, e.getMessage()),
                    groupId, artifactId, version, e);
        }
        return new FileModelSource(artifact.getFile());
    }

    @Override
    public ModelSource resolveModel(final Parent parent)
            throws UnresolvableModelException {

        return resolveModel(parent.getGroupId(), parent.getArtifactId(),
                parent.getVersion());
    }

    @Override
    public ModelSource resolveModel(final Dependency dependency)
            throws UnresolvableModelException {

        return resolveModel(dependency.getGroupId(), dependency.getArtifactId(),
                dependency.getVersion());
    }
}
