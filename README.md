# PlayerBot

A server-side plugin that aims to simulate player basebuilding for no reason.
Eventual goal is to have an ai sophisticated enough to engage in basic pvp and survival by itself.

Crafty server owners may use this to boost server player count.

Note that this is a *heavy* plugin, resource use is high. Lag spikes are common on world load. 

This plugin is also very UNFINISHED.

**Current progress:**
- Server thinks its a real player [x]
- Moves around, mines, builds, and can say words like a player [x]
- Bots intelligently avoids static threats and pathfinds through obstacles [x]
- Bots can intelligently pathfind conveyors through the base. [-]
    - bridges and junctions appropriately [x]
    - takes care to not pollute adjacent blocks [x]
    - takes into consideration what should be on the conveyor [x]
    - routes conveyors around static threats (like enemy turret ranges) [x]
    - takes into consideration anti-build areas [-]
    - can pathfind armoured conveyors effectively [-]
    - can pathfind plastanium conveyors effectively [-]
- AI can analyse buildings placed to determine its purpose [-]
    - Can analyse item conveyance structures and determine [-]
        - its throughput [~] (not for plastanium conveyors, which are a little tricky)
        - the items transported, based on the structures attached [-]
        - when its throughput is too limited for the structures attached [-]
        - how to upgrade them if necessary [-]
    - Can analyse production structures and determine [-]
        - What and how much it produces [-]
        - What and how much it uses [-]
        - The location of its outputs [-]
        - The location of its inputs [-]
- AI can design drill placements and connect them with conveyors.[-]
    - Search space is very large, whether this succeeds will determine if ai will also deisgn its own schematics.
    - Conveyors only.[-]
    - Bridges included.[-]
- AI can place production blocks and connect them with conveyors.[-]
- AI can follow the super-early game tech tree with no threats (copper,lead, graphite, no power). [-]

...

### Setup

Clone this repository first.
To edit the plugin display name and other data, take a look at `plugin.json`.
Edit the name of the project itself by going into `settings.gradle`.

### Basic Usage

See `src/example/ExamplePlugin.java` for some basic commands and event handlers.  
Every main plugin class must extend `Plugin`. Make sure that `plugin.json` points to the correct main plugin class.

Please note that the plugin system is in beta, and as such is subject to changes.

### Building a Jar

`gradlew jar` / `./gradlew jar`

Output jar should be in `build/libs`.


### Installing

Simply place the output jar from the step above in your server's `config/mods` directory and restart the server.
List your currently installed plugins/mods by running the `mods` command.
