/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation. All rights reserved.
 * Copyright (c) 2008, 2018 Oracle and/or its affiliates. All rights reserved.
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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Echo a message.
 */
@Mojo(name = "echo", threadSafe = true)
public final class EchoMojo extends AbstractMojo {

    /**
     * Any String to print out.
     */
    @Parameter(property = "message")
    private String message;

    @Override
    @SuppressWarnings("checkstyle:LineLength")
    public void execute() throws MojoExecutionException,  MojoFailureException {
        getLog().info("------------------------------------------------------------------------");
        getLog().info(message);
        getLog().info("------------------------------------------------------------------------");
    }
}
