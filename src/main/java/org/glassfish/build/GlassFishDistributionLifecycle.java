/*
 * Copyright (c) 2023 Eclipse Foundation and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.lifecycle.mapping.DefaultLifecycleMapping;
import org.apache.maven.lifecycle.mapping.Lifecycle;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.lifecycle.mapping.LifecycleMojo;
import org.apache.maven.lifecycle.mapping.LifecyclePhase;
import org.apache.maven.model.Dependency;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.glassfish.build.xpp3dom.AssemblyDescriptorIdElements;
import org.glassfish.build.xpp3dom.PropertyElement;

import static org.glassfish.build.xpp3dom.ConfigurationElement.getOrCreateConfiguration;

/**
 * Lifecycle of the glassfish-distribution package type.
 */
@Component(role = LifecycleMapping.class, hint = "glassfish-distribution")
public class GlassFishDistributionLifecycle extends DefaultLifecycleMapping {

    /**
     * Creates the configured instance.
     */
    public GlassFishDistributionLifecycle() {
        super(List.of(createLifecycle()));
    }


    private static Lifecycle createLifecycle() {
        final Lifecycle lifecycle = new Lifecycle();
        lifecycle.setId("default");
        lifecycle.setLifecyclePhases(createPhases());
        return lifecycle;
    }


    private static Map<String, LifecyclePhase> createPhases() {
        final Map<String, LifecyclePhase> phases = new HashMap<>();
        phases.put("process-resources",
            new LifecyclePhase("org.glassfish.build:glassfishbuild-maven-plugin:featuresets-dependencies"));
        phases.put("package", createPackagePhase());
        phases.put("install", new LifecyclePhase("org.apache.maven.plugins:maven-install-plugin:install"));
        phases.put("deploy", new LifecyclePhase("org.apache.maven.plugins:maven-deploy-plugin:deploy"));
        return phases;
    }


    private static LifecyclePhase createPackagePhase() {
        final LifecyclePhase packagePhase = new LifecyclePhase(
            "org.apache.maven.plugins:maven-assembly-plugin:single,"
            + "org.glassfish.build:glassfishbuild-maven-plugin:set-main-artifact");

        final LifecycleMojo assemblyMojo = packagePhase.getMojos().get(0);
        assemblyMojo.setConfiguration(createAssemblyCfg(assemblyMojo));
        assemblyMojo.setDependencies(createAssemblyDependencies(assemblyMojo));

        final LifecycleMojo setMainArtifactMojo = packagePhase.getMojos().get(1);
        setMainArtifactMojo.setConfiguration(createSetMainArtifactCfg(setMainArtifactMojo));
        return packagePhase;
    }


    private static List<Dependency> createAssemblyDependencies(final LifecycleMojo assemblyMojo) {
        final List<Dependency> dependencies = getDependencies(assemblyMojo);
        final Dependency plugin = new Dependency();
        // FIXME: copy the descriptor out and configure assembly to use it.
        plugin.setArtifactId("glassfishbuild-maven-plugin");
        plugin.setGroupId("org.glassfish.build");
        plugin.setVersion("4.0.0-SNAPSHOT");
        dependencies.add(plugin);
        return dependencies;
    }


    private static Xpp3Dom createAssemblyCfg(final LifecycleMojo assemblyMojo) {
        final Xpp3Dom cfg = getOrCreateConfiguration(assemblyMojo);
        cfg.addChild(new PropertyElement("appendAssemblyId", "false"));
        cfg.addChild(new PropertyElement("attach", "false"));
        cfg.addChild(new AssemblyDescriptorIdElements("glassfish-distribution"));
        return cfg;
    }


    private static Xpp3Dom createSetMainArtifactCfg(final LifecycleMojo mojo) {
        final Xpp3Dom cfg = getOrCreateConfiguration(mojo);
        cfg.addChild(new PropertyElement("file",
            "${project.build.directory}" + File.separatorChar + "${project.build.finalName}.zip"));
        cfg.addChild(new PropertyElement("type", "zip"));
        return cfg;
    }


    private static List<Dependency> getDependencies(final LifecycleMojo mojo) {
        return mojo.getDependencies() == null ? new ArrayList<>() : mojo.getDependencies();
    }
}
