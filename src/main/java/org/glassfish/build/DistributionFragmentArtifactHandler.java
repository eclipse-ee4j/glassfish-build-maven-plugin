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

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.codehaus.plexus.component.annotations.Component;

/**
 * Mapping of the distribution-fragment package type with the zip extension.
 */
@Component(role = ArtifactHandler.class, hint = "distribution-fragment")
public class DistributionFragmentArtifactHandler extends DefaultArtifactHandler {

    /**
     * Creates the configured instance.
     */
    public DistributionFragmentArtifactHandler() {
        super("distribution-fragment");
        setExtension("zip");
        setAddedToClasspath(false);
        setLanguage("none");
    }
}
