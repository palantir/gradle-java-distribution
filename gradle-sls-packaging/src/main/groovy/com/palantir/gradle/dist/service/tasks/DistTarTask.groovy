/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.gradle.dist.service.tasks


import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Tar

class DistTarTask {


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

            from("${project.projectDir}/deployment") {
                into "${archiveRootDir}/deployment"
            }

            from("${project.projectDir}/service") {
                into "${archiveRootDir}/service"
                exclude("bin/*")
            }
            
            from("${project.projectDir}/service/bin") {
        		into("${archiveRootDir}/service/bin")
        		fileMode = 0755
        	}

            into("${archiveRootDir}/service/lib") {
                from(project.tasks.jar.outputs.files)
                from(project.configurations.runtimeClasspath)
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

            into("${archiveRootDir}/service/lib/linux-x86-64") {
                from("${project.buildDir}/libs/linux-x86-64")
                fileMode = 0755
            }

            into("${archiveRootDir}/deployment") {
                from("${project.buildDir}/deployment")
            }
        }
    }
}
