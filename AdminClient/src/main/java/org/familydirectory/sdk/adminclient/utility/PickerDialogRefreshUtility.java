package org.familydirectory.sdk.adminclient.utility;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.WaitingDialog;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.sdk.adminclient.AdminClientTui;
import org.familydirectory.sdk.adminclient.utility.dialogs.RefreshableListSelectDialog;
import org.familydirectory.sdk.adminclient.utility.pickers.model.PickerModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

public final
class PickerDialogRefreshUtility {
    @NotNull
    private final PickerModel picker;
    @NotNull
    private final String title;
    @Nullable
    private final String description;
    @NotNull
    private final String waitText;

    public
    PickerDialogRefreshUtility (final @NotNull PickerModel picker, final @NotNull String title, final @Nullable String description, final @NotNull String waitText)
    {
        super();
        this.picker = requireNonNull(picker);
        this.title = requireNonNull(title);
        this.description = description;
        this.waitText = requireNonNull(waitText);
    }

    @NotNull
    public
    MemberRecord showDialog (final @NotNull WindowBasedTextGUI gui) {
        final AtomicReference<List<MemberRecord>> contentRef = new AtomicReference<>();
        final AtomicReference<MemberRecord> memberRecordRef = new AtomicReference<>();
        final AtomicBoolean needsRefresh = new AtomicBoolean(false);
        do {
            final WaitingDialog waitDialog = WaitingDialog.createDialog(this.title, this.waitText);
            waitDialog.setHints(AdminClientTui.EXTRA_WINDOW_HINTS);
            waitDialog.showDialog(requireNonNull(gui), false);
            CompletableFuture.runAsync(() -> {
                                 if (needsRefresh.get()) {
                                     this.picker.refresh();
                                 }
                                 contentRef.set(this.picker.getEntries());
                                 waitDialog.close();
                             })
                             .exceptionally(e -> {
                                 throw new RuntimeException(e);
                             });
            waitDialog.waitUntilClosed();
            final RefreshableListSelectDialog<MemberRecord> listSelectDialog = new RefreshableListSelectDialog<>(this.title, this.description, contentRef.get(), new TerminalSize(20, 10));
            memberRecordRef.set(listSelectDialog.showDialog(gui));
            if (isNull(memberRecordRef.get())) {
                needsRefresh.set(true);
            }
        } while (isNull(memberRecordRef.get()));
        return memberRecordRef.get();
    }
}
