package org.familydirectory.sdk.adminclient.utility.pickers.model;

import java.util.Comparator;
import java.util.List;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

public
interface PickerModel extends Runnable {
    Comparator<MemberRecord> LAST_NAME_COMPARATOR = Comparator.comparing(memberRecord -> memberRecord.member()
                                                                                                     .getLastName());

    boolean isEmpty ();

    void removeEntry (final @NotNull MemberRecord memberRecord);

    void addEntry (final @NotNull MemberRecord memberRecord);

    @Contract(pure = true)
    @NotNull @UnmodifiableView
    List<MemberRecord> getEntries ();
}
