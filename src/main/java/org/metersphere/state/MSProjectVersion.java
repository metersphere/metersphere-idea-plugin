package org.metersphere.state;

import lombok.Data;

/**
 * @author guqing
 * @date 2022-01-24
 */
@Data
public class MSProjectVersion {

    private String name;
    private String status;
    private Boolean latest;
}
