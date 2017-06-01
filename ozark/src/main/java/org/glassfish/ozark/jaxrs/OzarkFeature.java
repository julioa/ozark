/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.ozark.jaxrs;

import org.glassfish.ozark.cdi.ValidationInterceptor;
import org.glassfish.ozark.core.ViewRequestFilter;
import org.glassfish.ozark.core.ViewResponseFilter;
import org.glassfish.ozark.core.ViewableWriter;
import org.glassfish.ozark.locale.LocaleRequestFilter;
import org.glassfish.ozark.security.CsrfProtectFilter;
import org.glassfish.ozark.security.CsrfValidateInterceptor;

import javax.annotation.Priority;
import javax.mvc.annotation.Controller;
import javax.mvc.engine.Priorities;
import javax.servlet.ServletContext;
import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.FeatureContext;
import java.util.Arrays;
import java.util.logging.Logger;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.ext.Provider;
import org.glassfish.ozark.jersey.OzarkModelProcessor;

import static org.glassfish.ozark.util.AnnotationUtils.getAnnotation;

/**
 * <p>
 * JAX-RS feature that sets up the JAX-RS pipeline for MVC processing using one or more providers. This feature is enabled only
 * if any of the classes or methods in the application has an instance of the {@link javax.mvc.annotation.Controller}
 * annotation.</p>
 *
 * @author Santiago Pericas-Geertsen
 * @author Eddú Meléndez
 * @author Dmytro Maidaniuk
 */
@ConstrainedTo(RuntimeType.SERVER)
@Priority(Priorities.FRAMEWORK)
@Provider
public class OzarkFeature implements DynamicFeature {

    private static final Logger LOG = Logger.getLogger(OzarkFeature.class.getName());

    @Context
    private ServletContext servletContext;

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        final Configuration config = context.getConfiguration();
        if (config.isRegistered(ViewResponseFilter.class)) {
            return;     // already registered!
        }

        boolean classAnnotation = resourceInfo.getResourceClass().isAnnotationPresent(Controller.class);
        boolean methodAnnotation = resourceInfo.getResourceMethod().isAnnotationPresent(Controller.class);
        
        boolean enableOzark = false;

//        enableOzark = config.getClasses().stream().anyMatch(this::isController)
//                || config.getInstances().stream().map(o -> o.getClass()).anyMatch(this::isController);
        
        enableOzark = enableOzark || classAnnotation || methodAnnotation;
        
        if (enableOzark) {
            context.register(ViewRequestFilter.class);
            context.register(ViewResponseFilter.class);
            //context.register(ViewableWriter.class);
            context.register(ValidationInterceptor.class);
            //context.register(OzarkModelProcessor.class);
            context.register(CsrfValidateInterceptor.class);
            context.register(CsrfProtectFilter.class);
            context.register(LocaleRequestFilter.class);
        }
    }

//    private boolean isController(Class<?> c) {
//        return getAnnotation(c, Controller.class) != null
//                || Arrays.stream(c.getMethods()).anyMatch(m -> getAnnotation(m, Controller.class) != null);
//    }
}
