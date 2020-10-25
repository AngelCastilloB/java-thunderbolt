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

/* IMPORTS *******************************************************************/

import com.thunderbolt.state.NodeService;
import com.thunderbolt.theme.Theme;

import javax.swing.*;

/* IMPLEMENTATION ************************************************************/

public class EncryptWalletScreen extends ScreenBase
{
    public EncryptWalletScreen()
    {
        setTitle("ENCRYPT WALLET");
        setBackground(Theme.FOREGROUND_COLOR);

        JTextField label = new JPasswordField();

        label.setSize(getWidth() - 20, 50);
        label.setFont(Theme.TITLE_FONT);
        label.setLocation(10, getHeight() - 150);
        //label.setHorizontalAlignment(0);
        //label.setBorder(BorderFactory.createEmptyBorder());
        //label.setBackground(Theme.FOREGROUND_COLOR);


        add(label);
    }
}
