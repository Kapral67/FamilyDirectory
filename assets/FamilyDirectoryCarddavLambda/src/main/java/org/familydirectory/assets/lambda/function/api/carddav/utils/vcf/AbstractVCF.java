package org.familydirectory.assets.lambda.function.api.carddav.utils.vcf;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

@SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
public sealed abstract class AbstractVCF permits ContactVCF, GroupVCF {
    protected static final DateTimeFormatter VCARD_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    protected static final String CRLF = "\r\n";

    private static final String BEGIN = "BEGIN:VCARD" + CRLF;
    private static final String VERSION = "VERSION:3.0" + CRLF;
    private static final String FN_FORMAT = "FN:%s" + CRLF;
    private static final String UID_FORMAT = "UID:%s" + CRLF;
    private static final String REV_FORMAT = "REV:%s" + CRLF;
    private static final String END = "END:VCARD" + CRLF;

    protected abstract String fn();
    protected abstract String uid();
    protected abstract Instant rev();

    protected String createVCF (Consumer<StringBuilder> vcfBodyWriter) {
        final StringBuilder vcf = new StringBuilder();
        vcf.append(BEGIN);
        vcf.append(VERSION);
        vcf.append(FN_FORMAT.formatted(this.fn()));
        vcf.append(UID_FORMAT.formatted(this.uid()));
        vcf.append(REV_FORMAT.formatted(this.rev().toString()));
        vcfBodyWriter.accept(vcf);
        vcf.append(END);
        return vcf.toString();
    }
}
