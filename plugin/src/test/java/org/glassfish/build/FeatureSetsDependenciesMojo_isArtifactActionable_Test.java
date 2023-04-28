/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * Copyright (c) 2022 Contributors to Eclipse Foundation. All rights reserved.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.codehaus.plexus.logging.Logger.LEVEL_WARN;
import static org.glassfish.build.FeatureSetsDependenciesMojo.isArtifactActionable;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatureSetsDependenciesMojo_isArtifactActionable_Test {
    private final ArtifactResult artifact = new ArtifactResult(new ArtifactRequest());
    private final Logger logger = new ConsoleLogger(LEVEL_WARN, "console");
    private final Log log = new DefaultLog(logger);

    @BeforeEach
    void setUpArtifact() {
        Artifact warArtifact = new TestArtifact(
                "org.glassfish.build.test",
                "war",
                "1.2.3",
                "war");
        artifact.setArtifact(warArtifact);
    }
    @Test
    void testNotIncludedWarType() {
        List<String> actionTypes = Arrays.asList("jar", "rar");
        List<String> actionExcludes = Collections.emptyList();

        boolean actionable = isArtifactActionable(artifact, actionTypes, actionExcludes, log);

        assertFalse(actionable,
                "Artifact of type [war] not included in action types [jar, rar] " +
                        "should not be actionable.");
    }

    @Test
    void testExcludedWar() {
        List<String> actionTypes = Arrays.asList("jar", "war");
        List<String> actionExcludes = Arrays.asList("war");

        boolean actionable = isArtifactActionable(artifact, actionTypes, actionExcludes, log);

        assertFalse(actionable,
                "Artifact of type [war] included in action types [jar, war] " +
                        "but excluded from action " +
                        "should not be actionable.");
    }

    @Test
    void testNotExcludedWar() {
        List<String> actionTypes = Arrays.asList("jar", "war");
        List<String> actionExcludes = Collections.emptyList();

        boolean actionable = isArtifactActionable(artifact, actionTypes, actionExcludes, log);

        assertTrue(actionable,
                "Artifact of type [war] included in action types [jar, war] " +
                        "and not excluded from action " +
                        "should be actionable.");
    }
}
