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
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LocalizedString;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.DialogWindow;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialogResultValidator;
import org.familydirectory.sdk.adminclient.AdminClientTui;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

/**
 * {@link com.googlecode.lanterna.gui2.dialogs.TextInputDialog}
 *
 * @author Martin
 * @author Maxwell Kapral
 */
public final
class SkippableTextInputDialog extends DialogWindow {

    @NotNull
    private final TextBox textBox;
    @Nullable
    private final TextInputDialogResultValidator validator;
    @Nullable
    private String result;

    public
    SkippableTextInputDialog (final @NotNull String title, final @Nullable String description, final boolean allowSkip, final @Nullable TextInputDialogResultValidator validator) {
        super(requireNonNull(title));
        this.setHints(AdminClientTui.EXTRA_WINDOW_HINTS);
        this.textBox = new TextBox();
        this.validator = validator;
        this.result = null;

        final Panel buttonPanel = new Panel();
        buttonPanel.setLayoutManager(new GridLayout(2).setHorizontalSpacing(1));
        buttonPanel.addComponent(
                new Button(LocalizedString.OK.toString(), this::onOK).setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.CENTER, true, false)));
        if (allowSkip) {
            buttonPanel.addComponent(new Button("Skip", this::close));
        }

        final Panel mainPanel = new Panel();
        mainPanel.setLayoutManager(new GridLayout(1).setLeftMarginSize(1)
                                                    .setRightMarginSize(1));
        if (nonNull(description)) {
            mainPanel.addComponent(new Label(description));
        }
        mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));
        this.textBox.setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.FILL, GridLayout.Alignment.CENTER, true, false))
                    .addTo(mainPanel);
        mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));
        buttonPanel.setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.END, GridLayout.Alignment.CENTER, false, false))
                   .addTo(mainPanel);
        this.setComponent(mainPanel);
    }

    private
    void onOK () {
        final String text = this.textBox.getText();
        if (nonNull(this.validator)) {
            final String errorMessage = this.validator.validate(text);
            if (nonNull(errorMessage)) {
                MessageDialog.showMessageDialog(this.getTextGUI(), this.getTitle(), errorMessage, MessageDialogButton.OK);
                return;
            }
        }
        this.result = text;
        this.close();
    }

    @Override
    @Nullable
    public
    String showDialog (final WindowBasedTextGUI textGUI) {
        this.result = null;
        super.showDialog(textGUI);
        return this.result;
    }
}
