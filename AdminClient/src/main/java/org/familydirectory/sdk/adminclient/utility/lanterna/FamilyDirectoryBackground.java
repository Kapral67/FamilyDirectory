/*
 * This file is part of lanterna (https://github.com/mabe02/lanterna).
 *
 * lanterna is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2010-2020 Martin Berglund
 *
 * Copyright (C) 2024 Maxwell Kapral
 */
package org.familydirectory.sdk.adminclient.utility.lanterna;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.ComponentRenderer;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import org.familydirectory.sdk.adminclient.AdminClientTui;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.util.Objects.isNull;
import static org.familydirectory.assets.Constants.VERSION_STR;

/**
 * {@link com.googlecode.lanterna.gui2.GUIBackdrop}
 * <br/>
 * <a href="https://raw.githubusercontent.com/mabe02/lanterna/master/src/test/java/com/googlecode/lanterna/gui2/WelcomeSplashTest.java">com.googlecode.lanterna.gui2.WelcomeSplashTest</a>
 *
 * @author Martin
 * @author Maxwell Kapral
 */
public final
class FamilyDirectoryBackground extends EmptySpace {
    @Nullable
    private final TextColor.ANSI textColor;
    @Nullable
    private final TextColor.ANSI backgroundColor;

    public
    FamilyDirectoryBackground (final @Nullable TextColor.ANSI textColor, final @Nullable TextColor.ANSI backgroundColor) {
        super();
        this.textColor = textColor;
        this.backgroundColor = backgroundColor;
    }

    @Contract(value = " -> new", pure = true)
    @Override
    @NotNull
    protected
    ComponentRenderer<EmptySpace> createDefaultRenderer () {
        final boolean text = false;
        return new ComponentRenderer<>() {
            @Override
            @NotNull
            public
            TerminalSize getPreferredSize (final @Nullable EmptySpace ignored) {
                return TerminalSize.ONE;
            }

            @Override
            public
            void drawComponent (final @NotNull TextGUIGraphics graphics, final @Nullable EmptySpace ignored) {
                graphics.setForegroundColor(isNull(FamilyDirectoryBackground.this.textColor)
                                                    ? TextColor.ANSI.BLACK
                                                    : FamilyDirectoryBackground.this.textColor);
                graphics.setBackgroundColor(isNull(FamilyDirectoryBackground.this.backgroundColor)
                                                    ? TextColor.ANSI.BLUE_BRIGHT
                                                    : FamilyDirectoryBackground.this.backgroundColor);
                graphics.fill(' ');
                final TerminalSize terminalSize = graphics.getSize();
                final String awsId = "AWS ACCOUNT ID: " + AdminClientTui.AWS_ACCOUNT_ID;
                final String awsRegion = "AWS REGION: " + AdminClientTui.AWS_REGION;
                final String version = 'v' + VERSION_STR;
                final boolean widthCheck = (awsId.length() + awsRegion.length() + 1) < terminalSize.getColumns();
                graphics.putString(0, 0, awsId);
                final int awsRegionColumn = widthCheck
                        ? terminalSize.getColumns() - awsRegion.length()
                        : 0;
                final int awsRegionRow = widthCheck
                        ? 0
                        : 1;
                graphics.putString(awsRegionColumn, awsRegionRow, awsRegion);
                graphics.putString(terminalSize.getColumns() - version.length(), terminalSize.getRows() - 1, version);
            }
        };
    }
}