/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.ozark.binding;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import javax.inject.Inject;
import javax.mvc.MvcContext;
import javax.mvc.binding.ValidationError;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.mvc.annotation.Controller;
import javax.validation.Validator;
import javax.validation.executable.ExecutableValidator;

import static org.glassfish.ozark.binding.BindingResultUtils.updateBindingResultViolations;

/**
 * CDI backed interceptor to handle validation and binding issues.
 *
 * @author Santiago Pericas-Geertsen
 * @author Jakub Podlesak
 * @author Dmytro Maidaniuk
 */
@Controller
@Interceptor
public class BindingInterceptorImpl implements Serializable {

    private static final long serialVersionUID = -5804986456381504613L;

    private static final Logger LOG = Logger.getLogger(BindingInterceptorImpl.class.getName());

    @Inject
    Validator validator;

    @Inject
    private ConstraintViolationTranslator violationTranslator;

    @Inject
    private MvcContext mvcContext;

    @AroundInvoke
    public Object validateMethodInvocation(InvocationContext ctx) throws Exception {

        LOG.info("Started validation interceptor");
        ExecutableValidator executableValidator = validator.forExecutables();
        Object resource = ctx.getTarget();
        final BindingResultImpl bindingResult = null;
        Set<ConstraintViolation<Object>> violations = executableValidator.validateParameters(
                resource, ctx.getMethod(), ctx.getParameters());

        ConstraintViolationException cve;
        
        if (!violations.isEmpty()) {
            cve = new ConstraintViolationException(
                    getMessage(ctx.getMethod(), ctx.getParameters(), violations), violations);
            // Update binding result or re-throw exception if not present
            if (!updateBindingResultViolations(resource, buildViolationErrors(cve), bindingResult)) {
                throw cve;
            }
        }

        Object result = ctx.proceed();

        violations = executableValidator.validateReturnValue(ctx.getTarget(), ctx.getMethod(), result);

        if (!violations.isEmpty()) {
            cve = new ConstraintViolationException(
                    getMessage(ctx.getMethod(), ctx.getParameters(), violations), violations);
            // Update binding result or re-throw exception if not present
            if (!updateBindingResultViolations(resource, buildViolationErrors(cve), bindingResult)) {
                throw cve;
            }
        }

        return result;
    }

    /**
     * Creates a set of violation errors from a {@link ConstraintViolationException}.
     *
     * @param cve the exception containing the violations
     * @return the set of validation errors
     */
    private Set<ValidationError> buildViolationErrors(ConstraintViolationException cve) {

        Set<ValidationError> validationErrors = new LinkedHashSet<>();

        for (ConstraintViolation<?> violation : cve.getConstraintViolations()) {

            String paramName = ConstraintViolationUtils.getParamName(violation);
            if (paramName == null) {
                LOG.log(Level.WARNING, "Cannot resolve paramName for violation: {0}", violation);
            }

            String message = violationTranslator.translate(violation, mvcContext.getLocale());

            validationErrors.add(new ValidationErrorImpl(violation, paramName, message));

        }

        return validationErrors;

    }

    private String getMessage(Method method, Object[] args, Set<? extends ConstraintViolation<?>> violations) {

        StringBuilder message = new StringBuilder(400);
        message.append(violations.size());
        message.append(" constraint violation(s) occurred during method invocation.");
        message.append("\nMethod: ");
        message.append(method);
        message.append("\nArgument values: ");
        message.append(Arrays.toString(args));
        message.append("\nConstraint violations: ");

        int i = 1;
        for (ConstraintViolation<?> violation : violations) {
            message.append("\n (");
            message.append(i);
            message.append(") Message: ");
            message.append(violation.getMessage());

            i++;
        }

        return message.toString();
    }

}

