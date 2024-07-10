package io.metersphere.exporter;

import io.metersphere.model.Api;

import java.util.List;

public interface IExporter {
    void sync(List<Api> apis);
}
