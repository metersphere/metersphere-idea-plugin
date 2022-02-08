package org.metersphere.state;

import lombok.Data;

@Data
public class MSWorkSpace {
    private String name;
    private String id;

    public String toString() {
        return this.name;
    }
}
