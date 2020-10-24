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
import com.thunderbolt.state.INodeStatusChangeListener;
import com.thunderbolt.state.NodeState;
import com.thunderbolt.state.StateService;
import com.thunderbolt.theme.Theme;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Objects;

/* IMPLEMENTATION ************************************************************/

/**
 * Panel component that can display an image as background.
 */
public class MenuPanel extends JPanel implements INodeStatusChangeListener
{
    private static final int LEFT_MARGIN                  = 37;
    private static final int BUTTON_WIDTH                 = 215;
    private static final int BUTTON_HEIGHT                = 35;
    private static final int OVERVIEW_BUTTON_POSITION     = 290;
    private static final int SEND_BUTTON_POSITION         = 340;
    private static final int RECEIVE_BUTTON_POSITION      = 390;
    private static final int ENCRYPT_KEYS_BUTTON_POSITION = 440;
    private static final int EXPORT_BUTTON_POSITION       = 490;
    private static final int DUMP_BUTTON_POSITION         = 540;

    private Image            m_img;
    private final MenuButton m_overviewButton     = new MenuButton(ResourceManager.loadImage("images/overview.png"), "Overview");
    private final MenuButton m_sendButton         = new MenuButton(ResourceManager.loadImage("images/send.png"), "Send");
    private final MenuButton m_receiveButton      = new MenuButton(ResourceManager.loadImage("images/receive.png"), "Receive");
    private final MenuButton m_encryptButton      = new MenuButton(ResourceManager.loadImage("images/encrypt.png"), "Encrypt Keys");
    private final MenuButton m_exportButton       = new MenuButton(ResourceManager.loadImage("images/export.png"), "Export Wallet");
    private final MenuButton m_dumpKeysButton     = new MenuButton(ResourceManager.loadImage("images/dump_keys.png"), "Dump Keys");

    /**
     * Initializes a new instance of the ImagePanel class.
     *
     * @param img The path to the image to be drawn.
     */
    public MenuPanel(String img) throws IOException
    {
        this(ImageIO.read(Objects.requireNonNull(MenuPanel.class.getClassLoader().getResourceAsStream(img))));
        StateService.getInstance().addListener(this);
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
            if (StateService.getInstance().getNodeState().equals(NodeState.Offline))
            {
                ScreenManager.getInstance().replaceTopScreen(new MessageScreen("The node is offline. Please start the Thunderbolt node."));
                ResourceManager.playAudio(Theme.MENU_BUTTON_INVALID_SOUND);
                return;
            }

            if (StateService.getInstance().getNodeState().equals(NodeState.Syncing))
            {
                ScreenManager.getInstance().replaceTopScreen(new MessageScreen("The node is currently syncing with peers. Please wait."));
                ResourceManager.playAudio(Theme.MENU_BUTTON_INVALID_SOUND);
                return;
            }

            ScreenManager.getInstance().replaceTopScreen(new OverviewScreen());
            ResourceManager.playAudio(Theme.MENU_BUTTON_CLICK_SOUND);
            activateButton(m_overviewButton);
        });

        m_sendButton.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        m_sendButton.setLocation(LEFT_MARGIN, SEND_BUTTON_POSITION);

        m_sendButton.addActionListener(e ->
        {
            if (StateService.getInstance().getNodeState().equals(NodeState.Offline))
            {
                ScreenManager.getInstance().replaceTopScreen(new MessageScreen("The node is offline. Please start the Thunderbolt node."));
                ResourceManager.playAudio(Theme.MENU_BUTTON_INVALID_SOUND);
                return;
            }

            if (StateService.getInstance().getNodeState().equals(NodeState.Syncing))
            {
                ScreenManager.getInstance().replaceTopScreen(new MessageScreen("The node is currently syncing with peers. Please wait."));
                ResourceManager.playAudio(Theme.MENU_BUTTON_INVALID_SOUND);
                return;
            }

            ScreenManager.getInstance().replaceTopScreen(new SendScreen());
            ResourceManager.playAudio(Theme.MENU_BUTTON_CLICK_SOUND);
            activateButton(m_sendButton);
        });

        m_receiveButton.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        m_receiveButton.setLocation(LEFT_MARGIN, RECEIVE_BUTTON_POSITION);

        m_receiveButton.addActionListener(e ->
        {
            if (StateService.getInstance().getNodeState().equals(NodeState.Offline))
            {
                ScreenManager.getInstance().replaceTopScreen(new MessageScreen("The node is offline. Please start the Thunderbolt node."));
                ResourceManager.playAudio(Theme.MENU_BUTTON_INVALID_SOUND);
                return;
            }

            if (StateService.getInstance().getNodeState().equals(NodeState.Syncing))
            {
                ScreenManager.getInstance().replaceTopScreen(new MessageScreen("The node is currently syncing with peers. Please wait."));
                ResourceManager.playAudio(Theme.MENU_BUTTON_INVALID_SOUND);
                return;
            }

            ScreenManager.getInstance().replaceTopScreen(new ReceiveScreen());
            ResourceManager.playAudio(Theme.MENU_BUTTON_CLICK_SOUND);
            activateButton(m_receiveButton);
        });

        m_encryptButton.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        m_encryptButton.setLocation(LEFT_MARGIN, ENCRYPT_KEYS_BUTTON_POSITION);

        m_encryptButton.addActionListener(e ->
        {
            if (StateService.getInstance().getNodeState().equals(NodeState.Offline))
            {
                ScreenManager.getInstance().replaceTopScreen(new MessageScreen("The node is offline. Please start the Thunderbolt node."));
                ResourceManager.playAudio(Theme.MENU_BUTTON_INVALID_SOUND);
                return;
            }

            if (StateService.getInstance().getNodeState().equals(NodeState.Syncing))
            {
                ScreenManager.getInstance().replaceTopScreen(new MessageScreen("The node is currently syncing with peers. Please wait."));
                ResourceManager.playAudio(Theme.MENU_BUTTON_INVALID_SOUND);
                return;
            }

            ScreenManager.getInstance().replaceTopScreen(new EncryptWalletScreen());
            ResourceManager.playAudio(Theme.MENU_BUTTON_CLICK_SOUND);
            activateButton(m_encryptButton);
        });

        m_exportButton.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        m_exportButton.setLocation(LEFT_MARGIN, EXPORT_BUTTON_POSITION);

        m_exportButton.addActionListener(e ->
        {
            if (StateService.getInstance().getNodeState().equals(NodeState.Offline))
            {
                ScreenManager.getInstance().replaceTopScreen(new MessageScreen("The node is offline. Please start the Thunderbolt node."));
                ResourceManager.playAudio(Theme.MENU_BUTTON_INVALID_SOUND);
                return;
            }

            if (StateService.getInstance().getNodeState().equals(NodeState.Syncing))
            {
                ScreenManager.getInstance().replaceTopScreen(new MessageScreen("The node is currently syncing with peers. Please wait."));
                ResourceManager.playAudio(Theme.MENU_BUTTON_INVALID_SOUND);
                return;
            }

            ScreenManager.getInstance().replaceTopScreen(new ExportWalletScreen());
            ResourceManager.playAudio(Theme.MENU_BUTTON_CLICK_SOUND);
            activateButton(m_exportButton);
        });

        m_dumpKeysButton.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        m_dumpKeysButton.setLocation(LEFT_MARGIN, DUMP_BUTTON_POSITION);

        m_dumpKeysButton.addActionListener(e ->
        {
            if (StateService.getInstance().getNodeState().equals(NodeState.Offline))
            {
                ScreenManager.getInstance().replaceTopScreen(new MessageScreen("The node is offline. Please start the Thunderbolt node."));
                ResourceManager.playAudio(Theme.MENU_BUTTON_INVALID_SOUND);
                return;
            }

            if (StateService.getInstance().getNodeState().equals(NodeState.Syncing))
            {
                ScreenManager.getInstance().replaceTopScreen(new MessageScreen("The node is currently syncing with peers. Please wait."));
                ResourceManager.playAudio(Theme.MENU_BUTTON_INVALID_SOUND);
                return;
            }

            ScreenManager.getInstance().replaceTopScreen(new DumpKeysScreen());
            ResourceManager.playAudio(Theme.MENU_BUTTON_CLICK_SOUND);
            activateButton(m_dumpKeysButton);
        });

        add(m_overviewButton);
        add(m_sendButton);
        add(m_receiveButton);
        add(m_encryptButton);
        add(m_exportButton);
        add(m_dumpKeysButton);
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
        Graphics2D graphics2d = (Graphics2D)graphics;
        graphics2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        graphics.drawImage(m_img, 0, 0, null);

        graphics.setColor(Theme.MENU_OVERVIEW_FONT_COLOR);
        graphics.setFont(Theme.MENU_OVERVIEW_FONT);

        String availableText = StateService.getInstance().getAvailableBalance();
        String pendingText = StateService.getInstance().getPendingBalance();
        String totalText = StateService.getInstance().getTotalBalance();

        int availableWidth = graphics.getFontMetrics().stringWidth(availableText);
        int pendingWidth = graphics.getFontMetrics().stringWidth(pendingText);
        int totalWidth = graphics.getFontMetrics().stringWidth(totalText);

        graphics.drawString("Available:", 40, 150);
        graphics.drawString(availableText, getWidth() - availableWidth - 40, 150);

        graphics.drawString("Pending:", 40, 190);
        graphics.drawString(pendingText, getWidth() - pendingWidth - 40, 190);

        graphics.drawString("Total:", 40, 230);
        graphics.drawString(totalText, getWidth() - totalWidth - 40, 230);

        paintChildren(graphics);
    }

    /**
     * Activates the given button.
     *
     * @param button The button to be activated.
     */
    private void activateButton(MenuButton button)
    {
        m_overviewButton.setActive(m_overviewButton == button);
        m_sendButton.setActive(m_sendButton == button);
        m_receiveButton.setActive(m_receiveButton == button);
        m_encryptButton.setActive(m_encryptButton == button);
        m_exportButton.setActive(m_exportButton == button);
        m_dumpKeysButton.setActive(m_dumpKeysButton == button);
    }

    /**
     * Trigger when the node state changes.
     *
     * @param state The new state.
     */
    @Override
    public void onNodeStatusChange(NodeState state)
    {
        if (state == NodeState.Ready)
        {
            m_overviewButton.setActive(true);
            m_overviewButton.doClick();
        }

        if (state == NodeState.Offline)
        {
            ScreenManager.getInstance().replaceTopScreen(new MessageScreen("The node is offline. Please start the Thunderbolt node."));
            return;
        }

        if (state == NodeState.Syncing)
            ScreenManager.getInstance().replaceTopScreen(new MessageScreen("The node is currently syncing with peers. Please wait."));
    }
}