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
import java.nio.file.Path;
import java.util.List;

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

    private static final Path GENERATED_DIR = Path.of("generated-sources", "hk2-config-generator");

    @Parameter(property="project.build.outputDirectory")
    private File outputDirectory;

    @Parameter(property = "glassfish.generate-injectors.skip", defaultValue = "false")
    private boolean skip;

    @Override
    protected List<String> getCompileSourceRoots() {
        return project.getCompileSourceRoots();
    }


    @Override
    protected File getGeneratedDirectory() {
        return new File(project.getBuild().getDirectory()).toPath().resolve(GENERATED_DIR).toFile();
    }


    @Override
    protected void addCompileSourceRoot(String path) {
        project.addCompileSourceRoot(path);
    }


    @Override
    protected boolean skip() {
        return skip;
    }
}
