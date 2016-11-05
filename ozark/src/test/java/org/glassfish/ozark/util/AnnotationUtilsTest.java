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
package org.glassfish.ozark.util;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mvc.annotation.Controller;
import javax.mvc.annotation.View;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * The JUnit tests for the AnnotationUtils class.
 *
 * @author Florian Hirsch
 */
@RunWith(CdiTestRunner.class)
public class AnnotationUtilsTest {

	@Inject
	private SomeController someController; // not proxied

	@Inject
	private SomeBean someBean; // proxied

	@Test
	public void getAnnotation() {
		Path path = AnnotationUtils.getAnnotation(someController.getClass(), Path.class);
		assertThat(path.value(), is("start"));
		Named named = AnnotationUtils.getAnnotation(someBean.getClass(), Named.class);
		assertThat(named.value(), is("someBean"));
        assertThat(AnnotationUtils.getAnnotation(BaseController.class, Controller.class), is(notNullValue()));
        // inheritance of class or interface annotations is not supported
        assertThat(AnnotationUtils.getAnnotation(InheritedController.class, Controller.class), is(nullValue()));
        assertThat(AnnotationUtils.getAnnotation(ControllerImpl.class, Controller.class), is(nullValue()));
    }

	@Test
	public void hasAnnotation() {
		assertTrue(AnnotationUtils.hasAnnotation(someController.getClass(), Path.class));
		assertFalse(AnnotationUtils.hasAnnotation(someController.getClass(), Named.class));
		assertTrue(AnnotationUtils.hasAnnotation(someBean.getClass(), Named.class));
		assertFalse(AnnotationUtils.hasAnnotation(someBean.getClass(), Path.class));
        assertTrue(AnnotationUtils.hasAnnotation(BaseController.class, Controller.class));
        // inheritance of class or interface annotations is not supported
        assertFalse(AnnotationUtils.hasAnnotation(InheritedController.class, Controller.class));
        assertFalse(AnnotationUtils.hasAnnotation(ControllerImpl.class, Controller.class));
	}

	@Test
	public void getAnnotationOnMethod() throws NoSuchMethodException {
		View view = AnnotationUtils.getAnnotation(someController.getClass().getMethod("start"), View.class);
		assertThat(view.value(), is("start.jsp"));
		NotNull notNull = AnnotationUtils.getAnnotation(someBean.getClass().getMethod("notNull"), NotNull.class);
		assertThat(notNull.message(), is("notNull"));
        assertThat(AnnotationUtils.getAnnotation(BaseController.class.getMethod("start"), View.class), is(notNullValue()));
        assertThat(AnnotationUtils.getAnnotation(InheritedController.class.getMethod("start"), View.class), is(notNullValue()));
        assertThat(AnnotationUtils.getAnnotation(ControllerImpl.class.getMethod("start"), View.class), is(notNullValue()));
        View inheritedView = AnnotationUtils.getAnnotation(InheritedControllerImpl.class.getMethod("start"), View.class);
        // Annotations on a super-class take precedence over those on an implemented interface
        assertThat(inheritedView.value(), is("start-base.jsp"));
        // if a subclass or implementation method has any MVC or JAX-RS annotations
        // then all of the annotations on the superclass or interface method are ignored
        assertThat(AnnotationUtils.getAnnotation(NoInheritanceController.class.getMethod("start"), Path.class), is(nullValue()));
	}

	@Test
	public void hasAnnotationOnMethod() throws NoSuchMethodException {
		assertTrue(AnnotationUtils.hasAnnotation(someController.getClass().getMethod("start"), View.class));
		assertFalse(AnnotationUtils.hasAnnotation(someController.getClass().getMethod("start"), NotNull.class));
		assertTrue(AnnotationUtils.hasAnnotation(someBean.getClass().getMethod("notNull"), NotNull.class));
		assertFalse(AnnotationUtils.hasAnnotation(someBean.getClass().getMethod("notNull"), View.class));
        assertTrue(AnnotationUtils.hasAnnotation(BaseController.class.getMethod("start"), View.class));
        assertTrue(AnnotationUtils.hasAnnotation(InheritedController.class.getMethod("start"), View.class));
        assertTrue(AnnotationUtils.hasAnnotation(ControllerImpl.class.getMethod("start"), View.class));
        // if a subclass or implementation method has any MVC or JAX-RS annotations
        // then all of the annotations on the superclass or interface method are ignored
        assertFalse(AnnotationUtils.hasAnnotation(NoInheritanceController.class.getMethod("start"), Path.class));
	}

	@Controller
	@Path("start")
	static class SomeController {
		@View("start.jsp") public void start() {}
	}

	@Named("someBean")
	@RequestScoped
	static class SomeBean {
		@NotNull(message = "notNull") public String notNull() {
            return "something";
        }
	}

    @Controller
    private static class BaseController {
        @View("start-base.jsp") @Path("/base") public void start() {}
    }

    private static class InheritedController extends BaseController {}

    @Controller
    private interface ControllerInterface {
        @View("start-interface.jsp") void start();
    }

    private static class ControllerImpl implements ControllerInterface {
        public void start() {}
    }

    private static class InheritedControllerImpl extends BaseController implements ControllerInterface {}

    private static class NoInheritanceController extends BaseController {
        @View("start.jsp") public void start() {}
    }

}




