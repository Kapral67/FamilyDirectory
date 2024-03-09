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
 */
package org.familydirectory.sdk.adminclient.utility.lanterna;

import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Panels;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.DialogWindow;
import org.jetbrains.annotations.NotNull;

/**
 * {@link com.googlecode.lanterna.gui2.dialogs.WaitingDialog}
 * <p>
 * Dialog that displays a text message, an optional spinning indicator and an optional progress bar. There is no buttons
 * in this dialog so it has to be explicitly closed through code.
 *
 * @author martin
 * <p>
 * <p>
 * TODO: Delete this class
 * Temporary Fix for <a href="https://github.com/mabe02/lanterna/issues/595">lantera issue #595</a>
 * @author Maxwell Kapral
 */
public final
class WaitingDialog extends DialogWindow {
    @NotNull
    private final AnimatedLabel2 spinningLine;

    private
    WaitingDialog (final String title, final String text) {
        super(title);

        this.spinningLine = AnimatedLabel2.createClassicSpinningLine();
        final Panel mainPanel = Panels.horizontal(new Label(text), this.spinningLine);
        this.setComponent(mainPanel);
    }

    /**
     * Creates and displays a waiting dialog without blocking for it to finish
     *
     * @param textGUI GUI to add the dialog to
     * @param title   Title of the waiting dialog
     * @param text    Text to display on the waiting dialog
     *
     * @return Created waiting dialog
     */
    public static
    WaitingDialog showDialog (final WindowBasedTextGUI textGUI, final String title, final String text) {
        final WaitingDialog waitingDialog = createDialog(title, text);
        waitingDialog.showDialog(textGUI, false);
        return waitingDialog;
    }

    /**
     * Creates a new waiting dialog
     *
     * @param title Title of the waiting dialog
     * @param text  Text to display on the waiting dialog
     *
     * @return Created waiting dialog
     */
    public static
    WaitingDialog createDialog (final String title, final String text) {
        return new WaitingDialog(title, text);
    }

    /**
     * Displays the waiting dialog and optionally blocks until another thread closes it
     *
     * @param textGUI          GUI to add the dialog to
     * @param blockUntilClosed If {@code true}, the method call will block until another thread calls {@code close()} on
     *                         the dialog, otherwise the method call returns immediately
     */
    public
    void showDialog (final WindowBasedTextGUI textGUI, final boolean blockUntilClosed) {
        textGUI.addWindow(this);

        if (blockUntilClosed) {
            //Wait for the window to close, in case the window manager doesn't honor the MODAL hint
            this.waitUntilClosed();
        }
    }

    @Override
    public
    Object showDialog (final WindowBasedTextGUI textGUI) {
        this.showDialog(textGUI, true);
        return null;
    }

    /**
     * TODO: Delete this once <a href="https://github.com/mabe02/lanterna/issues/595">lantera issue #595</a> is resolved
     */
//    @Override
//    public
//    void close () {
//        super.close();
//        this.spinningLine.stopAnimation();
//    }
}