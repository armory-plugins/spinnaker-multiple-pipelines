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

