package org.metersphere.state;

import lombok.Data;

/**
 * @author guqing
 * @date 2022-01-24
 */
@Data
public class MSProjectVersion {
    private String name;
    private String id;
    private Boolean latest;

    public String toString() {
        if (this.latest != null && this.latest) {
            return this.name + " " + ("( latest )");
        }
        return this.name;
    }

}
