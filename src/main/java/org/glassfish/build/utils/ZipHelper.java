/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * Copyright (c) 2023 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.build.utils;

import java.io.File;
import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.apache.maven.plugin.logging.Log;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.taskdefs.Zip.Duplicate;
import org.apache.tools.ant.types.ZipFileSet;

/**
 * Helper to create zip files using ant.
 */
final class ZipHelper {

    /**
     * Create a new {@code ZipHelper} instance.
     */
    private ZipHelper() {
    }

    /**
     * Lazy singleton holder.
     */
    private static final class LazyHolder {

        /**
         * The singleton instance.
         */
        static final ZipHelper INSTANCE = new ZipHelper();
    }

    /**
     * Get the Singleton instance for {@code ZipHelper}.
     * @return the {@code ZipHelper} instance
     */
    static ZipHelper getInstance() {
        return LazyHolder.INSTANCE;
    }

    /**
     * Create a zip file.
     * @param properties Ant project properties
     * @param mavenLog Maven logger
     * @param duplicate behavior for duplicate file, one of "add", "preserve"
     * or "fail"
     * @param fsets list of {@code ZipFileSet} that describe the resources to
     * zip
     * @param target the {@code File} instance for the zip file to create
     * @param timestamp optional reproducible build timestamp
     */
    void zip(final Properties properties,
            final Log mavenLog,
            final String duplicate,
            final List<ZipFileSet> fsets,
            final File target,
            final Optional<Instant> timestamp) {

        Project antProject = new Project();
        antProject.addBuildListener(new AntBuildListener(mavenLog));
        Iterator it = properties.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            antProject.setProperty(key, properties.getProperty(key));
        }

        Zip zip = new Zip();
        zip.setProject(antProject);
        zip.setDestFile(target);
        Duplicate df = new Duplicate();
        df.setValue(duplicate);
        zip.setDuplicate(df);
        mavenLog.info(String.format("[zip] duplicate: %s", duplicate));
        if (timestamp.isPresent()) {
            zip.setModificationtime(timestamp.get().toString());
        }

        List<ZipFileSet> filesets;
        if (fsets == null) {
            filesets = Collections.EMPTY_LIST;
        } else {
            filesets = fsets;
        }

        if (filesets.isEmpty()) {
            ZipFileSet zfs = MavenHelper.createZipFileSet(new File(""), "", "");
            // work around for
            // http://issues.apache.org/bugzilla/show_bug.cgi?id=42122
            zfs.setDirMode("755");
            zfs.setFileMode("644");
            filesets.add(zfs);
        }

        for (ZipFileSet fset : filesets) {
            zip.addZipfileset(fset);
            String desc = fset.getDescription();
            if (desc != null && !desc.isEmpty()) {
                mavenLog.info(String.format("[zip] %s", desc));
            }
        }
        zip.executeMain();
    }

    /**
     * {@code BuilderListener} implementation to log Ant events.
     */
    private static final class AntBuildListener implements BuildListener {

        /**
         * Maximum Event priority that is logged.
         */
        private static final int MAX_EVENT_PRIORITY = 3;

        /**
         * Maven logger.
         */
        private final Log log;

        /**
         * Create a new {@code AntBuildListener} instance.
         * @param mavenLog Maven logger
         */
        private AntBuildListener(final Log mavenLog) {
            this.log = mavenLog;
        }

        @Override
        public void buildStarted(final BuildEvent event) {
        }

        @Override
        public void buildFinished(final BuildEvent event) {
        }

        @Override
        public void targetStarted(final BuildEvent event) {
        }

        @Override
        public void targetFinished(final BuildEvent event) {
        }

        @Override
        public void taskStarted(final BuildEvent event) {
        }

        @Override
        public void taskFinished(final BuildEvent event) {
        }

        @Override
        public void messageLogged(final BuildEvent event) {
            if (event.getPriority() < MAX_EVENT_PRIORITY) {
                log.info(String.format("[zip] %s", event.getMessage()));
            } else {
                log.debug(String.format("[zip] %s", event.getMessage()));
            }
        }
    }
}
