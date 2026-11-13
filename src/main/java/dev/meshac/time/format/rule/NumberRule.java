package dev.meshac.time.format.rule;

public interface NumberRule extends FormatRule {

    void appendTo(StringBuffer stringBuffer, int index);
}
