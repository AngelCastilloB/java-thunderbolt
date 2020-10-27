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
import com.thunderbolt.resources.ResourceManager;
import com.thunderbolt.state.NodeService;
import com.thunderbolt.theme.Theme;
import com.thunderbolt.worksapce.NotificationButtons;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/* IMPLEMENTATION ************************************************************/

public class DumpKeyScreen extends ScreenBase
{
    private static final int QR_WIDTH               = 200;
    private static final int QR_HEIGHT              = 200;
    private static final int DOWNLOAD_BUTTON_HEIGHT = 30;
    private static final int DOWNLOAD_BUTTON_WIDTH  = 40;

    private BufferedImage m_publicKeyQrCode  = null;
    private BufferedImage m_privateKeyQrCode = null;
    private BufferedImage m_dumpedKeys       = null;

    public DumpKeyScreen() throws WriterException
    {
        setTitle("DUMP KEYS");
        setBackground(Theme.FOREGROUND_COLOR);

        String publicKey = NodeService.getInstance().getPublicKey();
        String privateKey = NodeService.getInstance().getPrivateKey();

        // Lock the wallet after we got the sensitive data.
        NodeService.getInstance().lockWallet();

        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        BitMatrix bitMatrix = qrCodeWriter.encode(publicKey,
                BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT);

        m_publicKeyQrCode = MatrixToImageWriter.toBufferedImage(bitMatrix);

        bitMatrix = qrCodeWriter.encode(privateKey,
                BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT);

        m_privateKeyQrCode = MatrixToImageWriter.toBufferedImage(bitMatrix);


        JTextField publicKeyTextField = new JTextField();
        publicKeyTextField.setSize(getWidth() - 50, 50);
        publicKeyTextField.setText(publicKey);
        publicKeyTextField.setFont(Theme.TITLE_FONT);
        publicKeyTextField.setLocation(40, 220);
        publicKeyTextField.setEditable(false);
        publicKeyTextField.setBorder(BorderFactory.createEmptyBorder());
        publicKeyTextField.setBackground(Theme.FOREGROUND_COLOR);

        JTextField privateKeyTextField = new JTextField();
        privateKeyTextField.setSize(getWidth() - 100, 50);
        privateKeyTextField.setText(privateKey);
        privateKeyTextField.setFont(Theme.TITLE_FONT);
        privateKeyTextField.setLocation(getWidth() - privateKeyTextField.getWidth() - 5, getHeight() - privateKeyTextField.getHeight() - 2);
        privateKeyTextField.setEditable(false);
        privateKeyTextField.setBorder(BorderFactory.createEmptyBorder());
        privateKeyTextField.setBackground(Theme.FOREGROUND_COLOR);


        ButtonComponent button = new ButtonComponent(
                ResourceManager.loadImage("images/download.png"),
                Theme.FOREGROUND_COLOR,
                Theme.RECEIVE_SCREEN_BUTTON_COLOR
        );

        button.setSize(DOWNLOAD_BUTTON_WIDTH, DOWNLOAD_BUTTON_HEIGHT);
        button.setLocation(getWidth() - 80, 20);

        button.addButtonClickListener(() -> {
            try
            {
                while (true)
                {
                    final JFileChooser chooser = new JFileChooser();
                    chooser.setDialogType(JFileChooser.SAVE_DIALOG);
                    chooser.setSelectedFile(new File("keys.png"));
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
                            ImageIO.write(m_dumpedKeys, "PNG", file);

                            ScreenManager.getInstance().showNotification("Keys Saved",
                                    "Your keys were saved as an image.",
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
        add(publicKeyTextField);
        add(privateKeyTextField);
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

        graphics.drawImage(m_publicKeyQrCode, 15, 40, null);
        String message = "PUBLIC KEY";
        graphics.drawString(message, 40, 40);

        graphics.setColor(Theme.PRIMARY_TEXT_COLOR);
        graphics.setFont(Theme.TITLE_FONT);

        graphics.drawImage(m_privateKeyQrCode, getWidth() - m_privateKeyQrCode.getWidth() - 5, 300 , null);
        message = "PRIVATE KEY";
        graphics.drawString(message, getWidth() - 130, 300);
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

        if (m_dumpedKeys == null)
        {
            m_dumpedKeys = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
            printAll(m_dumpedKeys.getGraphics());
        }
    }
}
