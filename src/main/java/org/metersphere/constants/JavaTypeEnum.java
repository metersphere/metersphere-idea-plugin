package org.metersphere.constants;

public enum JavaTypeEnum {

    OBJECT("object"),
    ARRAY("array"),
    ENUM("enum");
    private String name;

    JavaTypeEnum(String name) {
        this.name = name;
    }
}
