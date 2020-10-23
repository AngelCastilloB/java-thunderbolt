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

package com.thunderbolt.screens;

/* IMPORTS ********************************************************************/

import javax.swing.*;

/* DECLARATION ****************************************************************/

/**
 * Base class for all screens.
 */
public class ScreenBase extends JComponent
{
    private ScreenResult m_result     = ScreenResult.None;
    private String       m_title      = "";
    private boolean      m_showTitle  = false;
    private boolean      m_showHeader = true;
    private boolean      m_showFooter = true;
    private boolean      m_closeAble  = false;
    private boolean      m_showing    = false;

    /**
     * Gets the screen result.
     *
     * @return The screen result.
     */
    public ScreenResult getResult()
    {
        return m_result;
    }

    /**
     * Sets the screen result.
     *
     * @param result The screen result.
     */
    public void setResult(ScreenResult result)
    {
        m_result = result;
    }

    /**
     * Gets the screen title.
     *
     * @return The screen title.
     */
    public String getTitle()
    {
        return m_title;
    }

    /**
     * Sets the screen title.
     *
     * @param title The screen title.
     */
    public void setTitle(String title)
    {
        m_title = title;
    }

    /**
     * Gets whether the title of this screen is showing.
     *
     * @return True if the title is showing, otherwise, false.
     */
    public boolean isTitleShowing()
    {
        return m_showTitle;
    }

    /**
     * Sets whether the title of this screen is showing.
     *
     * @param showTitle True if the title of this is screen must be shown, otherwise, false.
     */
    public void setIsTitleShowing(boolean showTitle)
    {
        m_showTitle = showTitle;
    }

    /**
     * Gets whether the header of this screen is showing.
     */
    public boolean isHeaderShowing()
    {
        return m_showHeader;
    }

    /**
     * Gets whether the header of this screen is showing.
     *
     * @param showHeader True if the header of this is screen must be shown, otherwise, false.
     */
    public void setIsHeaderShowing(boolean showHeader)
    {
        this.m_showHeader = showHeader;
    }

    /**
     * Gets whether the footer of this screen is showing.
     */
    public boolean isFooterShowing()
    {
        return m_showFooter;
    }

    /**
     * Gets whether the footer of this screen is showing.
     *
     * @param showFooter True if the header of this is screen must be shown, otherwise, false.
     */
    public void setIsFooterShowing(boolean showFooter)
    {
        this.m_showFooter = showFooter;
    }

    /**
     * Gets whether the screen is closeable.
     */
    public boolean isCloseAble()
    {
        return m_closeAble;
    }

    /**
     * Sets whether the screen is closeable.
     *
     * @param closeAble True if the screen is closeable, otherwise, false.
     */
    public void setIsCloseAble(boolean closeAble)
    {
        this.m_closeAble = closeAble;
    }

    /**
     * Gets whether this screen is currently being shown.
     *
     * @return True if the screen is being shown, otherwise, false.
     */
    public boolean isShowing()
    {
        return m_showing;
    }

    /**
     * Sets whether this screen is currently being shown.
     *
     * @param showing True if the screen is being shown, otherwise, false.
     */
    public void setIsShowing(boolean showing)
    {
        m_showing = showing;
    }

    /**
     * This method will be called by the screen manager just before adding the screen to the workspace.
     */
    public void onShow()
    {
        m_showing = true;
    }

    /**
     * This method will be called by the screen manager just before removing the screen from the workspace.
     */
    public void onClose()
    {
        m_showing = false;
    }
}
