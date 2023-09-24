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

package org.glassfish.build.xpp3dom;

import org.apache.maven.lifecycle.mapping.LifecycleMojo;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * The <code>configuration</code> element of the plugin.
 */
public final class ConfigurationElement extends Xpp3Dom {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the <code>configuration</code> element of the plugin.
     */
    public ConfigurationElement() {
        super("configuration");
    }


    /**
     * Creates new {@link ConfigurationElement} object.
     * If mojo container configuration, it is merged to the new object.
     *
     * @param mojo plugin mojo
     * @return new ConfigurationElement
     */
    public static ConfigurationElement getOrCreateConfiguration(final LifecycleMojo mojo) {
        final Xpp3Dom original = mojo.getConfiguration();
        final ConfigurationElement cfg = new ConfigurationElement();
        if (original == null) {
            return cfg;
        }
        return (ConfigurationElement) Xpp3Dom.mergeXpp3Dom(cfg, original);
    }
}
