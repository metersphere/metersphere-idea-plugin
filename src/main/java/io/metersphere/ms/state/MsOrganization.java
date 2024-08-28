package io.metersphere.ms.state;

import lombok.Data;

import java.io.Serial;

@Data
public class MsOrganization implements java.io.Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

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
        if (obj instanceof MsOrganization) {
            return obj.hashCode() == this.hashCode();
        }
        return false;
    }

    public MsOrganization() {
    }

    public MsOrganization(String name, String id) {
        this.name = name;
        this.id = id;
    }
}
