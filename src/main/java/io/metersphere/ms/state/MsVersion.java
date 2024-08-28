package io.metersphere.ms.state;

import lombok.Data;

@Data
public class MsVersion {
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
        if (obj instanceof MsVersion) {
            if (obj.hashCode() == this.hashCode()) {
                return true;
            }
        }
        return false;
    }

}
