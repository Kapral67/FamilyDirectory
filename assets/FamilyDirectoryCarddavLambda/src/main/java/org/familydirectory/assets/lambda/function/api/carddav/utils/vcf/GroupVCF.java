package org.familydirectory.assets.lambda.function.api.carddav.utils.vcf;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.familydirectory.assets.ddb.models.member.IMemberRecord;
import org.jetbrains.annotations.NotNull;
import static java.time.Clock.systemUTC;
import static java.time.Instant.now;

@SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
@RequiredArgsConstructor
public final class GroupVCF extends AbstractVCF {

    private static final String X_ADDRESSBOOKSERVER = "X-ADDRESSBOOKSERVER";
    private static final String KIND = X_ADDRESSBOOKSERVER + "-KIND:group" + CRLF;
    private static final String MEMBER_FORMAT = X_ADDRESSBOOKSERVER + "-MEMBER:urn:uuid:%s" + CRLF;

    @NonNull
    private final String uniqueId;
    @NonNull
    private final String displayLabel;
    @NonNull
    private final Set<IMemberRecord> members;

    @Override
    protected
    String fn () {
        return this.displayLabel;
    }

    @Override
    protected
    String uid () {
        return this.uniqueId;
    }

    @Override
    protected
    Instant rev () {
        return now(systemUTC());
    }

    @Override
    @NotNull
    public
    String toString () {
        return this.createVCF(vcard -> {
            vcard.append(KIND);
            members.stream()
                   .map(IMemberRecord::id)
                   .map(UUID::toString)
                   .map(MEMBER_FORMAT::formatted)
                   .forEach(vcard::append);
        });
    }
}
