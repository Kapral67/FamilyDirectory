package org.familydirectory.sdk.adminclient.utility.pickers.model;

import java.util.Comparator;
import org.familydirectory.assets.ddb.models.member.MemberRecord;

public
interface PickerModel {
    Comparator<MemberRecord> LAST_NAME_COMPARATOR = Comparator.comparing(memberRecord -> memberRecord.member()
                                                                                                     .getLastName());
}
