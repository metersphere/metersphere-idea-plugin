package org.metersphere.state;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * @author guqing
 * @date 2022-01-24
 */
@Data
public class MSVersion {

    private String name;
    private String status;
    private Boolean latest;
}
