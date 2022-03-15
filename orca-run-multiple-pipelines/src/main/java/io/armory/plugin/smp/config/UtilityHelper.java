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

package io.armory.plugin.smp.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import groovy.lang.Closure;
import io.armory.plugin.smp.parseyml.App;
import io.armory.plugin.smp.parseyml.Apps;
import io.armory.plugin.smp.parseyml.BundleWeb;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class UtilityHelper {

    public List<String> getTriggerOrder(Apps apps,
                                        List<App> appOrder,
                                        Gson gson,
                                        ObjectMapper mapper) throws JsonProcessingException {
        List<String> triggerOrder = new LinkedList<>();

        for (Map.Entry<String, Object> entry : apps.getApps().entrySet()) {
            Map<String, Object> mapApp = Map.ofEntries(entry);
            String jsonApp = gson.toJson(mapApp.get(entry.getKey()));
            App app = mapper.readValue(jsonApp, App.class);
            appOrder.add(app);
            if (app.getDependsOn() != null) {
                app.getDependsOn().forEach(a -> {
                    if (!triggerOrder.contains(a)) {
                        triggerOrder.add(0, a);
                    }
                });
                if (!triggerOrder.contains(entry.getKey())) {
                    triggerOrder.add(entry.getKey());
                }
            }
        }

        for (Map.Entry<String, Object> entry : apps.getApps().entrySet()) {
            if (!triggerOrder.contains(entry.getKey())) {
                triggerOrder.add(entry.getKey());
            }
        }
        return triggerOrder;
    }

    public void sortAppOrderToTriggerOrder(List<App> appOrder, List<String> triggerOrder) {
        for (int i = 0; i < triggerOrder.size()-1; i++) {
            if (!triggerOrder.get(i).equals(appOrder.get(i).getArguments().get("app"))) {
                for (App s : appOrder) {
                    if (s.getArguments().get("app").equals(triggerOrder.get(i))) {
                        Collections.swap(appOrder, i, appOrder.indexOf(s));
                    }
                }
            }
        }
    }

    public List<Map<String, Object>> getPipelineConfigs(List<App> apps, List<Map<String, Object>> pipelines) {
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

    public Apps getApps(RunMultiplePipelinesContext context, Gson gson, ObjectMapper mapper) throws JsonProcessingException {
        String json = gson.toJson(context.getYamlConfig().get(0));
        BundleWeb bundleWeb = mapper.readValue(json, BundleWeb.class);

        String jsonApps = gson.toJson(bundleWeb.getBundleWeb());
        Apps apps = mapper.readValue(jsonApps, Apps.class);
        return apps;
    }



}
