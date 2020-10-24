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

package com.thunderbolt.theme;

/* IMPORTS ********************************************************************/

import com.thunderbolt.resources.ResourceManager;
import java.awt.*;

/* DECLARATION ***************************************************************/

/**
 * Application theme.
 */
public class Theme
{
    public static final Color BACKGROUND_COLOR      = new Color(249, 249, 249);
    public static final Color FOREGROUND_COLOR      = new Color(253, 253, 253);
    public static final Color SHADOW_UMBRA_COLOR    = new Color(233, 233, 233);
    public static final Color SHADOW_PENUMBRA_COLOR = new Color(246, 246, 246);
    public static final Color TITLE_FONT_COLOR      = new Color(0, 12, 60);
    public static final Font  TITLE_FONT            = ResourceManager.loadFont("fonts/Roboto-Medium.ttf", 16f);
    public static final Color TITLE_COLOR           = Color.WHITE;
    public static final Color HEADER_COLOR          = new Color(255, 9, 14, 15);
}
