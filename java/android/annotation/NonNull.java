
package android.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Denotes that a parameter, field or method return value can never be null.
 * <p>
 * This is a marker annotation and it has no specific attributes.
 *
 * @paramDoc This value cannot be {@code null}.
 * @returnDoc This value cannot be {@code null}.
 * @hide
 */
@Retention(CLASS)
@Target({METHOD, PARAMETER, FIELD})
public @interface NonNull {
}