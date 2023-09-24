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

@Component(role = LifecycleMapping.class, hint = "distribution-fragment")
public class DistributionFragmentLifecycle extends DefaultLifecycleMapping {

    public DistributionFragmentLifecycle() {
        super(List.of(createLifecycle()));
    }


    private static Lifecycle createLifecycle() {
        Lifecycle lifecycle = new Lifecycle();
        lifecycle.setId("default");
        lifecycle.setLifecyclePhases(createPhases());
        return lifecycle;
    }


    private static Map<String, LifecyclePhase> createPhases() {
        final Map<String, LifecyclePhase> phases = new HashMap<>();
        phases.put("process-resources",
            new LifecyclePhase("org.apache.maven.plugins:maven-resources-plugin:resources"));
        phases.put("compile", new LifecyclePhase(""));
        phases.put("package", createPackagePhase());
        phases.put("install", new LifecyclePhase("org.apache.maven.plugins:maven-install-plugin:install"));
        phases.put("deploy", new LifecyclePhase("org.apache.maven.plugins:maven-deploy-plugin:deploy"));
        return phases;
    }


    private static LifecyclePhase createPackagePhase() {
        final LifecyclePhase packagePhase = new LifecyclePhase("org.apache.maven.plugins:maven-assembly-plugin:single,"
            + "org.glassfish.build:glassfishbuild-maven-plugin:set-main-artifact");
        final LifecycleMojo assemblyMojo = packagePhase.getMojos().get(0);
        assemblyMojo.setConfiguration(createAssemblyCfg(assemblyMojo));
        assemblyMojo.setDependencies(createAssemblyDependencies(assemblyMojo));
        final LifecycleMojo setMainArtifactMojo = packagePhase.getMojos().get(1);
        setMainArtifactMojo.setConfiguration(createSetMainArtifactCfg(setMainArtifactMojo));
        return packagePhase;
    }


    private static List<Dependency> createAssemblyDependencies(LifecycleMojo assemblyMojo) {
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
        // TODO: use inline descriptor instead of the reference.
        final Xpp3Dom appendAssemblyId = createNameValueDom("appendAssemblyId", "false");
        // assembly plugin attaches the artifact, but doesn't set it as main artifact except for pom types.
        // install plugin then fails the build OR if configured, prints a warning.
        // In our case we have just one artifact to be installed and deployed.
        final Xpp3Dom attach = createNameValueDom("attach", "false");
        final Xpp3Dom assemblyCfg = getConfiguration(assemblyMojo);
        assemblyCfg.addChild(appendAssemblyId);
        assemblyCfg.addChild(attach);
        assemblyCfg.addChild(createDescriptorRefs());
        return assemblyCfg;
    }


    private static Xpp3Dom createDescriptorRefs() {
        final Xpp3Dom descriptorRef = createNameValueDom("descriptorRef", "distribution-fragment");
        final Xpp3Dom descriptorRefs = createNameChildDom("descriptorRefs", descriptorRef);
        return descriptorRefs;
    }


    private static Xpp3Dom createSetMainArtifactCfg(LifecycleMojo mojo) {
        final Xpp3Dom file = createNameValueDom("file",
            "${project.build.directory}" + File.separatorChar + "${project.build.finalName}.zip");
        final Xpp3Dom type = createNameValueDom("type", "zip");
        final Xpp3Dom cfg = getConfiguration(mojo);
        cfg.addChild(file);
        cfg.addChild(type);
        return cfg;
    }


    private static Xpp3Dom createNameValueDom(String name, String value) {
        final Xpp3Dom type = new Xpp3Dom(name);
        type.setValue(value);
        return type;
    }


    private static Xpp3Dom createNameChildDom(String name, Xpp3Dom... children) {
        final Xpp3Dom type = new Xpp3Dom(name);
        for (Xpp3Dom child : children) {
            type.addChild(child);
        }
        return type;
    }


    private static Xpp3Dom getConfiguration(final LifecycleMojo mojo) {
        return mojo.getConfiguration() == null ? new Xpp3Dom("configuration") : mojo.getConfiguration();
    }


    private static List<Dependency> getDependencies(final LifecycleMojo mojo) {
        return mojo.getDependencies() == null ? new ArrayList<>() : mojo.getDependencies();
    }
}
