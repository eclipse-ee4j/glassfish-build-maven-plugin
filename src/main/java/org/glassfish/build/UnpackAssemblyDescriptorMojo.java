/*
 * Copyright (c) 2023, 2024 Eclipse Foundation and/or its affiliates. All rights reserved.
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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import static org.glassfish.build.LifecyclePhaseHelper.GFBUILD_ASSEMBLY_DIR;

/**
 * Unpacks our descriptor so it can be used by the assembly-maven-plugin.
 * This mojo is just a helper mojo for lifecycles of this plugin.
 */
@Mojo(name = "unpack-assembly-descriptor", threadSafe = true, defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class UnpackAssemblyDescriptorMojo extends AbstractMojo {

    /**
     * The main artifact to be attached to the project as a main artifact.
     */
    @Parameter(property = "descriptorResource", required = true)
    private String descriptorResource;

    @Parameter(
        property = "outputDirectory",
        required = true,
        defaultValue = GFBUILD_ASSEMBLY_DIR)
    private File outputDirectory;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final InputStream source = UnpackAssemblyDescriptorMojo.class
            .getResourceAsStream("/assemblies/" + descriptorResource);
        outputDirectory.mkdirs();
        final File target = new File(outputDirectory, descriptorResource);
        getLog().debug("Unpacking " + descriptorResource + " to " + target.getAbsolutePath());
        try {
            Files.copy(source, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException e) {
            throw new MojoExecutionException("Failed to copy into " + target.getAbsolutePath(), e);
        }
    }
}
