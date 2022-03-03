/*
 * Copyright 2020 Armory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.armory.plugin.smp.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.netflix.spinnaker.orca.api.pipeline.Task;
import com.netflix.spinnaker.orca.api.pipeline.TaskResult;
import com.netflix.spinnaker.orca.api.pipeline.models.ExecutionStatus;
import com.netflix.spinnaker.orca.api.pipeline.models.PipelineExecution;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import com.netflix.spinnaker.orca.front50.Front50Service;
import com.netflix.spinnaker.orca.front50.DependentPipelineStarter;
import com.netflix.spinnaker.security.AuthenticatedRequest;
import groovy.lang.Closure;
import io.armory.plugin.smp.config.RunMultiplePipelinesConfig;
import io.armory.plugin.smp.config.RunMultiplePipelinesContext;
import javax.annotation.Nonnull;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import io.armory.plugin.smp.parseyml.App;
import io.armory.plugin.smp.parseyml.AppNames;
import io.armory.plugin.smp.parseyml.BundleWeb;
import lombok.SneakyThrows;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Extension
public class RunMultiplePipelinesTask implements Task {

    private final Front50Service front50Service;
    private final DependentPipelineStarter dependentPipelineStarter;
    private final RunMultiplePipelinesConfig config;

    public RunMultiplePipelinesTask(final RunMultiplePipelinesConfig config, Optional<Front50Service>front50Service,
                                    DependentPipelineStarter dependentPipelineStarter)  {
        this.config = config;
        this.front50Service = front50Service.orElse(null);
        this.dependentPipelineStarter = dependentPipelineStarter;
    }

    private final Logger logger = LoggerFactory.getLogger(RunMultiplePipelinesTask.class);

    @SneakyThrows
    @Nonnull
    @Override
    public TaskResult execute(@Nonnull StageExecution stage) {
        RunMultiplePipelinesContext context = stage.mapTo(RunMultiplePipelinesContext.class);

        Gson gson = new Gson();
        String json = gson.toJson(context.getYamlConfig().get(0));
        List<App> apps = getApps(json);

        String application = (String) (stage.getContext().get("pipelineApplication") != null ? stage.getContext().get("pipelineApplication") : stage.getExecution().getApplication());
        if (front50Service.equals(null)) {
            throw new UnsupportedOperationException("Cannot start a stored pipeline, front50 is not enabled. Fix this by setting front50.enabled: true");
        }

        List<Map<String, Object>> pipelines = front50Service.getPipelines(application, false);
        List<Map<String, Object>> pipelineConfigs = getPipelineConfigs(apps, pipelines);

        List<Map<String, Object>> parameters = getParameters(apps);

        for (int i = 0; i < pipelineConfigs.size(); i++) {
            String jsonString = gson.toJson(pipelineConfigs.get(i));
            Type type = new TypeToken<HashMap<String, Object>>(){}.getType();
            Map<String, Object> pipelineConfigCopy = gson.fromJson(jsonString, type);
            dependentPipelineStarter.trigger(
                    pipelineConfigCopy,
                    stage.getExecution().getAuthentication().getUser(),
                    stage.getExecution(),
                    parameters.get(i),
                    stage.getId(),
                    getUser(stage.getExecution())
            );
        }

        return TaskResult
                .builder(ExecutionStatus.SUCCEEDED)
                .outputs(Collections.singletonMap("my_message", "Hola"))
                .build();
    }

    private List<Map<String, Object>> getParameters(List<App> apps) {
        ArrayList<Map<String, Object>> parameters = new ArrayList<>();
        for (App app: apps) {
            Map<String, Object> params = new HashMap<>();
            params.put("app", app.getApp());
            params.put("targetEnv", app.getTargetEnv());
            params.put("tag", app.getTag());
            params.put("skipCanary", app.getSkipCanary());
            parameters.add(params);
        }
        return parameters;
    }

    private List<Map<String, Object>> getPipelineConfigs(List<App> apps, List<Map<String, Object>> pipelines) {
        List<Map<String, Object>> pipelineConfigs = new LinkedList<>();
        for (App app : apps) {
            pipelineConfigs.add( DefaultGroovyMethods.find(pipelines, new Closure<Boolean>(this ,this) {
                public Boolean doCall(Map<String, Object> it) {
                    return (it.get("name").equals(app.getChildPipeline()));
                }

                public Boolean doCall() {
                    return doCall(null);
                }
            }));
        }
        return pipelineConfigs;
    }

    private List<App> getApps(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        BundleWeb bundleWeb = mapper.readValue(json, BundleWeb.class);
        AppNames appNames = bundleWeb.getAppNames();
        List<Map<String, String>> appMaps = appNames.getApps();

        List<App> apps = new ArrayList<App>(appMaps.size());
        for (Map<String, String> item : appMaps) {
            apps.add(mapper.convertValue(item, App.class));
        }
        return apps;
    }

    private PipelineExecution.AuthenticationDetails getUser(PipelineExecution parentPipeline) {
        Optional<String> korkUsername = AuthenticatedRequest.getSpinnakerUser();
        if (korkUsername.isPresent()) {
            String korkAccounts = AuthenticatedRequest.getSpinnakerAccounts().orElse("");
            return new PipelineExecution.AuthenticationDetails(korkUsername.get(), korkAccounts.split(","));
        }

        if (parentPipeline.getAuthentication().getUser() != null) {
            return parentPipeline.getAuthentication();
        }

        return null;
    }
}
