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
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.sdk.adminclient.utility.pickers.model.PickerModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

public final
class PickerListSelectDialog extends DialogWindow {
    @NotNull
    private final PickerModel picker;
    @Nullable
    private final String description;
    @Nullable
    private final String waitText;
    @Nullable
    private MemberRecord result;

    public
    PickerListSelectDialog (final @NotNull PickerModel picker, final @NotNull String title, final @Nullable String description, final @Nullable String waitText)
    {
        super(requireNonNull(title));
        this.setHints(Set.of(Hint.MODAL, Hint.CENTERED));
        this.picker = requireNonNull(picker);
        this.result = null;
        this.description = description;
        this.waitText = waitText;
        this.refresh(false);
    }

    private
    void refresh (final boolean refresh) {
        this.setComponent(Panels.horizontal(new Label(this.waitText), AnimatedLabel.createClassicSpinningLine()));
        if (refresh) {
            this.picker.refresh();
        }
        this.loadContent();
    }

    private
    void loadContent () {
        if (this.picker.isEmpty()) {
            throw new IllegalArgumentException("content may not be empty");
        }

        final List<MemberRecord> entries = this.picker.getEntries();

        final ActionListBox listBox = new ActionListBox();
        entries.forEach(item -> listBox.addItem(item.toString(), () -> this.onSelect(item)));

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
    void onSelect (final MemberRecord item) {
        this.result = item;
        this.close();
    }

    @Override
    public
    MemberRecord showDialog (final WindowBasedTextGUI textGUI) {
        this.result = null;
        super.showDialog(textGUI);
        return this.result;
    }
}
