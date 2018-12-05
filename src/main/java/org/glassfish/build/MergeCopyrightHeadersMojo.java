/*
 * Copyright (c) 2013, 2018 Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Merges two property files properly.
 */
@Mojo(name = "merge-copyright-headers")
public final class MergeCopyrightHeadersMojo extends AbstractMojo {

    /**
     * Parameters property prefix.
     */
    private static final String PROPERTY_PREFIX =
            "merge.copyright.headers.outputFile";

    /**
     * The merged file.
     */
    @Parameter(property = PROPERTY_PREFIX + "outputFile",
            defaultValue = "${project.build.directory}/merged.properties")
    private File outputFile;

    /**
     * The files to merge.
     */
    @Parameter(property = PROPERTY_PREFIX + "inputFiles")
    private File[] inputFiles;

    /**
     * Skip this mojo.
     */
    @Parameter(property = PROPERTY_PREFIX + "skip",
            defaultValue = "false")
    private Boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (skip) {
            getLog().info("Skipping file merge ...");
            return;
        }

        BufferedReader br1 = null, br2 = null;
        BufferedWriter writer = null;

        try {
            String line;
            StringBuilder sb = new StringBuilder();

            if (inputFiles != null && inputFiles.length > 1) {

                File file1 = inputFiles[0];
                getLog().info("Reading input file:"
                        + file1.getAbsolutePath());

                // Get contents of file1
                try {

                    br1 = new BufferedReader(new FileReader(file1));
                    while ((line = br1.readLine()) != null) {
                        sb.append(line);
                        sb.append(System.lineSeparator());
                    }
                } finally {
                    if (br1 != null) {
                        br1.close();
                    }
                }

                for (int i = 1; i < inputFiles.length; i++) {

                    File file2 = inputFiles[i];
                    getLog().info("Reading input file:"
                            + file2.getAbsolutePath());

                    // Get contents of file2 and skip the comments
                    try {
                        br2 = new BufferedReader(new FileReader(file2));
                        while ((line = br2.readLine()) != null) {
                            line = line.trim();
                            if (line.startsWith("#")) {
                                continue;
                            }
                            sb.append(line);
                            sb.append(System.lineSeparator());
                        }
                    } finally {
                        if (br2 != null) {
                            br2.close();
                        }
                    }
                }

                // Initialize the writer and write the merged contents
                try {
                    writer = new BufferedWriter(new FileWriter(outputFile));
                    writer.write(sb.toString());
                    writer.flush();
                } finally {
                    if (writer != null) {
                        writer.close();
                    }
                }
            }
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }
}
