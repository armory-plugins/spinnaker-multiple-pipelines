RunMultiplePipelines plugin adds a new custom stage to Spinnaker to trigger multiple child pipelines based on a yaml config.

### Version Compatibility
Minimum tested version is 2.26.x

| Plugin  | Spinnaker Platform |
|:------------- | :--------- |
| 1.0.2 <= |  2.27.x |
| 1.0.3 <= |  2.27.x |
| 1.0.4 <= |  2.27.x |
| 1.0.5 <= |  2.27.x |
| 1.0.6 <= |  2.27.x |
| 1.0.7 <= |  2.27.x |
| 1.0.8 <= |  2.27.x |

NOTE: The plugin is not actively tested in all compatible versions with all variants, but is expected to work in the above.

Check [release notes](https://github.com/armory/spinnaker-multiple-pipelines#release-notes) for more info

# Installation & Configuration
Note: alternative you can consume the plugin using this example patch as well. [example](https://github.com/armory/multiple-pipelines-plugin-releases/blob/main/plugins/custom/patch-plugin-multiple-pipelines-2.yml)
1. Identify the released version of the RunMultiplePipelines plugin you wish to install. Official releases are found [here](https://github.com/armory/multiple-pipelines-plugin-releases).
2. In your Spinnaker configuration, add the following repository & plugin configuration. [example](https://github.com/armory/multiple-pipelines-plugin-releases/blob/main/plugins/custom/patch-plugin-multiple-pipelines.yml)
```yaml
spinnakerConfig:
  # spec.spinnakerConfig.profiles - This section contains the YAML of each service's profile
  profiles:
    gate:
      spinnaker:
        extensibility:
          deck-proxy: # you need this for plugins with a Deck component
            enabled: true
            plugins:
              Armory.RunMultiplePipelines:
                enabled: true
                version: 1.0.8
          repositories:
            runMultiplePipelinesRepo:
              url: https://raw.githubusercontent.com/armory/multiple-pipelines-plugin-releases/main/plugins.json
    orca:
      spinnaker:
        extensibility:
          plugins:
            Armory.RunMultiplePipelines:
            enabled: true
            version: 1.0.8
            extensions:
              armory.runMultiplePipelinesStage:
                enabled: true
          repositories:
            runMultiplePipelinesRepo:
            url: https://raw.githubusercontent.com/armory/multiple-pipelines-plugin-releases/main/plugins.json
```
3. On startup, the plugin will be downloaded and installed to the appropriate Spinnaker services.

- if you don't specify a version it will take the newest.

- after redeploying spinnaker this will load and start the plugin during app startup for gate and orca.

The custom stage consumes a yaml under yaml-config with the next format:
```yaml
bundle_web: 
  app_name: 
    arguments: List #Optional
      - object
      - object
      - object
      - object
    child_pipelines: string #Required
    depends_on: List #Optional
      - string
      - string
```
Example:
```yaml
bundle_web:
  appName1:
    arguments:
      app: app1
      deploymentFrezeOverride: true
      skipCanary: true
      tag: 1.1.0
      targetEnv: targetEnv
    child_pipeline: childPipeline
  appName2:
    arguments:
      app: app2
      deploymentFrezeOverride: true
      skipCanary: true
      tag: 1.1.0
      targetEnv: targetEnv
    child_pipeline: childPipeline
    depends_on:
      - appName1

```

## yaml config specifics
- The bundle name needs to be bundle_web
- The child_pipeline name needs to exist on the same spinnaker application
- The Deploy (manifest) stage you wish to rollback in your child_pipeline has to have the prefix "Deploy"
- The plugin will look a created artifact name that includes the app argument
- For rollback_onfailure or manual rollbacks to work you need to create a pipeline with the name rollbackOnFailure in the same application

### release notes
- Version 1.0.2 Only supports concurrent executions using Redis
- Version 1.0.3 Supports concurrent executions using SQL database
- Version 1.0.4:
    - Breaking change don't use depends_on property 
    - The plugin used to work in one task and had a loop to control the order of executions but this had a bug where the "main" task on previous versions restarted itself after 10 min
    - Now the plugin stage uses three tasks - the last task monitor child executions
- Version 1.0.5: Fixes bug where failed evaluate expressions warnings were shown in the UI
- Version 1.0.6: 
    - Prevent manual executions of rollbackOnFailure pipeline used by the plugin
    - Filter the deploy manifest stages of child_pipeline to only retrieve on outputs supported types for rollout (Deployment/DaemonSet/StatefulSet)
- Version 1.0.7:
    - Removes restriction where object name needed a match for arguments.app -> check old readme [point two "All apps need an argument app..."](https://github.com/armory/spinnaker-multiple-pipelines/tree/ba2b3155851f05608d899864bc37064730a5dbe5#yaml-config-specifics)
    - Added a stage property "checkDuplicated" for validation when reading the yaml-config to check if you have duplicated objects(same arguments values and same child_pipeline) 
    - checkDuplicated is a hidden property by default set as true, setting it as false will mean you are allowing duplicated executions
- Version 1.0.8:
  - Fixed depends_on property to control order of child executions
  - Reduces size of outputs and context for the Stage and child-trigger-ParentPipeline

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

# UI Plugin Development

1) Run `yarn && yarn build` inside the plugin project <deck-run-multiple-pipelines>.
2) Clone the spinnaker deck repo `git clone git@github.com:spinnaker/deck.git` and create a directory plugins inside the project.
3) Make a symbolic link with the index.js file generated to the deck repo `sudo ln -sf ~/<git-clone-directory>/spinnaker-multiple-pipelines/deck-run-multiple-pipelines/build/dist/index.js ~/<git-clone-directory>/deck/plugins/index.js`.
4) On deck project edit ~/<git-clone-directory>/deck/packages/app/public/plugin-manifest.json file to:
```
[
    {
        "id": "Armory.RunMultiplePipelinesPlugin",
        "url": "./plugins/index.js",
        "version": "1.0.0"
    }
]
```
5) Run the following command on deck project:
- `yarn`
- `yarn modules`
- `yarn start`

6) Deck Project is running at http://localhost:9000/
7) And finally with Port forward to access gate you should see a new stage called Run Multiple Pipelines.

