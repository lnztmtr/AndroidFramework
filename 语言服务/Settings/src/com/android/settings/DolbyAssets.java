/*
 * This program is protected under international and U.S. copyright laws as
 * an unpublished work. These changes are confidential and proprietary to the
 * copyright owners. Reproduction or disclosure, in whole or in part, or the
 * production of derivative works therefrom without the express permission of
 * the copyright owners is prohibited.
 *
 *                Copyright (C) 2011-2012 by Dolby Laboratories,
 *                            All rights reserved.
 */
package com.android.settings;

import android.content.Context;
import android.graphics.Typeface;

public class DolbyAssets {

    private static Typeface sFont;

    public static void init(Context context) {
        if (sFont == null) {
            sFont = Typeface.createFromAsset(context.getAssets(),
                    "fonts/Roboto-Regular.ttf");
        }
    }

    public static final Typeface getFont() {
        return sFont;
    }
}
