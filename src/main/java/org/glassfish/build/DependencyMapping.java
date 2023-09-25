/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * Copyright (c) 2017, 2022 Oracle and/or its affiliates. All rights reserved.
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

/**
 * Configuration of dependency mapping to name.
 * This allows customizing names of dependencies unpacked.
 */
public final class DependencyMapping {

    /**
     * The groupId of the dependency.
     */
    private String groupId;

    /**
     * The artifacTId of the dependency.
     */
    private String artifactId;

    /**
     * The mapped name of the dependency.
     */
    private String name;

    /**
     * Set the artifactId of the dependency.
     *
     * @param depArtifactId the artifactId
     */
    public void setArtifactId(final String depArtifactId) {
        this.artifactId = depArtifactId;
    }


    /**
     * Get the artifactId of the dependency.
     *
     * @return the artifactId
     */
    public String getArtifactId() {
        return artifactId;
    }


    /**
     * Set the groupId of the dependency.
     *
     * @param depGroupId the groupId
     */
    public void setGroupId(final String depGroupId) {
        this.groupId = depGroupId;
    }


    /**
     * Get the groupId of the dependency.
     *
     * @return the groupId
     */
    public String getGroupId() {
        return groupId;
    }


    /**
     * Set the mapped name of the dependency.
     *
     * @param depName the mapped name
     */
    public void setName(final String depName) {
        this.name = depName;
    }


    /**
     * Get the mapped name of the dependency.
     *
     * @return the groupId
     */
    public String getName() {
        return name;
    }
}
