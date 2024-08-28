package io.metersphere.ms.state;

import lombok.Data;

import java.io.Serial;

@Data
public class MsModule implements java.io.Serializable{
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
        if (obj instanceof MsModule) {
            return obj.hashCode() == this.hashCode();
        }
        return false;
    }
}
