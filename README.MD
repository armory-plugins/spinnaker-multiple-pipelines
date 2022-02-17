# Spinnaker Multiple Pipelines plugin development

## Debug a Spinnaker cluster service using tellepresence
* [How to install telepresence and debug spinnaker](https://youtu.be/Lc8i4PhrIXM)


# Debugging the plugin

To debug the `orca-run-multiple-pipelines`  server component inside a Spinnaker service (like Orca) using IntelliJ Idea follow these steps:

1) Run `./gradlew releaseBundle` in the plugin project.
2) Make a symbolic link with the followin command `sudo ln -sf ~/<git-clone-direcory>/spinnaker-multiple-pipelines/orca-run-multiple-pipelines/build/Armory.RunMultitplePipelines-orca.plugin-ref /opt/orca/plugins/Armory.RunMultitplePipelines-orca.plugin-ref`
3) Create the /opt/orca/plugins directories if you don't have them
3) Link the plugin project to the service project in IntelliJ (from the service project use the `+` button in the Gradle tab and select the plugin build.gradle).
4) Add to the Spinnaker ~/.spinnaker/orca-local.yml the followin lines.
```
spinnaker:
  extensibility:
    plugins:
      Armory.RunMultitplePipelines:
       enabled: true
```
5) Create a new IntelliJ run configuration for the service that has the VM option `-Dpf4j.mode=development` and does a `Build Project` before launch.
6) Debug away...