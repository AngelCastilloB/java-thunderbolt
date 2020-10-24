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

package com.thunderbolt.resources;

/* IMPORTS *******************************************************************/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/* IMPLEMENTATION ************************************************************/

/**
 * Resources manager class.
 */
public class ResourceManager
{
    private static final Logger              s_logger     = LoggerFactory.getLogger(ResourceManager.class);
    private static final Map<String, byte[]> s_imageCache = new HashMap<>();
    private static final Map<String, Font>   s_fontCache  = new HashMap<>();
    private static final Map<String, byte[]> s_audioCache = new HashMap<>();

    /**
     * Loads a font from the resources folder.
     *
     * @param font The font to be loaded.
     * @param size The size.
     *
     * @return The new loaded font.
     */
    static public Font loadFont(String font, float size)
    {
        Font result = null;
        String key = String.format("%s%s", font, size);
        
        try
        {
            if (s_fontCache.containsKey(key))
                return s_fontCache.get(key);

            result = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(ResourceManager.class.getClassLoader()
                    .getResourceAsStream(font))).deriveFont(size);

            s_fontCache.put(key, result);
        }
        catch (FontFormatException | IOException e)
        {
            throw new IllegalStateException(String.format("Resource %s could not be loaded.", font), e);
        }

        return result;
    }

    /**
     * Loads an image resource from the resource folder.
     *
     * @param image The name of the image to be loaded.
     *
     * @return A BufferedImage image instance.
     */
    static public BufferedImage loadImage(String image)
    {
        BufferedImage result = null;

        try
        {
            if (!s_imageCache.containsKey(image))
            {
                InputStream stream = ResourceManager.class.getClassLoader().getResourceAsStream(image);

                if (stream == null)
                    throw new IllegalStateException(String.format("Resource %s not found.", image));

                s_imageCache.put(image, stream.readAllBytes());
            }

            result = ImageIO.read(new ByteArrayInputStream(s_imageCache.get(image)));
        }
        catch (IOException exception)
        {
            throw new IllegalStateException(String.format("Resource %s could not be loaded.", image), exception);
        }

        return result;
    }

    /**
     * Plays the given audio from the resources folder.
     *
     * @param audio The audio to be played.
     */
    static public void playAudio(String audio)
    {
        try
        {
            if (!s_audioCache.containsKey(audio))
            {
                InputStream stream = ResourceManager.class.getClassLoader().getResourceAsStream(audio);

                if (stream == null)
                    throw new IllegalStateException(String.format("Resource %s not found.", audio));

                s_audioCache.put(audio, stream.readAllBytes());
            }

            AudioInputStream audioIn = AudioSystem.getAudioInputStream(new ByteArrayInputStream(s_audioCache.get(audio)));
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        }
        catch (UnsupportedAudioFileException | IOException | LineUnavailableException e)
        {
            s_logger.error("There was an error while trying to play the audio file {}", audio, e);
        }
    }
}
