package io.armory.plugin.smp;

import com.netflix.spinnaker.kork.plugins.internal.PluginJar;
import com.netflix.spinnaker.orca.StageResolver;
import com.netflix.spinnaker.orca.api.test.OrcaFixture;
import com.netflix.spinnaker.orca.front50.DependentPipelineStarter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

@ContextConfiguration(classes = DependentPipelineStarter.class)
@TestPropertySource(properties = {
  "spinnaker.extensibility.plugins.Armory.RunMultiplePipelines.enabled=true",
  "spinnaker.extensibility.plugins-root-path=build/plugins"
})
@AutoConfigureMockMvc
public class RunMultiplePipelinesFixture extends OrcaFixture {

    @Autowired
    StageResolver stageResolver;

    private static File pluginsFolder;

    @BeforeAll
    public static void setup() {
        String pluginId = "Armory.RunMultiplePipelines";

        pluginsFolder = new File("build/plugins");
        pluginsFolder.mkdirs();

        new PluginJar.Builder(pluginsFolder.toPath().resolve(pluginId + ".jar"), pluginId)
                .pluginClass(RunMultiplePipelinesPlugin.class.getName())
                .pluginVersion("1.0.0")
                .build();
    }

    @AfterAll
    public static void cleanUp() throws IOException {
        Files.walk(pluginsFolder.toPath())
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
}
