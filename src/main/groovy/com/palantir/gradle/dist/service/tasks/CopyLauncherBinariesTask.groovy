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

import com.palantir.gradle.dist.service.JavaDistributionPlugin
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.RelativePath
import org.gradle.api.DefaultTask

class CopyLauncherBinariesTask extends DefaultTask {
    CopyLauncherBinariesTask() {
        group = JavaDistributionPlugin.GROUP_NAME
        description = "Creates go-java-launcher binaries."
        doLast{
            project.copy {
                def zipPath = project.configurations.goJavaLauncherBinaries.find {
                    it.name.startsWith("go-java-launcher")
                }
                def zipFile = project.file(zipPath)

                from project.tarTree(zipFile)
                into "${project.buildDir}/scripts"
                includeEmptyDirs = false

                // remove first three levels of directory structure from Tar container
                eachFile { FileCopyDetails fcp ->
                    fcp.relativePath = new RelativePath(
                            !fcp.file.isDirectory(),
                            fcp.relativePath.segments[3..-1] as String[])
                }
            }
        }
    }
}
