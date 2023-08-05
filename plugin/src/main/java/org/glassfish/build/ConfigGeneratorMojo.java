/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
    name = "generate-injectors",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
    threadSafe = true)
public class ConfigGeneratorMojo extends AbstractConfigGeneratorMojo {

    private static final String generatedDirectory;
    static {
        StringBuilder sb = new StringBuilder();
        sb.append(GENERATED_SOURCES).append(File.separatorChar).append(MAIN_NAME).append(File.separatorChar).append(JAVA_NAME);
        generatedDirectory = sb.toString();
    }

    @Parameter(property="project.build.outputDirectory")
    private File outputDirectory;

    @Parameter(property="project.build.sourceDirectory")
    private File sourceDirectory;


    @Override
    protected File getSourceDirectory() {
        return sourceDirectory;
    }


    @Override
    protected File getGeneratedDirectory() {
        return new File(project.getBuild().getDirectory(), generatedDirectory);
    }


    @Override
    protected File getOutputDirectory() {
        return outputDirectory;
    }


    @Override
    protected void addCompileSourceRoot(String path) {
        project.addCompileSourceRoot(path);
    }
}
