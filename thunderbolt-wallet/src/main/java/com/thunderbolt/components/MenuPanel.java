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

package com.thunderbolt.components;

/* IMPORTS *******************************************************************/

import com.thunderbolt.resources.ResourceManager;
import com.thunderbolt.screens.*;
import com.thunderbolt.theme.Theme;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Objects;

/* IMPLEMENTATION ************************************************************/

/**
 * Panel component that can display an image as background.
 */
public class MenuPanel extends JPanel
{
    private static final int LEFT_MARGIN                  = 37;
    private static final int BUTTON_WIDTH                 = 215;
    private static final int BUTTON_HEIGHT                = 35;
    private static final int OVERVIEW_BUTTON_POSITION     = 305;
    private static final int SEND_BUTTON_POSITION         = 355;
    private static final int RECEIVE_BUTTON_POSITION      = 405;
    private static final int TRANSACTIONS_BUTTON_POSITION = 455;
    private static final int ENCRYPT_BUTTON_POSITION      = 505;

    private Image            m_img;
    private final MenuButton m_overviewButton     = new MenuButton();
    private final MenuButton m_sendButton         = new MenuButton();
    private final MenuButton m_receiveButton      = new MenuButton();
    private final MenuButton m_transactionsButton = new MenuButton();
    private final MenuButton m_encryptButton      = new MenuButton();

    /**
     * Initializes a new instance of the ImagePanel class.
     *
     * @param img The path to the image to be drawn.
     */
    public MenuPanel(String img) throws IOException
    {
        this(ImageIO.read(Objects.requireNonNull(MenuPanel.class.getClassLoader().getResourceAsStream(img))));
    }

    /**
     * Initializes a new instance of the ImagePanel class.
     *
     * @param img The image to be drawn.
     */
    public MenuPanel(Image img)
    {
        m_img = img;
        setLayout(null);

        m_overviewButton.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        m_overviewButton.setLocation(LEFT_MARGIN, OVERVIEW_BUTTON_POSITION);

        m_overviewButton.addActionListener(e ->
        {
            ScreenManager.getInstance().replaceTopScreen(new OverviewScreen());
            ResourceManager.playAudio(Theme.MENU_BUTTON_CLICK_SOUND);
        });

        m_sendButton.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        m_sendButton.setLocation(LEFT_MARGIN, SEND_BUTTON_POSITION);

        m_sendButton.addActionListener(e ->
        {
            ScreenManager.getInstance().replaceTopScreen(new SendScreen());
            ResourceManager.playAudio(Theme.MENU_BUTTON_CLICK_SOUND);
        });

        m_receiveButton.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        m_receiveButton.setLocation(LEFT_MARGIN, RECEIVE_BUTTON_POSITION);

        m_receiveButton.addActionListener(e ->
        {
            ScreenManager.getInstance().replaceTopScreen(new ReceiveScreen());
            ResourceManager.playAudio(Theme.MENU_BUTTON_CLICK_SOUND);
        });

        m_transactionsButton.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        m_transactionsButton.setLocation(LEFT_MARGIN, TRANSACTIONS_BUTTON_POSITION);

        m_transactionsButton.addActionListener(e ->
        {
            ScreenManager.getInstance().replaceTopScreen(new TransactionsScreen());
            ResourceManager.playAudio(Theme.MENU_BUTTON_CLICK_SOUND);
        });

        m_encryptButton.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        m_encryptButton.setLocation(LEFT_MARGIN, ENCRYPT_BUTTON_POSITION);

        m_encryptButton.addActionListener(e ->
        {
            ScreenManager.getInstance().replaceTopScreen(new EncryptWalletScreen());
            ResourceManager.playAudio(Theme.MENU_BUTTON_CLICK_SOUND);
        });

        add(m_overviewButton);
        add(m_sendButton);
        add(m_receiveButton);
        add(m_transactionsButton);
        add(m_encryptButton);
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
        graphics.drawImage(m_img, 0, 0, null);
        paintChildren(graphics);
    }
}