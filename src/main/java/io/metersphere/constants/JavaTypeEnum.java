package io.metersphere.constants;

public enum JavaTypeEnum {

    //对象
    OBJECT("object"),
    //数组
    ARRAY("array"),
    //基本数据类型或者枚举类型
    ENUM("enum");
    private String name;

    JavaTypeEnum(String name) {
        this.name = name;
    }
}
