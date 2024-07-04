package io.metersphere.state;

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

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MSProjectVersion) {
            if (obj.hashCode() == this.hashCode()) {
                return true;
            }
        }
        return false;
    }

}
