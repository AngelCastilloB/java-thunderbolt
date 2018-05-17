/*
 * MIT License
 *
 * Copyright (c) 2018 Angel Castillo.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.thunderbolt.common;

/* IMPORTS *******************************************************************/

import java.util.HashMap;
import java.util.Map;

/* IMPLEMENTATION ************************************************************/

/**
 * Central registry of services. Using this class you can register and retrieve services at any point
 * in the application.
 */
public class ServiceLocator
{
    private static Map<String, Object> s_services = new HashMap<>();

    /**
     * Register a service to the service locator.
     *
     * @param service The service instance.
     * @param clazz   The type under which this service will be registered.
     */
    public static <T> void register(Class<T> clazz, T service)
    {
        // We need to make sure the service is assignable to the class under which is being registered.
        if (!clazz.isAssignableFrom(service.getClass()))
            throw new RuntimeException(String.format("The class %s is not assignable from %s", clazz.getName(), service.getClass().getName()));

        s_services.put(clazz.getName(), service);
    }

    /**
     * Gets the service registered with the given name.
     *
     * @param clazz The class of the instance we want to locate. We need to pass the class here, since
     *              the Java compiler applies type erasure to generic types, which means we cant get the name
     *              of the class using the type T.
     *
     * @return The service.
     *
     * <p>Note: The unchecked warning appears because the compiler can't guarantee type safety even if the casting works
     * fine at runtime due to type erasure. However we can suppress this since we know the underlying type will always
     * be of type T</p>
     */
    @SuppressWarnings("unchecked")
    public static <T> T getService(Class<T> clazz)
    {
        if(!s_services.containsKey(clazz.getName()))
            throw new RuntimeException(String.format("Service %s not found.", clazz.getName()));

        return (T)s_services.get(clazz.getName());
    }
}
