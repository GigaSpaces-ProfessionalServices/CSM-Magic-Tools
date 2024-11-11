package com.gigaspaces.heap.space;

import java.util.HashMap;
import java.util.Map;

public class SpaceReport {
    private final String spaceName;
    private final String instanceId;
    private final String heapUsageEstimatorDesc;
    private final Map<String, SpaceTypeReport> typeReportMap = new HashMap<>();

    public SpaceReport(String spaceName, String instanceId, String heapUsageEstimatorDesc) {
        this.spaceName = spaceName;
        this.instanceId = instanceId;
        this.heapUsageEstimatorDesc = heapUsageEstimatorDesc;
    }

    public void add(SpaceTypeReport typeReport) {
        typeReportMap.put(typeReport.getTypeName(), typeReport);
    }

    public Map<String, SpaceTypeReport> getTypeReportMap() {
        return typeReportMap;
    }

    public String getSpaceName() {
        return spaceName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getHeapUsageEstimatorDesc() {
        return heapUsageEstimatorDesc;
    }
}
