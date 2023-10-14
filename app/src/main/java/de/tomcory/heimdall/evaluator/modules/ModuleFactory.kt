package de.tomcory.heimdall.evaluator.modules

object ModuleFactory {

    val registeredModules = mutableListOf<Module>()
    init {
        this.registeredModules.add(StaticPermissionsScore())
        this.registeredModules.add(TrackerScore())
        /* add new modules here:
        this.registeredModules.add(newModule())
         */
    }
}
