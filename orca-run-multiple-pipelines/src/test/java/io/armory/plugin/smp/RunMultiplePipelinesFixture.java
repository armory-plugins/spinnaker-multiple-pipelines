package io.armory.plugin.smp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.kork.plugins.internal.PluginJar;
import com.netflix.spinnaker.orca.StageResolver;
import com.netflix.spinnaker.orca.api.test.OrcaFixture;
import com.netflix.spinnaker.orca.front50.DependentPipelineStarter;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;

@ContextConfiguration(classes = DependentPipelineStarter.class)
@TestPropertySource(properties = {
        "spinnaker.extensibility.plugins.Armory.RunMultiplePipelines.enabled=true",
        "spinnaker.extensibility.plugins-root-path=build/tmp/plugins"
})
@AutoConfigureMockMvc
public class RunMultiplePipelinesFixture extends OrcaFixture {

    @Autowired
    StageResolver stageResolver;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper mapper;

    public RunMultiplePipelinesFixture() {
        String pluginId = "Armory.RunMultiplePipelines";
        File plugins = new File("build/tmp/plugins");
        FileUtils.deleteQuietly(plugins);
        plugins.mkdir();

        new PluginJar.Builder(plugins.toPath()
                .resolve(pluginId+".jar"), pluginId)
                .pluginClass(RunMultiplePipelinesPlugin.class.getName())
                .pluginVersion("1.0.0")
                .build();
    }
}
