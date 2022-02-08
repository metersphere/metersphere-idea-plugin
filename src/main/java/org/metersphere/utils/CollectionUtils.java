package org.metersphere.utils;

import java.util.Collection;

public class CollectionUtils {
    public static boolean isNotEmpty(Collection coll) {
        return !isEmpty(coll);
    }

    public static boolean isEmpty(Collection coll) {
        return coll == null || coll.isEmpty();
    }
}
