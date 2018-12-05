/*
 * Copyright (c) 2013, 2018 Oracle and/or its affiliates. All rights reserved.
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
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;

import static org.glassfish.build.utils.MavenHelper.createArtifact;
import static org.glassfish.build.utils.MavenHelper.createAttachedArtifacts;
import static org.glassfish.build.utils.MavenHelper.getPomInTarget;
import static org.glassfish.build.utils.MavenHelper.readModel;

/**
 * Guess artifacts from target directory and attach them to the project.
 */
@Mojo(name = "attach-all-artifacts",
      requiresOnline = true)
public final class AttachAllArtifactsMojo extends AbstractMojo {

    /**
     * The maven project.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * The project pom file.
     */
    @Parameter(property = "attach.all.artifacts.pomFile",
            defaultValue = "${project.file}",
            required = true)
    private File pomFile;

    /**
     * Skip this mojo.
     */
    @Parameter(property = "attach.all.artifacts.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping artifact attachment");
            return;
        }

        // check for an existing .pom under target
        File targetPom = getPomInTarget(project.getBuild().getDirectory());
        if (targetPom != null) {
            pomFile = targetPom;
        }

        // if supplied pomFile is invalid, default to the project's pom
        if (pomFile == null || !pomFile.exists()) {
            pomFile = project.getFile();
        }

        if (!pomFile.exists()) {
            getLog().info("Skipping as there is no model to read from");
            return;
        }

        // read the model manually
        Model model = readModel(pomFile);

        // create the project artifact manually
        Artifact artifact = createArtifact(project.getBuild().getDirectory(),
                model);
        if (artifact == null) {
            getLog().info(
                    "Skipping as there is no file found for this artifact");
            return;
        }

        // create the project attached artifacts manually
        List<Artifact> attachedArtifacts =
                createAttachedArtifacts(project.getBuild().getDirectory(),
                        artifact, model);

        // add metadata to the project if not a "pom" type
        if (!"pom".equals(model.getPackaging())) {
            ArtifactMetadata metadata = new ProjectArtifactMetadata(artifact,
                    pomFile);
            artifact.addMetadata(metadata);
        }

        // set main artifact
        project.setArtifact(artifact);

        // set model
        project.setFile(pomFile);

        for (Iterator i = attachedArtifacts.iterator(); i.hasNext();) {
            project.addAttachedArtifact((Artifact) i.next());
        }
    }
}
