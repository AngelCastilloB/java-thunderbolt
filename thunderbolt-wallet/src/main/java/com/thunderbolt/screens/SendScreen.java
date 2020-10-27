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

import com.thunderbolt.common.Convert;
import com.thunderbolt.components.ButtonComponent;
import com.thunderbolt.components.InputComponent;
import com.thunderbolt.components.InputType;
import com.thunderbolt.configuration.Configuration;
import com.thunderbolt.state.NodeService;
import com.thunderbolt.theme.Theme;
import com.thunderbolt.wallet.Address;
import com.thunderbolt.worksapce.NotificationButtons;
import com.thunderbolt.worksapce.NotificationResult;

import java.awt.*;

/* IMPLEMENTATION ************************************************************/

/**
 * This screen display all the information necessary to send transactions.
 */
public class SendScreen extends ScreenBase
{
    private static final int BUTTON_WIDTH  = 80;
    private static final int BUTTON_HEIGHT = 35;

    private InputComponent m_payToAddress = new InputComponent(InputType.PlainText);
    private InputComponent m_amount       = new InputComponent(InputType.Numbers);
    private InputComponent m_fee          = new InputComponent(InputType.Numbers);

    public SendScreen()
    {
        setTitle("SEND");
        setBackground(Theme.FOREGROUND_COLOR);


        m_payToAddress.setTile("To (Address):");
        m_payToAddress.setSize(600, 50);
        m_payToAddress.setFont(Theme.SEND_SCREEN_FIELD_FONT);
        m_payToAddress.setLocation(getWidth() / 2 - m_payToAddress.getWidth() / 2, 120);

        m_amount.setTile("Amount");
        m_amount.setSize(300, 50);
        m_amount.setFont(Theme.SEND_SCREEN_FIELD_FONT);
        m_amount.setLocation(getWidth() - m_amount.getWidth() - 70, 220);

        m_fee.setTile("Fee");
        m_fee.setSize(300, 50);
        m_fee.setFont(Theme.SEND_SCREEN_FIELD_FONT);
        m_fee.setLocation(getWidth() - m_fee.getWidth() - 70, 320);
        m_fee.setText(Double.toString(Configuration.getPayTransactionFee()));

        ButtonComponent buttonComponent = new ButtonComponent(Theme.NOTIFICATION_BUTTON_BACKGROUND,
                Theme.NOTIFICATION_BUTTON_BACKGROUND,
                Theme.NOTIFICATION_BUTTON_TEXT,
                Theme.NOTIFICATION_BUTTON_TEXT);
        buttonComponent.setText("Send");
        buttonComponent.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);

        buttonComponent.setLocation(m_amount.getX() + m_amount.getWidth() - buttonComponent.getWidth(),
                m_fee.getY() + m_fee.getHeight() + 60);

        buttonComponent.addButtonClickListener(() -> {

            if (m_payToAddress.getText().isEmpty())
            {
                ScreenManager.getInstance().showNotification("Error","Address can not be empty.",
                        NotificationButtons.GotIt, result -> {});
                return;
            }

            try
            {
                Address address = new Address(m_payToAddress.getText());
            }
            catch (IllegalArgumentException exception)
            {
                ScreenManager.getInstance().showNotification("Error","There is an error in the given address. " +
                                "Make sure you typed it correctly.",
                        NotificationButtons.GotIt, result -> {});
                return;
            }

            if (m_amount.getText().equals("0"))
            {
                ScreenManager.getInstance().showNotification("Error","Amount must be greater than 0.",
                        NotificationButtons.GotIt, result -> {
                        });
                return;
            }

            ScreenManager.getInstance().showNotification("Info",
                    String.format("Are you sure you want to send %s THB to:\n%s\n(With %s fee THB)?",
                            m_amount.getText(),
                            m_payToAddress.getText().substring(0, 40) + "...",
                            m_fee.getText()),
                    NotificationButtons.AgreeDisagree, result -> {

                        if (result == NotificationResult.Agree)
                        {
                           NodeService.getInstance().sendToAddress(
                                   m_payToAddress.getText(),
                                   Double.parseDouble(m_amount.getText()),
                                   Double.parseDouble(m_fee.getText()));
                        }
                    });
        });

        add(m_payToAddress);
        add(m_amount);
        add(m_fee);
        add(buttonComponent);
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

        graphics.setFont(Theme.SEND_SCREEN_FONT);
        graphics.setColor(Theme.SEND_SCREEN_TEXT_COLOR);

        String message = "Please enter The Transaction Details";
        int measure = graphics2d.getFontMetrics().stringWidth(message);

        graphics.drawString(message, getWidth() / 2 - measure / 2, 60);
    }
}
