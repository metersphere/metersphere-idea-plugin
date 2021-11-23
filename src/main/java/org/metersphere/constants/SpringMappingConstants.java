package org.metersphere.constants;

import java.util.ArrayList;
import java.util.List;

public class SpringMappingConstants {
    public static List<String> mapList = new ArrayList<>(){{
        add("org.springframework.web.bind.annotation.DeleteMapping");
        add("org.springframework.web.bind.annotation.GetMapping");
        add("org.springframework.web.bind.annotation.PatchMapping");
        add("org.springframework.web.bind.annotation.PostMapping");
        add("org.springframework.web.bind.annotation.PutMapping");
        add("org.springframework.web.bind.annotation.RequestMapping");
    }};
}
