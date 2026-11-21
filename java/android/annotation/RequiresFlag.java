/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.annotation;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated source code is guarded by an aconfig flag.
 *
 * <p>This annotation serves as a formal contract signifying that the annotated class, method, or
 * constructor must not be accessed or invoked unless the specified flag is enabled. This can also
 * be used to pass on the requirement to check the flag to the callers of the annotated element.
 *
 * <p>Example: <code><pre>
 *     import com.example.foobar.Flags;
 *
 *     &#64;RequiresFlag(Flags.FLAG_FOOBAR)
 *     public void foobar() { ... }
 * </pre></code>
 *
 * <p>Usage example: <code><pre>
 *     public void codeThatUsesFoobarApi() {
 *         if (Flags.foobar()) {
 *             foobar();
 *         } else {
 *             // gracefully handle the case where the flag is disabled.
 *         }
 *     }
 * </pre></code>
 *
 * @hide
 */
@Target({TYPE, METHOD, CONSTRUCTOR})
@Retention(RetentionPolicy.CLASS)
public @interface RequiresFlag {
    /**
     * The aconfig flag used to guard the functionality of the annotated element. Use the aconfig
     * auto-generated constant to refer to the flag, e.g. {@code @RequiresFlag(Flags.FLAG_FOOBAR)}.
     */
    String value();
}
