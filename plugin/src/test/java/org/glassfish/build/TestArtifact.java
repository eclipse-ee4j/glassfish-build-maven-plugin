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

import org.eclipse.aether.artifact.AbstractArtifact;

import java.io.File;
import java.util.Map;

class TestArtifact extends AbstractArtifact {
    private final String artifactId;
    private final String groupId;
    private final String version;
    private final String extension;

    TestArtifact(String groupId, String artifactId, String version, String extension) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.extension = extension;
    }

    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getClassifier() {
        return "";
    }

    @Override
    public String getExtension() {
        return extension;
    }

    @Override
    public File getFile() {
        return null;
    }

    @Override
    public Map<String, String> getProperties() {
        return null;
    }
}
