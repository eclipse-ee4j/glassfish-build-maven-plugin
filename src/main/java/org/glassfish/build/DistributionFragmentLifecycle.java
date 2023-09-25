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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.lifecycle.mapping.DefaultLifecycleMapping;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.lifecycle.mapping.LifecycleMojo;
import org.apache.maven.lifecycle.mapping.LifecyclePhase;
import org.codehaus.plexus.component.annotations.Component;

import static org.glassfish.build.LifecyclePhaseHelper.createAssemblyCfg;
import static org.glassfish.build.LifecyclePhaseHelper.createGenerateResourcesPhase;
import static org.glassfish.build.LifecyclePhaseHelper.createLifecycle;
import static org.glassfish.build.LifecyclePhaseHelper.createSetMainArtifactCfg;

/**
 * Lifecycle of the distribution-fragment package type.
 */
@Component(role = LifecycleMapping.class, hint = "distribution-fragment")
public class DistributionFragmentLifecycle extends DefaultLifecycleMapping {

    private static final String DESCRIPTOR_FILENAME = "distribution-fragment.xml";

    /**
     * Creates a preconfigured lifecycle.
     */
    public DistributionFragmentLifecycle() {
        super(List.of(createLifecycle(DistributionFragmentLifecycle::createPhases)));
    }


    private static Map<String, LifecyclePhase> createPhases() {
        final Map<String, LifecyclePhase> phases = new HashMap<>();
        phases.put("generate-resources", createGenerateResourcesPhase(DESCRIPTOR_FILENAME));
        phases.put("process-resources",
            new LifecyclePhase("org.apache.maven.plugins:maven-resources-plugin:resources"));
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
        assemblyMojo.setConfiguration(createAssemblyCfg(assemblyMojo, DESCRIPTOR_FILENAME));

        final LifecycleMojo setMainArtifactMojo = packagePhase.getMojos().get(1);
        setMainArtifactMojo.setConfiguration(createSetMainArtifactCfg(setMainArtifactMojo));
        return packagePhase;
    }
}
