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
package org.familydirectory.sdk.adminclient.utility.dialogs;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.AnimatedLabel;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Panels;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.DialogWindow;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

/**
 * {@link com.googlecode.lanterna.gui2.dialogs.ListSelectDialog}
 *
 * @author Martin
 * @author Maxwell Kapral
 */
public final
class RefreshableListSelectDialog<T> extends DialogWindow {
    @Nullable
    private final String description;
    @Nullable
    private final String waitText;
    @NotNull
    private final Supplier<List<T>> contentSupplier;
    @NotNull
    private final Runnable refreshAction;
    @Nullable
    private Thread preStartedRefreshAction;
    @Nullable
    private T result;

    public
    RefreshableListSelectDialog (final @NotNull String title, final @Nullable String description, final @Nullable String waitText, final @NotNull Supplier<List<T>> contentSupplier,
                                 final @NotNull Runnable refreshAction, final @Nullable Thread preStartedRefreshAction)
    {
        super(title);
        this.setHints(Set.of(Hint.MODAL, Hint.CENTERED));
        this.result = null;
        this.description = description;
        this.waitText = waitText;
        this.contentSupplier = requireNonNull(contentSupplier);
        this.refreshAction = requireNonNull(refreshAction);
        this.preStartedRefreshAction = preStartedRefreshAction;
        this.refresh(false);
    }

    private
    void refresh (final boolean doRunAction) {
        this.setComponent(Panels.horizontal(new Label(this.waitText), AnimatedLabel.createClassicSpinningLine()));
        if (nonNull(this.preStartedRefreshAction)) {
            if (this.preStartedRefreshAction.isAlive()) {
                try {
                    this.preStartedRefreshAction.join();
                } catch (final InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            this.preStartedRefreshAction = null;
        }
        if (doRunAction) {
            this.refreshAction.run();
        }
        this.loadContent(!doRunAction);
    }

    private
    void loadContent (final boolean isRefreshable) {
        final List<T> content = this.contentSupplier.get();
        if (content.isEmpty()) {
            if (isRefreshable) {
                this.refreshAction.run();
            } else {
                throw new IllegalArgumentException("content may not be empty");
            }
        }

        final ActionListBox listBox = new ActionListBox();
        content.forEach(item -> listBox.addItem(item.toString(), () -> this.onSelect(item)));

        final Panel mainPanel = new Panel();
        mainPanel.setLayoutManager(new GridLayout(1).setLeftMarginSize(1)
                                                    .setRightMarginSize(1));
        if (nonNull(this.description)) {
            mainPanel.addComponent(new Label(this.description));
            mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));
        }
        listBox.setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.FILL, GridLayout.Alignment.CENTER, true, false))
               .addTo(mainPanel);
        mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));

        final Panel buttonPanel = new Panel();
        buttonPanel.setLayoutManager(new GridLayout(2).setHorizontalSpacing(1));
        buttonPanel.addComponent(new Button("Refresh", () -> this.refresh(true)).setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.CENTER, true, false)));
        buttonPanel.setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.END, GridLayout.Alignment.CENTER, false, false))
                   .addTo(mainPanel);

        this.setComponent(mainPanel);
    }

    private
    void onSelect (final T item) {
        this.result = item;
        this.close();
    }

    @Override
    public
    T showDialog (final WindowBasedTextGUI textGUI) {
        this.result = null;
        super.showDialog(textGUI);
        return this.result;
    }
}
