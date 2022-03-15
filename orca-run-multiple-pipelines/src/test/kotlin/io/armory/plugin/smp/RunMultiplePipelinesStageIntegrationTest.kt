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

import com.google.gson.Gson
import com.netflix.spinnaker.orca.api.test.orcaFixture
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import io.armory.plugin.smp.config.RunMultiplePipelinesContext
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import strikt.api.expect
import strikt.assertions.isEqualTo
import java.io.File

/**
 * This test demonstrates that the RunMultiplePipelines can be loaded by Orca
 * and that RunMultiplePipeline's StageDefinitionBuilder can be retrieved at runtime.
 */
class RunMultiplePipelinesStageIntegrationTest : JUnit5Minutests{

    val jsonString: String = File("./src/test/resources/yamlConfig.json").readText(Charsets.UTF_8)
    val gson = Gson()
    val json = gson.toJson(jsonString)

    fun tests() = rootContext<OrcaPluginsFixture> {
        context("a running Orca instance") {
            orcaFixture {
                OrcaPluginsFixture()
            }

            test("RunMultiplePipelinesStage extension is resolved to the correct type") {
                val stageDefinitionBuilder = stageResolver.getStageDefinitionBuilder(
                    RunMultiplePipelinesStage::class.java.simpleName, "runMultiplePipelines")

                expect {
                    that(stageDefinitionBuilder.type).isEqualTo("runMultiplePipelines")
                }
            }

            test("RunMultiplePipelines can be executed as a stage within a live pipeline execution") {
                val response = mockMvc.post("/orchestrate/{pipelineId}","123") {
                    contentType = MediaType.APPLICATION_JSON
                    content = mapper.writeValueAsString(mapOf(
                        "application" to "pf4j-stage-plugin",
                        "stages" to listOf(mapOf(
                            "refId" to "1",
                            "type" to "runMultiplePipelines",
                            "yamlConfig" to json
                        )
                    )))
                }.andReturn().response

                expect {
                    that(response.status).isEqualTo(200)
                }

            }
        }
    }

    data class Stage(val status: String, val context: RunMultiplePipelinesContext, val type: String)
}