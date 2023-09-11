/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * Copyright (c) 2012, 2019 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019 Payara Services Ltd.
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
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.Environment.Variable;

/**
 * Execute a command.
 */
@Mojo(name = "exec",
      requiresProject = true,
      requiresDependencyResolution = ResolutionScope.RUNTIME,
      defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public final class ExecMojo extends AbstractMojo {

    /**
     * The maven project.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * Executable to execute.
     */
    @Parameter(property = "executable")
    private String executable;

    /**
     * Working dir.
     */
    @Parameter(property = "workingDir",
            defaultValue = "${project.build.directory}")
    private File workingDir;

    /**
     * Command line argument.
     */
    @Parameter(property = "commandlineArgs")
    private String commandlineArgs;

    /**
     * Plugin should return failure if an error occurred.
     */
    @Parameter(property = "failOnError", defaultValue = "true")
    private Boolean failOnError;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        Project antProject = new Project();
        antProject.addBuildListener(new AntBuildListener());

        Properties mavenProperties = project.getProperties();
        Iterator it = mavenProperties.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            antProject.setProperty(key, mavenProperties.getProperty(key));
        }
        ExecTask exec = new ExecTask();
        exec.setProject(antProject);
        exec.setDir(workingDir);

        if (new Os("Windows").eval()
                && !executable.endsWith(".bat")
                && new File(executable + ".bat").exists()) {
            executable += ".bat";
        }

        exec.setExecutable(executable);
        getLog().info("executable: " + executable);
        exec.createArg().setLine(commandlineArgs);
        exec.setFailonerror(failOnError);
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null) {
            exec.addEnv(createVariable("AS_JAVA", javaHome));
            exec.addEnv(createVariable("JAVA_HOME", javaHome));
        }
        getLog().info("commandLineArgs: " + commandlineArgs);
        exec.execute();
    }

    private Variable createVariable(final String key, final String value) {
        final Variable variable = new Variable();
        variable.setKey(key);
        variable.setValue(value);
        return variable;
    }

    /**
     * {@code BuilderListener} implementation to log Ant events.
     */
    private final class AntBuildListener implements BuildListener {

        /**
         * Maximum Event priority that is logged.
         */
        private static final int MAX_EVENT_PRIORITY = 3;

        @Override
        public void buildStarted(final BuildEvent event) {
        }

        @Override
        public void buildFinished(final BuildEvent event) {
        }

        @Override
        public void targetStarted(final BuildEvent event) {
        }

        @Override
        public void targetFinished(final BuildEvent event) {
        }

        @Override
        public void taskStarted(final BuildEvent event) {
        }

        @Override
        public void taskFinished(final BuildEvent event) {
        }

        @Override
        public void messageLogged(final BuildEvent event) {
            if (event.getPriority() < MAX_EVENT_PRIORITY) {
                getLog().info("[exec] " + event.getMessage());
            } else {
                getLog().debug("[exec] " + event.getMessage());
            }
        }
    }
}
