package com.netflix.gradle.plugins.utils

import org.codehaus.groovy.runtime.GeneratedClosure
import org.gradle.internal.metaobject.ConfigureDelegate
import org.gradle.util.Configurable
import org.gradle.util.internal.ClosureBackedAction

import javax.annotation.Nullable

class ConfigureUtil {
    static <T> T configure(@Nullable Closure configureClosure, T target) {
        if (configureClosure == null) {
            return target;
        } else {
            if (target instanceof Configurable) {
                ((Configurable)target).configure(configureClosure);
            } else {
                configureTarget(configureClosure, target, new ConfigureDelegate(configureClosure, target));
            }

            return target
        }
    }

    private static <T> void configureTarget(Closure configureClosure, T target, ConfigureDelegate closureDelegate) {
        if (!(configureClosure instanceof GeneratedClosure)) {
            (new ClosureBackedAction(configureClosure, 1, false)).execute(target);
        } else {
            Closure withNewOwner = configureClosure.rehydrate(target, closureDelegate, configureClosure.getThisObject());
            (new ClosureBackedAction(withNewOwner, 2, false)).execute(target);
        }
    }
}
