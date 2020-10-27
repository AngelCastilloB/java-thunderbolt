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

import com.thunderbolt.common.Convert;
import com.thunderbolt.screens.ScreenManager;
import com.thunderbolt.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.ParseException;

/* IMPLEMENTATION ************************************************************/

/**
 * Component that handles user input in text form.
 */
public class InputComponent extends JComponent
{
    private static final int MARGIN = 5;
    private static final int UNDERLINE_SIZE = 1;

    private final JComponent    m_field;
    private final IInputHandler m_handler;
    private String              m_title = "";

    /**
     * Initializes a new instance of the InputComponent class.
     *
     * @param type The input type.
     * @param handler The handler for when the input is ready.
     */
    public InputComponent(InputType type, IInputHandler handler)
    {
        setLayout(null);

        m_handler = handler;

        switch (type)
        {
            case Password:
                m_field = new JPasswordField();
                break;
            case Numbers:
                double min = 0.00000000 ;
                double max = 21000000.0;
                double step = 0.00000001;
                double i = 0.0000000;
                SpinnerModel value = new SpinnerNumberModel(i, min, max, step);

                JSpinner spinner = new JSpinner(value);
                JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "#.########");
                spinner.setEditor(editor);
                m_field = spinner;
             break;
            default:
            case PlainText:
                m_field = new JTextField();
        }
        m_field.addKeyListener(new KeyAdapter()
        {
            public void keyPressed(KeyEvent evt)
            {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER)
                {
                    if (m_handler != null && !ScreenManager.getInstance().isNotificationShown())
                        m_handler.onInput(getValue());
                }
            }
        });

        setBackground(Theme.INPUT_FIELD_BACKGROUND_COLOR);
        m_field.setFont(Theme.INPUT_FIELD_CONTENT_FONT);
        m_field.setBackground(Theme.INPUT_FIELD_BACKGROUND_COLOR);
        m_field.requestFocusInWindow();
        m_field.grabFocus();
        m_field.setBorder(BorderFactory.createEmptyBorder());

        add(m_field);
    }

    /**
     * Initializes a new instance of the InputComponent class.
     *
     * @param type The input type.
     */
    public InputComponent(InputType type)
    {
        this(type, null);
    }

    /**
     * Sets the title fo the component.
     *
     * @param title the title.
     */
    public void setTile(String title)
    {
        m_title = title;
    }

    /**
     * Sets the text of the input field.
     *
     * @param value The text.
     */
    public void setText(String value)
    {
        if (m_field instanceof JTextField)
            ((JTextField) m_field).setText(value);

        ((JSpinner)m_field).setValue(Double.valueOf(value));
    }

    /**
     * Gets the text from this input field.
     *
     * @return The text.
     */
    public String getText()
    {
        return getValue();
    }

    /**
     * Grabs the focus.
     */
    @Override
    public void grabFocus()
    {
        super.grabFocus();
        m_field.grabFocus();
    }

    /**
     * Requests the focus from the frame.
     */
    @Override
    public void requestFocus()
    {
        super.requestFocus();
        m_field.requestFocusInWindow();
    }

    /**
     * Sets the size of the component.
     *
     * @param width The width.
     * @param height The height.
     */
    @Override
    public void setSize(int width, int height)
    {
        super.setSize(width, height);
        m_field.setSize(width, height - (MARGIN * 2) - 10);
    }

    /**
     * Sets the location of this component.
     *
     * @param x The x location.
     * @param y The Y location.
     */
    @Override
    public void setLocation(int x, int y)
    {
        super.setLocation(x, y);
        m_field.setLocation(0, MARGIN + 10);
    }

    /**
     * Sets the font of this component.
     *
     * @param font The font.
     */
    @Override
    public void setFont(Font font)
    {
        m_field.setFont(font);
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

        graphics.setColor(Theme.INPUT_FIELD_BACKGROUND_COLOR);
        graphics2d.fillRect(0, 0, getWidth(), getHeight());
        graphics2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        graphics2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        graphics.setFont(Theme.INPUT_FIELD_FONT);
        graphics.setColor(Theme.INPUT_FIELD_UNDERLINE_COLOR);
        graphics.drawString(m_title, 0, 10);

        graphics.setColor(Theme.INPUT_FIELD_UNDERLINE_COLOR);
        graphics.drawLine(0, getHeight() - UNDERLINE_SIZE, getWidth(), getHeight() - UNDERLINE_SIZE);
    }

    /**
     * Gets the value of the underlying component.
     *
     * @return The value.
     */
    private String getValue()
    {
        if (m_field instanceof JTextField)
            return ((JTextField) m_field).getText();

        try
        {
            ((JSpinner)m_field).commitEdit();
        }
        catch (ParseException e)
        {
            return Convert.stripTrailingZeros((double)((JSpinner)m_field).getValue());
        }

        return Convert.stripTrailingZeros((double)((JSpinner)m_field).getValue());
    }
}

