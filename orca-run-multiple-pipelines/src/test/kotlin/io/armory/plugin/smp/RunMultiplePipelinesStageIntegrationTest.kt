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

import com.netflix.spinnaker.orca.api.test.orcaFixture
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import strikt.api.expect
import strikt.assertions.isEqualTo

/**
 * This test demonstrates that the RunMultiplePipelines can be loaded by Orca
 * and that RunMultiplePipeline's StageDefinitionBuilder can be retrieved at runtime.
 */
class RunMultiplePipelinesStageIntegrationTest : JUnit5Minutests{

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
        }
    }

}