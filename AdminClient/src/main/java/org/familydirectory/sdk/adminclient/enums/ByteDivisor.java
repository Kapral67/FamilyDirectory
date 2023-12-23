package org.familydirectory.sdk.adminclient.enums;

public
enum ByteDivisor {
    NONE(1.0),
    KILO(1000.0),
    KIBI(1024.0),
    MEGA(Math.pow(KILO.divisor(), 2.0)),
    MEBI(Math.pow(KIBI.divisor(), 2.0)),
    GIGA(Math.pow(KILO.divisor(), 3.0)),
    GIBI(Math.pow(KIBI.divisor(), 3.0)),
    TERA(Math.pow(KILO.divisor(), 4.0)),
    TIBI(Math.pow(KIBI.divisor(), 4.0));

    private final double divisor;

    ByteDivisor (final double divisor) {
        this.divisor = divisor;
    }

    public final
    double divisor () {
        return this.divisor;
    }
}
