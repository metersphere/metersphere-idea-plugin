package io.metersphere.constants;

import java.util.HashMap;
import java.util.Map;

public class PluginConstants {
    public static final String EXPORTER_MS = "MeterSphere";


    public enum MessageTitle {
        Info, Warning, Error
    }

    public static Map<Integer, String> EXCEPTIONCODEMAP = new HashMap<>() {{
        put(1, "please input correct ak sk!");
        put(2, "No java file detected! please change your search root");
        put(3, "No java api was found! please change your search root");
    }};

}
