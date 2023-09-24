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
import org.apache.maven.lifecycle.mapping.Lifecycle;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.lifecycle.mapping.LifecyclePhase;
import org.codehaus.plexus.component.annotations.Component;

@Component(role = LifecycleMapping.class, hint = "glassfish-jar")
public class GlassFishJarLifecycle extends DefaultLifecycleMapping {

    public GlassFishJarLifecycle() {
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
        phases.put("compile", new LifecyclePhase("org.apache.maven.plugins:maven-compiler-plugin:compile"));
        phases.put("process-classes",
            new LifecyclePhase("org.glassfish.hk2:osgiversion-maven-plugin:compute-osgi-version,"
                + "org.glassfish.hk2:hk2-inhabitant-generator:generate-inhabitants,"
                + "org.apache.felix:maven-bundle-plugin:manifest,"
                + "org.glassfish.build:command-security-maven-plugin:check"));
        phases.put("test-compile",
            new LifecyclePhase("org.apache.maven.plugins:maven-compiler-plugin:testCompile"));
        phases.put("process-test-resources",
            new LifecyclePhase("org.apache.maven.plugins:maven-resources-plugin:testResources"));
        phases.put("process-test-classes",
            new LifecyclePhase("org.glassfish.hk2:hk2-inhabitant-generator:generate-test-inhabitants"));
        phases.put("test", new LifecyclePhase("org.apache.maven.plugins:maven-surefire-plugin:test"));
        phases.put("package", new LifecyclePhase("org.apache.maven.plugins:maven-jar-plugin:jar"));
        phases.put("install", new LifecyclePhase("org.apache.maven.plugins:maven-install-plugin:install"));
        phases.put("deploy", new LifecyclePhase("org.apache.maven.plugins:maven-deploy-plugin:deploy"));
        return phases;
    }
}
