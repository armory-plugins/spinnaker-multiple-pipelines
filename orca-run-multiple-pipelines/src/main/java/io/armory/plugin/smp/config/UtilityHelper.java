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
import io.armory.plugin.smp.parseyml.App;
import io.armory.plugin.smp.parseyml.Apps;
import io.armory.plugin.smp.parseyml.BundleWeb;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class UtilityHelper {

    public Apps getApps(RunMultiplePipelinesContext context, Gson gson, ObjectMapper mapper) throws JsonProcessingException {
        String json = gson.toJson(context.getYamlConfig().get(0));
        BundleWeb bundleWeb = mapper.readValue(json, BundleWeb.class);

        String jsonApps = gson.toJson(bundleWeb.getBundleWeb());
        Apps apps = mapper.readValue(jsonApps, Apps.class);
        return apps;
    }

    public Map<String, Stack<App>> tryWithStack(Apps apps, ObjectMapper mapper, Gson gson) throws JsonProcessingException {
        Map<String, Stack<App>> result = new HashMap<>();

        //getAll appsNames
        List<String> topApps = new LinkedList<>();
        for (Map.Entry<String, Object> entry : apps.getApps().entrySet()) {
            topApps.add(entry.getKey());
        }
        topApps.forEach(System.out::println);

        //SortThem only keep the ones that don't depend on
        for (Map.Entry<String, Object> entry : apps.getApps().entrySet()) {
            Map<String, Object> mapApp = Map.ofEntries(entry);
            String jsonApp = gson.toJson(mapApp.get(entry.getKey()));
            App app = mapper.readValue(jsonApp, App.class);
            if (app.getDependsOn() != null) {
                app.getDependsOn().forEach(appName -> topApps.removeIf(appName::equals));
            }
        }

        //Push the "TopApps" that don't depend on anything to the stack
        topApps.forEach(appName -> result.put(appName, new Stack<>()));
        for (Map.Entry<String, Object> entry : apps.getApps().entrySet()) {
            Map<String, Object> mapApp = Map.ofEntries(entry);
            String jsonApp = gson.toJson(mapApp.get(entry.getKey()));
            App app = mapper.readValue(jsonApp, App.class);
            topApps.forEach(appName -> {
                if (appName.equals(entry.getKey())) {
                    result.get(entry.getKey()).push(app);
                }
            });
        }

        //iterate over Top apps to find the tree of dependencies
        //ex web -> pulse -> otherApp then pop form the stack to trigger in the right order
        for (Map.Entry<String, Stack<App>> stackEntry : result.entrySet()) {
            int i = 0;
            while (stackEntry.getValue().size() > i) {
                App stack = stackEntry.getValue().get(i);
                if(stack.getDependsOn() != null) {
                    for (String appName : stack.getDependsOn()) {
                        Map.Entry<String, Object> entry = apps.getApps().entrySet().stream().filter(e -> {
                            Map<String, Object> mapApp = Map.ofEntries(e);
                            String jsonApp = gson.toJson(mapApp.get(e.getKey()));
                            boolean filter = false;
                            try {
                                App app = mapper.readValue(jsonApp, App.class);
                                filter = appName.equals(app.getArguments().get("app"));
                            } catch (JsonProcessingException ex) {
                                ex.printStackTrace();
                            }
                            return filter;
                        }).findAny().get();
                        Map<String, Object> mapApp = Map.ofEntries(entry);
                        String jsonApp = gson.toJson(mapApp.get(entry.getKey()));
                        try {
                            App app = mapper.readValue(jsonApp, App.class);
                            stackEntry.getValue().push(app);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    }
                }
                i++;
            }
        }

        return result;
    }


}
