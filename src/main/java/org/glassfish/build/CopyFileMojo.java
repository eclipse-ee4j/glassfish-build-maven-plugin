/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation. All rights reserved.
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
import java.nio.file.StandardCopyOption;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Echo a message.
 */
@Mojo(name = "copy-file", threadSafe = true)
public final class CopyFileMojo extends AbstractMojo {

    private static final String PROPERTY_PREFIX = Constants.PROPERTY_PREFIX + "copyFile.";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Source file to copy. If not set, the main artifact will be copied.
     */
    @Parameter(property = PROPERTY_PREFIX + "sourceFile")
    private File sourceFile;

    @Parameter(property = PROPERTY_PREFIX + "destFile")
    private File destFile;

    @Parameter(property = PROPERTY_PREFIX + "overwrite", defaultValue = "true")
    private boolean overwrite;

    @Override
    public void execute() throws MojoExecutionException {
        if (sourceFile == null) {
            sourceFile = project.getArtifact().getFile();
            if (sourceFile == null) {
                throw new MojoExecutionException(this, "The main artifact has not been built yet, cannot copy it.",
                        "Either run this goal after the main artifact is built (in or after the package phase),"
                                + " or specify the 'sourceFile' parameter");
            }
        }
        try {
            Files.createDirectories(destFile.getParentFile().toPath());
            getLog().info("Copying " + sourceFile + " to " + destFile);
            if (overwrite) {
                Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.copy(sourceFile.toPath(), destFile.toPath());
            }
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to copy " + sourceFile + " to " + destFile, ex);
        }
    }
}
