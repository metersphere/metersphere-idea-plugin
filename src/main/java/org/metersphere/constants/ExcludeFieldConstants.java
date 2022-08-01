package org.metersphere.constants;

import java.util.ArrayList;
import java.util.List;

public class ExcludeFieldConstants {
    public static List<String> skipJavaTypes = new ArrayList<>() {{
        add("serialVersionUID".toLowerCase());
        add("optimisticLockVersion".toLowerCase());
        add("javax.servlet.http.HttpServletResponse");
        add("javax.servlet.http.HttpServletRequest");
    }};
}
