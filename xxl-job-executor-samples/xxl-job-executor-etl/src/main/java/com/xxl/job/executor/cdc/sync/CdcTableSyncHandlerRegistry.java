package com.xxl.job.executor.cdc.sync;

import com.xxl.job.executor.cdc.meta.CdcTableDef;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CdcTableSyncHandlerRegistry {
    private final Map<String, CdcTableSyncHandler> handlers;

    public CdcTableSyncHandlerRegistry(List<CdcTableSyncHandler> handlers) {
        this.handlers = handlers.stream().collect(Collectors.toUnmodifiableMap(
                handler -> handler.tableDef().getCaptureInstance(),
                Function.identity(),
                (left, right) -> {
                    throw new IllegalStateException("Duplicate CDC table sync handler: "
                            + left.tableDef().getCaptureInstance());
                }));
    }

    public CdcTableSyncHandler get(CdcTableDef tableDef) {
        return handlers.get(tableDef.getCaptureInstance());
    }
}
