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
import com.thunderbolt.state.NodeService;
import com.thunderbolt.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;

/* IMPLEMENTATION ************************************************************/

public class ReceiveScreen extends ScreenBase
{
    private BufferedImage m_qrCode = null;

    public ReceiveScreen() throws WriterException, IOException
    {
        setTitle("RECEIVE");

        setLayout(null);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        BitMatrix bitMatrix = qrCodeWriter.encode(NodeService.getInstance().getAddress(),
                BarcodeFormat.QR_CODE, 300, 300);

        m_qrCode = MatrixToImageWriter.toBufferedImage(bitMatrix);

        setBackground(Theme.FOREGROUND_COLOR);
        JTextField label = new JTextField();
        label.setSize(getWidth() - 20, 50);
        label.setText(NodeService.getInstance().getAddress());
        label.setFont(Theme.TITLE_FONT);
        label.setLocation(10, getHeight() - 150);
        label.setHorizontalAlignment(0);
        label.setEditable(false);
        label.setBorder(BorderFactory.createEmptyBorder());
        label.setBackground(Theme.FOREGROUND_COLOR);

        final JFileChooser fc = new JFileChooser();
        int returnVal = fc.showSaveDialog(this);

        add(label);
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
}
