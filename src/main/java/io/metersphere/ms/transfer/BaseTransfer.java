package io.metersphere.ms.transfer;


import io.metersphere.common.model.ApiDefinition;
import java.util.*;

public interface BaseTransfer {
    void upload(List<ApiDefinition> apis);
}
