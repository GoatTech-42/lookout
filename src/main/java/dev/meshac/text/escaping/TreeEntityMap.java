package dev.meshac.text.escaping;

import java.util.TreeMap;

public class TreeEntityMap extends BidirectionalEntityMap {

    public TreeEntityMap() {
        super(new TreeMap<>(), new TreeMap<>());
    }
}
