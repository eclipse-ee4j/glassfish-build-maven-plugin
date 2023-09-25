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

import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 *  The <code>descriptors</code> element group, used to configure the assembly-maven-plugin.
 */
public class AssemblyDescriptorElements extends Xpp3Dom {

    private static final long serialVersionUID = 1L;

    /**
     * @param descriptors
     */
    public AssemblyDescriptorElements(final String... descriptors) {
        super("descriptors");
        for (final String descriptor : descriptors) {
            addChild(new PropertyElement("descriptor", descriptor));
        }
    }
}
