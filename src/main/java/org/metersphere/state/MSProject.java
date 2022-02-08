package org.metersphere.state;

import lombok.Data;

@Data
public class MSProject {
    private String name;
    private String id;
    private Boolean versionEnable;

    public String toString() {
        return this.name;
    }
}
