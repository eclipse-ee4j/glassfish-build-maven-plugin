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
import java.util.Map;
import java.util.function.Supplier;

import org.apache.maven.lifecycle.mapping.Lifecycle;
import org.apache.maven.lifecycle.mapping.LifecycleMojo;
import org.apache.maven.lifecycle.mapping.LifecyclePhase;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.glassfish.build.xpp3dom.AssemblyDescriptorElements;
import org.glassfish.build.xpp3dom.ConfigurationElement;
import org.glassfish.build.xpp3dom.PropertyElement;

import static org.glassfish.build.xpp3dom.ConfigurationElement.getOrCreateConfiguration;

/**
 * Utilities to simplify {@link LifecyclePhase} construction.
 */
final class LifecyclePhaseHelper {

    static final String GFBUILD_ASSEMBLY_DIR = "target/gfbuild-maven-plugin-assembly";

    private LifecyclePhaseHelper() {
        // hidden
    }


    static Lifecycle createLifecycle(final Supplier<Map<String, LifecyclePhase>> phasesSupplier) {
        final Lifecycle lifecycle = new Lifecycle();
        lifecycle.setId("default");
        lifecycle.setLifecyclePhases(phasesSupplier.get());
        return lifecycle;
    }


    static LifecyclePhase createGenerateResourcesPhase(final String descriptorResource) {
        final LifecyclePhase phase = new LifecyclePhase(
            "org.glassfish.build:glassfishbuild-maven-plugin:unpack-assembly-descriptor");
        final LifecycleMojo mojo = phase.getMojos().get(0);
        final ConfigurationElement cfg = getOrCreateConfiguration(mojo);
        cfg.addChild(new PropertyElement("descriptorResource", descriptorResource));
        mojo.setConfiguration(cfg);
        return phase;
    }


    static Xpp3Dom createSetMainArtifactCfg(final LifecycleMojo mojo) {
        final ConfigurationElement cfg = getOrCreateConfiguration(mojo);
        cfg.addChild(new PropertyElement("file",
            "${project.build.directory}" + File.separatorChar + "${project.build.finalName}.zip"));
        cfg.addChild(new PropertyElement("type", "zip"));
        return cfg;
    }


    /**
     * The assembly plugin attaches the artifact, but doesn't set it as main artifact except for pom
     * types. The install plugin then fails the build OR if configured, prints a warning.
     * In our case we have just one artifact to be installed and deployed.
     *
     * @param assemblyMojo the assembly-maven-plugin mojo used as a possible source of the
     *            configuration. If there is none, this method creates it.
     * @param descriptorFilename descriptor file name to be unpacked and used by the assembly plugin.
     * @return {@link ConfigurationElement} for the maven assembly plugin mojo.
     */
    static ConfigurationElement createAssemblyCfg(final LifecycleMojo assemblyMojo, final String descriptorFilename) {
        final ConfigurationElement cfg = getOrCreateConfiguration(assemblyMojo);
        cfg.addChild(new PropertyElement("appendAssemblyId", "false"));
        cfg.addChild(new PropertyElement("attach", "false"));
        cfg.addChild(new AssemblyDescriptorElements(GFBUILD_ASSEMBLY_DIR + File.separatorChar + descriptorFilename));
        return cfg;
    }
}
