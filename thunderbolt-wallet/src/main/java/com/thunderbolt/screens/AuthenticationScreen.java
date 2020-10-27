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

import com.thunderbolt.components.ButtonComponent;
import com.thunderbolt.components.InputComponent;
import com.thunderbolt.components.InputType;
import com.thunderbolt.state.NodeService;
import com.thunderbolt.theme.Theme;
import com.thunderbolt.worksapce.NotificationButtons;

import java.awt.*;

/* IMPLEMENTATION ************************************************************/

/**
 * The authentication screen. Request the user the passphrase to unlock the wallet.
 */
public class AuthenticationScreen extends ScreenBase
{
    private static final int BUTTON_WIDTH  = 80;
    private static final int BUTTON_HEIGHT = 35;

    private final ISuccessHandler m_handler;
    private InputComponent        m_passphrase;

    /**
     * Initializes a new instance of the AuthenticationScreen class.
     *
     * @param handler The handler for when the authentication finish.
     */
    public AuthenticationScreen(ISuccessHandler handler)
    {
        setTitle("UNLOCK WALLET");

        m_handler = handler;

        m_passphrase = new InputComponent(InputType.Password, input -> {
            boolean unlocked = NodeService.getInstance().unlockWallet(m_passphrase.getText());

            if (unlocked)
            {
                m_handler.onSuccess();
            }
            else
            {
                ScreenManager.getInstance().showNotification("Warn",
                        "The Wallet could not be unlocked.",
                        NotificationButtons.GotIt, result -> {
                        });
            }
        });

        m_passphrase.setTile("Password");
        m_passphrase.setSize(500, 50);
        m_passphrase.setFont(Theme.ENCRYPT_INPUT_FIELD_FONT);
        m_passphrase.setLocation(getWidth() / 2 - m_passphrase.getWidth() / 2, 250);

        ButtonComponent buttonComponent = new ButtonComponent(Theme.NOTIFICATION_BUTTON_BACKGROUND,
                Theme.NOTIFICATION_BUTTON_BACKGROUND,
                Theme.NOTIFICATION_BUTTON_TEXT,
                Theme.NOTIFICATION_BUTTON_TEXT);
        buttonComponent.setText("Unlock");
        buttonComponent.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);

        buttonComponent.setLocation(m_passphrase.getX() + m_passphrase.getWidth() - buttonComponent.getWidth(),
                m_passphrase.getY() + m_passphrase.getHeight() + 40);

        buttonComponent.addButtonClickListener(() -> {

            boolean unlocked = NodeService.getInstance().unlockWallet(m_passphrase.getText());

            if (unlocked)
            {
                m_handler.onSuccess();
            }
            else
            {
                ScreenManager.getInstance().showNotification("Warn",
                        "The Wallet could not be unlocked.",
                        NotificationButtons.GotIt, result -> {
                        });
            }
        });

        add(m_passphrase);
        add(buttonComponent);
    }

    /**
     * This method will be called by the screen manager just before adding the screen to the workspace.
     */
    @Override
    public void onShow()
    {
        super.onShow();
        m_passphrase.grabFocus();
        m_passphrase.requestFocus();
    }

    /**
     * Paints this component's children. If shouldUseBuffer is true, no component ancestor has a buffer and the component
     * children can use a buffer if they have one. Otherwise, one ancestor has a buffer currently in use and children
     * should not use a buffer to paint.
     *
     * @param graphics the Graphics context in which to paint
     */
    public void paintComponent(Graphics graphics)
    {
        super.paintComponent(graphics);
        Graphics2D graphics2d = (Graphics2D) graphics;
        graphics2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        graphics.setFont(Theme.ENCRYPT_SCREEN_FONT);
        graphics.setColor(Theme.ENCRYPT_SCREEN_TEXT_COLOR);

        String message = "Please enter the wallet's passphrase";
        int measure = graphics2d.getFontMetrics().stringWidth(message);

        graphics.drawString(message, getWidth() / 2 - measure / 2, 200);
        paintChildren(graphics);

    }
}
