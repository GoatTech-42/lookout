package dev.meshac.time.format.rule;

import java.util.Calendar;

public interface FormatRule {

    int estimateLength();

    void appendTo(StringBuffer buffer, Calendar calendar);
}
