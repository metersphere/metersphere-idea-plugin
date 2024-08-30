package io.metersphere.component.state;

import lombok.Data;

import java.io.Serial;

@Data
public class MsProject implements java.io.Serializable{
    @Serial
    private static final long serialVersionUID = 1L;

    private String name;
    private String id;
    private Boolean versionEnable;

    public String toString() {
        return this.name;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MsProject) {
            return obj.hashCode() == this.hashCode();
        }
        return false;
    }
}
