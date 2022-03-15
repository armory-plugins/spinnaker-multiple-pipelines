/*
 * Copyright 2020 Armory, Inc.
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

package io.armory.plugin.smp

import com.netflix.spinnaker.kork.plugins.internal.PluginJar
import com.netflix.spinnaker.orca.StageResolver
import com.netflix.spinnaker.orca.api.test.OrcaFixture
import com.netflix.spinnaker.orca.front50.DependentPipelineStarter
import io.armory.plugin.smp.tasks.RunMultiplePipelinesTask
import java.io.File
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource

@ContextConfiguration(classes = [DependentPipelineStarter::class])
@TestPropertySource(properties = [
    "spinnaker.extensibility.plugins.Armory.RunMultiplePipelinesPlugin.enabled=true",
    "spinnaker.extensibility.plugins-root-path=build/plugins"
])
@AutoConfigureMockMvc
class OrcaPluginsFixture : OrcaFixture() {

    @Autowired
    lateinit var stageResolver: StageResolver

    init {
        val pluginId = "Armory.RunMultiplePipelinesPlugin"
        val plugins = File("build/plugins").also {
            it.delete()
            it.mkdir()
        }

        PluginJar.Builder(plugins.toPath().resolve("$pluginId.jar"), pluginId)
            .pluginClass(RunMultiplePipelinesPlugin::class.java.name)
            .pluginVersion("0.0.1")
            .extensions(mutableListOf(RunMultiplePipelinesStage::class.java.name, RunMultiplePipelinesTask::class.java.name))
            .build()
    }
}

