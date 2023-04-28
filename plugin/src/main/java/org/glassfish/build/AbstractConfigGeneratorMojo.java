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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.glassfish.build.hk2.config.generator.ConfigInjectorGenerator;

/**
 * @author jwells
 *
 * Abstract Mojo for config generator
 */
public abstract class AbstractConfigGeneratorMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;

    @Parameter(property = "supportedProjectTypes", defaultValue = "jar,glassfish-jar")
    private String supportedProjectTypes;

    @Parameter(property="includes", defaultValue = "**/*.java")
    private String includes;

    @Parameter(property="excludes", defaultValue = "")
    private String excludes;

    protected abstract List<String> getCompileSourceRoots();

    protected abstract File getGeneratedDirectory();

    /** Make the generated source directory visible for compilation */
    protected abstract void addCompileSourceRoot(String path);


    protected abstract boolean skip();


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip()) {
            getLog().info("Skipped.");
            return;
        }
        try {
            internalExecute();
        } catch (IOException th) {
            throw new MojoExecutionException(th.getMessage(), th);
        }
    }


    private void internalExecute() throws IOException, MojoExecutionException {
        List<String> projectTypes = Arrays.asList(supportedProjectTypes.split(","));
        if (!projectTypes.contains(project.getPackaging())) {
            getLog().debug("Project type " + project.getPackaging() + " is not configured to use this plugin.");
            return;
        }
        if (!getGeneratedDirectory().exists() && !getGeneratedDirectory().mkdirs()) {
            throw new IOException("Could not create the directory " + getGeneratedDirectory().getAbsolutePath());
        }

        List<String> fileNames = new ArrayList<>();
        for (String path : getCompileSourceRoots()) {
            File directory = new File(path);
            if (directory.exists()) {
                fileNames.addAll(FileUtils.getFileNames(directory, includes, excludes, true));
            }
        }
        if (fileNames.isEmpty()) {
            getLog().info("No sources, nothing to generate.");
            return;
        }

        List<String> options = new ArrayList<>();
        options.add("-proc:only");
        if (getLog().isDebugEnabled()) {
            options.add("-verbose");
        }
        options.add("-s");
        options.add(getGeneratedDirectory().getAbsolutePath());
        options.add("-cp");
        options.add(getBuildClasspath());
        getLog().info("Generating to " + getGeneratedDirectory());

        if (getLog().isDebugEnabled()) {
            getLog().debug("-- AnnotationProcessing Command Line --");
            getLog().debug(options.toString());
            getLog().debug(fileNames.toString());
        }
        StringBuilderWriter logs = getLog().isDebugEnabled() ? new StringBuilderWriter() : null;
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        Iterable<? extends JavaFileObject> files = fileManager.getJavaFileObjectsFromStrings(fileNames);
        JavaCompiler.CompilationTask task = compiler.getTask(logs, fileManager, diagnostics, options, null, files);
        task.setProcessors(Collections.singleton(new ConfigInjectorGenerator()));

        boolean compilationResult = task.call();
        if (!compilationResult) {
            if (logs != null) {
                // All info we have from the compiler
                getLog().debug(logs.toString());
            }
            // Just errors
            for (Diagnostic<? extends JavaFileObject> diag : diagnostics.getDiagnostics()) {
                getLog().error(diag.getMessage(Locale.getDefault()));
            }
            throw new MojoExecutionException("Annotation processing failed, sources were NOT generated!");
        }

        addCompileSourceRoot(getGeneratedDirectory().getAbsolutePath());
        if (getLog().isDebugEnabled()) {
            getLog().debug(
                "Sources generated in the directory " + getGeneratedDirectory() + " were registered to compilation.");
        }
    }


    private String getBuildClasspath() {
        StringBuilder sb = new StringBuilder();
        sb.append(project.getBuild().getOutputDirectory());
        sb.append(File.pathSeparator);

        List<Artifact> artList = new ArrayList<>(project.getArtifacts());
        Iterator<Artifact> i = artList.iterator();
        if (i.hasNext()) {
            sb.append(i.next().getFile().getPath());
            while (i.hasNext()) {
                sb.append(File.pathSeparator);
                sb.append(i.next().getFile().getPath());
            }
        }

        String classpath = sb.toString();
        if (getLog().isDebugEnabled()) {
            getLog().debug("-- Classpath --");
            getLog().debug(classpath);
        }
        return classpath;
    }
}
