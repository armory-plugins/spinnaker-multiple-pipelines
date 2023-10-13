package io.armory.plugin.smp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class RunMultiplePipelinesPluginTest {

    @Test
    public void checkIfPluginStartsSuccessfully() {
        var logger = mock(Logger.class);

        try (var loggerFactory = Mockito.mockStatic(LoggerFactory.class)) {
            loggerFactory.when(() -> LoggerFactory.getLogger(anyString())).thenReturn(logger);
            loggerFactory.when(() -> LoggerFactory.getLogger(any(Class.class))).thenReturn(logger);

            var pluginWrapper = mock(PluginWrapper.class);
            var plugin = new RunMultiplePipelinesPlugin(pluginWrapper);

            plugin.start();

            verify(logger).info("Starting RunMultiplePipelines plugin...");
        }
    }

    @Test
    public void checkIfPluginStopsSuccessfully() {
        var logger = mock(Logger.class);

        try (var loggerFactory = Mockito.mockStatic(LoggerFactory.class)) {
            loggerFactory.when(() -> LoggerFactory.getLogger(anyString())).thenReturn(logger);
            loggerFactory.when(() -> LoggerFactory.getLogger(any(Class.class))).thenReturn(logger);

            var pluginWrapper = mock(PluginWrapper.class);
            var plugin = new RunMultiplePipelinesPlugin(pluginWrapper);

            plugin.stop();

            verify(logger).info("Stopping RunMultiplePipelines plugin...");
        }
    }

    @Test
    public void checkIfPluginPackagesToScanAreCorrect() {
        var pluginWrapper = mock(PluginWrapper.class);
        var plugin = new RunMultiplePipelinesPlugin(pluginWrapper);

        var packages = plugin.getPackagesToScan();

        assertEquals(1, packages.size());
        assertEquals("io.armory.plugin.smp", packages.get(0));
    }
}
