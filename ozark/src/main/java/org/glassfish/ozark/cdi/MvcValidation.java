package org.glassfish.ozark.cdi;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.interceptor.InterceptorBinding;

/**
 * Triggers validation of incoming parameters in {@link javax.mvc.annotation.Controller} methods.
 * 
 * @author Dmytro Maidaniuk
 */
@Inherited
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({METHOD, TYPE})
public @interface MvcValidation {
    
}
