package de.tomcory.heimdall.evaluator.modules

object ModuleFactory {

    val registeredModules = mutableListOf<Module>()
    init {
        this.registeredModules.add(StaticPermissionsScore())
        this.registeredModules.add(TrackerScore())

        this.registeredModules.add(PrivacyPolicyScore())

    }
}
