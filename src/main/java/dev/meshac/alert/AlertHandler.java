package dev.meshac.alert;

@FunctionalInterface
public interface AlertHandler {

    void onChange(Object previousValue, Object currentValue);
}
