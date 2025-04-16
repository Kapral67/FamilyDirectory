package org.familydirectory.assets.lambda.function.api.carddav.resource;

import java.util.Date;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.assets.lambda.function.api.helpers.CarddavLambdaHelper;
import org.jetbrains.annotations.NotNull;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

public final
class MemberResource extends AbstractResource {

    @NotNull
    private final MemberRecord member;

    public
    MemberResource (@NotNull CarddavLambdaHelper carddavLambdaHelper, @NotNull MemberRecord member) {
        super(carddavLambdaHelper);
        this.member = requireNonNull(member);
    }

    @Override
    public
    String getUniqueId () {
        return this.member.id().toString();
    }

    @Override
    @NotNull
    public
    Date getModifiedDate () {
        return Date.from(this.member.member().getLastModified());
    }

    @Override
    public
    boolean equals (final Object o) {
        if (this == o) {
            return true;
        } else if (isNull(o) || !this.getClass().equals(o.getClass())) {
            return false;
        }
        return this.member.equals(((MemberResource) o).member);
    }
}
