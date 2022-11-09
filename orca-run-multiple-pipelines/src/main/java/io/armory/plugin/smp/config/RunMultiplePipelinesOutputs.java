package io.armory.plugin.smp.config;

import lombok.Data;

@Data
public class RunMultiplePipelinesOutputs {
    private String id;
    private String executionIdentifier;
    private Long startTime;
    private Long endTime;
    private String status;
    private ArtifactCreated artifactCreated;

    @Data
    public static class ArtifactCreated {
        private String account;
        private String manifestName;
        private String location;
    }
}

