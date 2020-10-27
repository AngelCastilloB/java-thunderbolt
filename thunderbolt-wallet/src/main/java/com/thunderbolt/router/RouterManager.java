/*
 * MIT License
 *
 * Copyright (c) 2020 Angel Castillo.
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

package com.thunderbolt.router;

/* IMPORTS *******************************************************************/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* IMPLEMENTATION ************************************************************/

/**
 * Manager for the router of the application.
 */
public class RouterManager
{
    // Static fields.
    private static RouterManager s_instance = null;
    private static final Logger  s_logger   = LoggerFactory.getLogger(RouterManager.class);

    private IRouter m_router = null;

    /**
     * Prevents a default instance of the RouteManager class from being created.
     */
    protected RouterManager()
    {
    }

    /**
     * Gets the router manager singleton instance.
     *
     * @return The router manager instance.
     */
    public static RouterManager getInstance()
    {
        if(s_instance == null)
            s_instance = new RouterManager();

        return s_instance;
    }

    /**
     * Sets the router instance.
     *
     * @param router The router.
     */
    public void setRouter(IRouter router)
    {
        m_router = router;
    }

    /**
     * Navigate to an area of the application.
     *
     * @param route the route to navigate to.
     */
    public void navigate(String route)
    {
        m_router.navigate(route);
    }
}
