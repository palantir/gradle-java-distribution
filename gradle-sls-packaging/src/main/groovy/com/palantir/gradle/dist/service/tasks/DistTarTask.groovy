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
package com.palantir.gradle.dist.service.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar

import com.palantir.gradle.dist.service.JavaServiceDistributionPlugin

class DistTarTask extends DefaultTask {

    static Tar createDistTarTask(Project project, String taskName) {
        project.tasks.create(taskName, Tar) { p ->
            p.group = JavaServiceDistributionPlugin.GROUP_NAME
            p.description = "Creates a compressed, gzipped tar file that contains required runtime resources."
            // Set compression in constructor so that task output has the right name from the start.
            p.compression = Compression.GZIP
            p.extension = 'sls.tgz'
        }
    }

    static void configure(Tar distTar, Project project, String serviceName, List<String> excludeFromVar, boolean isEnableManifestClasspath) {
        distTar.configure {
            setBaseName(serviceName)
            String archiveRootDir = serviceName + '-' + String.valueOf(project.version)

            from("${project.projectDir}/var") {
                into "${archiveRootDir}/var"

                excludeFromVar.each {
                    exclude it
                }
            }

            new File(project.buildDir, "gjd-tmp/var/data/tmp").mkdirs()
            from ("${project.buildDir}/gjd-tmp/var/data") {
                into "${archiveRootDir}/var/data"
            }

            from("${project.projectDir}/deployment") {
                into "${archiveRootDir}/deployment"
            }

            from("${project.projectDir}/service") {
                into "${archiveRootDir}/service"
            }

            into("${archiveRootDir}/service/lib") {
                from(project.tasks.jar.outputs.files)
                from(project.configurations.runtime)
            }

            if (isEnableManifestClasspath) {
                into("${archiveRootDir}/service/lib") {
                    from(project.tasks.getByName("manifestClasspathJar"))
                }
            }

            into("${archiveRootDir}/service/bin") {
                from("${project.buildDir}/scripts")
                fileMode = 0755
            }

            into("${archiveRootDir}/service/monitoring/bin") {
                from("${project.buildDir}/monitoring")
                fileMode = 0755
            }

            into("${archiveRootDir}/deployment") {
                from("${project.buildDir}/deployment")
            }
        }
    }
}
