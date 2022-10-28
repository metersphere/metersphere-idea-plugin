package org.metersphere.state;

import lombok.Data;

@Data
public class MSWorkSpace {
    private String name;
    private String id;

    public String toString() {
        return this.name;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MSWorkSpace) {
            if (obj.hashCode() == this.hashCode()) {
                return true;
            }
        }
        return false;
    }
}
