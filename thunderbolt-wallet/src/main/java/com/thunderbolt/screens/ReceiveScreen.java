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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.thunderbolt.components.ButtonComponent;
import com.thunderbolt.components.IButtonClickHandler;
import com.thunderbolt.resources.ResourceManager;
import com.thunderbolt.state.NodeService;
import com.thunderbolt.theme.Theme;
import com.thunderbolt.worksapce.NotificationButtons;
import com.thunderbolt.worksapce.NotificationResult;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/* IMPLEMENTATION ************************************************************/

/**
 * Displays the receive screen. This screen displays the necessary information to be able to receive coins.
 */
public class ReceiveScreen extends ScreenBase
{
    private static final int QR_WIDTH               = 300;
    private static final int QR_HEIGHT              = 300;
    private static final int DOWNLOAD_BUTTON_HEIGHT = 30;
    private static final int DOWNLOAD_BUTTON_WIDTH  = 40;

    private BufferedImage m_qrCode  = null;
    private BufferedImage m_address = null;

    /**
     * Initializes a new instance of the ReceiveScreen class.
     */
    public ReceiveScreen() throws WriterException
    {
        setTitle("RECEIVE");
        setLayout(null);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        BitMatrix bitMatrix = qrCodeWriter.encode(NodeService.getInstance().getAddress().toString(),
                BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT);

        m_qrCode = MatrixToImageWriter.toBufferedImage(bitMatrix);

        JTextField textField = new JTextField();
        textField.setSize(getWidth() - 20, 50);
        textField.setText(NodeService.getInstance().getAddress().toString());
        textField.setFont(Theme.TITLE_FONT);
        textField.setLocation(10, getHeight() - 150);
        textField.setHorizontalAlignment(0);
        textField.setEditable(false);
        textField.setBorder(BorderFactory.createEmptyBorder());
        textField.setBackground(Theme.FOREGROUND_COLOR);

        ButtonComponent button = new ButtonComponent(
                ResourceManager.loadImage("images/download.png"),
                Theme.FOREGROUND_COLOR,
                Theme.RECEIVE_SCREEN_BUTTON_COLOR
        );

        button.setSize(DOWNLOAD_BUTTON_WIDTH, DOWNLOAD_BUTTON_HEIGHT);
        button.setLocation(getWidth() / 2 + m_qrCode.getWidth() / 2, m_qrCode.getHeight() + 15 - button.getHeight());

        button.addButtonClickListener(() -> {
            try
            {
                while (true)
                {
                    final JFileChooser chooser = new JFileChooser();
                    chooser.setDialogType(JFileChooser.SAVE_DIALOG);
                    chooser.setSelectedFile(new File("address.png"));
                    chooser.setFileFilter(new FileNameExtensionFilter("png file","png"));

                    if (chooser.showSaveDialog(button) == JFileChooser.APPROVE_OPTION)
                    {
                        File file = new File(chooser.getSelectedFile().toString());

                        if (file.exists())
                        {
                            JOptionPane.showMessageDialog(null, "File already exists.");
                        }
                        else
                        {
                            ImageIO.write(m_address, "PNG", file);

                            ScreenManager.getInstance().showNotification("QR Saved",
                                    "Your address was saved as a QR image.",
                                    NotificationButtons.GotIt, result -> {});
                            break;
                        }
                    }
                    else
                    {
                        break;
                    }
                }
            }
            catch (IOException exception)
            {
                exception.printStackTrace();
            }
        });

        add(button);
        add(textField);
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

        graphics.setColor(Theme.PRIMARY_TEXT_COLOR);
        graphics.setFont(Theme.TITLE_FONT);

        String message = "Your Address";
        int width = graphics2d.getFontMetrics().stringWidth(message);

        graphics.drawString(message, getWidth() / 2 - width / 2, 40);

        graphics.drawImage(m_qrCode, getWidth() / 2 - m_qrCode.getWidth() / 2, 50, null);
    }

    /**
     * Paints this component's children. If shouldUseBuffer is true, no component ancestor has a buffer and the component
     * children can use a buffer if they have one. Otherwise, one ancestor has a buffer currently in use and children
     * should not use a buffer to paint.
     *
     * @param graphics the Graphics context in which to paint
     */
    public void paintChildren(Graphics graphics)
    {
        super.paintChildren(graphics);

        if (m_address == null)
        {
            m_address = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
            printAll(m_address.getGraphics());
        }
    }
}
