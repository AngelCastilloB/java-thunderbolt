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
import com.thunderbolt.state.NodeService;
import com.thunderbolt.theme.Theme;
import com.thunderbolt.worksapce.NotificationButtons;
import com.thunderbolt.worksapce.NotificationResult;

import javax.swing.*;
import java.awt.*;

/* IMPLEMENTATION ************************************************************/

public class EncryptWalletScreen extends ScreenBase
{
    private static final int BUTTON_WIDTH  = 80;
    private static final int BUTTON_HEIGHT = 35;

    private JPasswordField  m_passphrase        = new JPasswordField();
    private JPasswordField  m_confirmPassphrase = new JPasswordField();
    private ISuccessHandler m_handler;

    public EncryptWalletScreen(ISuccessHandler handler)
    {
        setTitle("ENCRYPT WALLET");

        m_handler = handler;

        m_passphrase.setSize(465, 50);
        m_passphrase.setFont(Theme.ENCRYPT_INPUT_FIELD_FONT);
        m_passphrase.setLocation(230, 230 - 30);

        m_confirmPassphrase.setSize(465, 50);
        m_confirmPassphrase.setFont(Theme.ENCRYPT_INPUT_FIELD_FONT);
        m_confirmPassphrase.setLocation(230, 350 - 30);

        ButtonComponent buttonComponent = new ButtonComponent(Theme.NOTIFICATION_BUTTON_BACKGROUND,
                Theme.NOTIFICATION_BUTTON_BACKGROUND,
                Theme.NOTIFICATION_BUTTON_TEXT,
                Theme.NOTIFICATION_BUTTON_TEXT);

        buttonComponent.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);

        buttonComponent.setLocation(getWidth() - buttonComponent.getWidth() - 50,
                getHeight() - buttonComponent.getHeight() - 100);

        buttonComponent.setText("Encrypt");

        buttonComponent.addButtonClickListener(() ->
        {
            if (m_passphrase.getText().isEmpty() || m_confirmPassphrase.getText().isEmpty())
            {
                ScreenManager.getInstance().showNotification("Warning",
                        "The wallet password should not be empty.",
                        NotificationButtons.GotIt, result -> {
                        });

                return;
            }

            if (m_passphrase.getText().equals(m_confirmPassphrase.getText()))
            {
                boolean encrypted = NodeService.getInstance().encryptWallet(m_passphrase.getText());

                if (encrypted)
                {
                    NodeService.getInstance().lockWallet();
                    ScreenManager.getInstance().showNotification("Information",
                            "Your wallet is now encrypted.",
                            NotificationButtons.GotIt, result -> {
                                m_handler.onSuccess();
                            });
                }
                else
                {
                    ScreenManager.getInstance().showNotification("Error",
                            "There was an error encrypting the wallet. Refer to the node logs.",
                            NotificationButtons.GotIt, result -> {
                            });
                }
            }
            else
            {
                ScreenManager.getInstance().showNotification("Warning",
                        "Passwords do not match.",
                        NotificationButtons.GotIt, result -> {
                        });
            }
        });

        add(buttonComponent);
        add(m_passphrase);
        add(m_confirmPassphrase);
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
        Graphics2D graphics2d = (Graphics2D)graphics;
        graphics2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        graphics.setFont(Theme.ENCRYPT_SCREEN_FONT);
        graphics.setColor(Theme.ENCRYPT_SCREEN_TEXT_COLOR);

        graphics.drawString("Enter the new passphrase to the wallet", 40, 80);
        graphics.drawString("Please use a passphrase of ten or more random characters, or eight or more words:", 40, 150);
        graphics.drawString("New passphrase:", 40, 230);
        graphics.drawString("Repeat passphrase:", 40, 350);
        paintChildren(graphics);
    }
}
