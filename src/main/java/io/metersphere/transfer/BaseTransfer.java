package io.metersphere.transfer;

import io.metersphere.model.ApiDefinition;

import java.util.List;

public interface BaseTransfer {
    void upload(List<ApiDefinition> apis);
}
