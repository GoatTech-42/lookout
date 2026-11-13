package dev.meshac.text.escaping;

import java.util.HashMap;

public class HashEntityMap extends BidirectionalEntityMap {

    public HashEntityMap() {
        super(new HashMap<>(), new HashMap<>());
    }
}
