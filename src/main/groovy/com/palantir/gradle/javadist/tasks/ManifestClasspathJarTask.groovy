/*
 * Copyright 2016 Palantir Technologies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <http://www.apache.org/licenses/LICENSE-2.0>
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.gradle.javadist.tasks

import org.gradle.api.tasks.bundling.Jar

import com.palantir.gradle.javadist.DistributionExtension
import com.palantir.gradle.javadist.JavaDistributionPlugin

/**
 * Produces a JAR whose manifest's {@code Class-Path} entry lists exactly the JARs produced by the project's runtime
 * configuration.
 */
class ManifestClasspathJarTask extends Jar {

    public ManifestClasspathJarTask() {
        group = JavaDistributionPlugin.GROUP_NAME
        description = "Creates a jar containing a Class-Path manifest entry specifying the classpath using pathing " +
                "jar rather than command line argument to work around long path sizes."
        appendix = 'manifest-classpath'

        doFirst {
            manifest.attributes("Class-Path": project.files(project.configurations.runtime)
                    .collect { it.getName() }
                    .join(' ') + ' ' + project.tasks.jar.archiveName
            )
        }
        onlyIf { distributionExtension().isEnableManifestClasspath() }
    }

    DistributionExtension distributionExtension() {
        return project.extensions.findByType(DistributionExtension)
    }
}
