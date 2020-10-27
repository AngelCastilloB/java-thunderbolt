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

package com.thunderbolt.theme;

/* IMPORTS ********************************************************************/

import com.thunderbolt.resources.ResourceManager;
import java.awt.*;

/* DECLARATION ***************************************************************/

/**
 * Application theme.
 */
public class Theme
{
    public static final Color DARK_PRIMARY_COLOR    = new Color(0x30, 0x3F, 0x9F);
    public static final Color DEFAULT_PRIMARY_COLOR = new Color(0x3F, 0x51, 0xB5);
    public static final Color LIGHT_PRIMARY_COLOR   = new Color(0xC5, 0xCA, 0xC9);
    public static final Color TEXT_PRIMARY_COLOR    = new Color(0xFF, 0xFF, 0xFF);
    public static final Color ACCENT_COLOR          = new Color(0x44, 0x8A, 0xFF);

    public static final Color PRIMARY_TEXT_COLOR    = new Color(26, 37, 57);
    public static final Color SECONDARY_TEXT_COLOR  = new Color(0x75, 0x75, 0x75);
    public static final Color DIVIDE_COLOR          = new Color(0xBD, 0xBD, 0xBD);

    public static final Color BACKGROUND_COLOR      = new Color(240, 240, 240);
    public static final Color FOREGROUND_COLOR      = new Color(253, 253, 253);
    public static final Color SHADOW_UMBRA_COLOR    = new Color(200, 200, 200);
    public static final Color SHADOW_PENUMBRA_COLOR = new Color(220, 220, 220);

    public static final Font  TITLE_FONT                  = ResourceManager.loadFont("fonts/Roboto-Regular.ttf", 16f);
    public static final Font  STATUS_FONT                 = ResourceManager.loadFont("fonts/Roboto-Regular.ttf", 16f);
    public static final Color STATUS_OFFLINE_COLOR        = new Color(210, 27, 3);
    public static final Color STATUS_SYNCING_COLOR        = new Color(255, 154, 0);
    public static final Color STATUS_READY_COLOR          = new Color(61, 137, 0);
    public static final String STATUS_READY_SOUND         = "audio/wallet_ready.wav";

    public static final Color  NOTIFICATION_BACKGROUND        = new Color(0, 0, 0, 128);
    public static final String NOTIFICATION_SOUND             = "audio/notification.wav";
    public static final Font   NOTIFICATION_TITLE_FONT        = ResourceManager.loadFont("fonts/Roboto-Bold.ttf", 18f);
    public static final Font   NOTIFICATION_TEXT              = ResourceManager.loadFont("fonts/Roboto-Regular.ttf", 16f);
    public static final Color  NOTIFICATION_BUTTON_TEXT       = new Color(0xFF, 0xFF, 0xFF);
    public static final Color  NOTIFICATION_BUTTON_BACKGROUND = new Color(0x30, 0x3F, 0x9F);
    public static final Color  NOTIFICATION_TILE_COLOR        = new Color(26, 37, 57);
    public static final Color  NOTIFICATION_TEXT_COLOR        = new Color(26, 37, 57);
    public static final Color  NOTIFICATION_BUTTON_TEXT_COLOR = new Color(0x75, 0x75, 0x75);

    public static final Color  MENU_BUTTON_BACKGROUND     = new Color(0, 0, 0, 30);
    public static final Color  MENU_BUTTON_ACTIVE         = FOREGROUND_COLOR;
    public static final Font   MENU_BUTTON_FONT           = ResourceManager.loadFont("fonts/Roboto-Regular.ttf", 13f);
    public static final Color  MENU_BUTTON_FONT_COLOR     = new Color(240, 240, 240);
    public static final Color  MENU_BUTTON_ACTIVE_COLOR   = Color.BLACK;
    public static final Color  MENU_BUTTON_INACTIVE_COLOR = new Color(240, 240, 240);
    public static final Font   MENU_OVERVIEW_FONT         = ResourceManager.loadFont("fonts/Roboto-Regular.ttf", 14f);
    public static final Color  MENU_OVERVIEW_FONT_COLOR   = new Color(240, 240, 240);
    public static final String MENU_BUTTON_CLICK_SOUND    = "audio/navigation_minimal.wav";
    public static final String MENU_BUTTON_INVALID_SOUND  = "audio/navigation_unavailable-selection.wav";
    public static final Font   MESSAGE_SCREEN_FONT        = ResourceManager.loadFont("fonts/Roboto-Regular.ttf", 24f);
    public static final Color  MESSAGE_SCREEN_COLOR       = new Color(0x75, 0x75, 0x75);

    public static final Font    ENCRYPT_SCREEN_FONT              = ResourceManager.loadFont("fonts/Roboto-Regular.ttf", 18f);
    public static final Color   ENCRYPT_SCREEN_TEXT_COLOR        = new Color(26, 37, 57);
    public static final Font    ENCRYPT_INPUT_FIELD_FONT          = ResourceManager.loadFont("fonts/Roboto-Regular.ttf", 32f);

    public static final Color RECEIVE_SCREEN_BUTTON_COLOR = Color.BLACK;

    public static final Color  TRANSACTION_COMPONENT_OUTGOING_COLOR = new Color(210, 27, 3);
    public static final Color  TRANSACTION_COMPONENT_PENDING_COLOR  = new Color(0x75, 0x75, 0x75);
    public static final Color  TRANSACTION_COMPONENT_INCOMING_COLOR = new Color(2, 33, 94);
    public static final Color  TRANSACTION_COMPONENT_SUBTEXT_COLOR  = new Color(0x75, 0x75, 0x75);
    public static final Font   TRANSACTION_COMPONENT_TITLE_FONT     = ResourceManager.loadFont("fonts/Roboto-Bold.ttf", 14f);
    public static final Font   TRANSACTION_COMPONENT_SUBTEXT_FONT   = ResourceManager.loadFont("fonts/Roboto-Regular.ttf", 12f);
    public static final String TRANSACTION_STATE_CHANGE_SOUND       = "audio/transaction_state_change.wav";

    public static final Color INPUT_FIELD_UNDERLINE_COLOR = new Color(0x30, 0x3F, 0x9F);
    public static final Color INPUT_FIELD_BACKGROUND_COLOR = FOREGROUND_COLOR;
    public static final Font  INPUT_FIELD_FONT              = ResourceManager.loadFont("fonts/Roboto-Regular.ttf", 12f);
}
