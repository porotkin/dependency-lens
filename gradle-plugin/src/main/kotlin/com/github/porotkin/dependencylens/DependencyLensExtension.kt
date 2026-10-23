package com.github.porotkin.dependencylens

import org.gradle.api.provider.SetProperty

abstract class DependencyLensExtension {
    abstract val ignored: SetProperty<String>
}
