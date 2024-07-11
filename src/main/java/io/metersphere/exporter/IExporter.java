package io.metersphere.exporter;

import io.metersphere.model.ApiDefinition;

import java.util.List;

public interface IExporter {
    void sync(List<ApiDefinition> apis);
}
