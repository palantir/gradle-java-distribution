/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.gradle.dist

import nebula.test.IntegrationTestKitSpec
import nebula.test.multiproject.MultiProjectIntegrationHelper
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

class GradleIntegrationSpec extends IntegrationTestKitSpec {
    protected MultiProjectIntegrationHelper helper
    protected String gradleVersion

    def setup() {
        keepFiles = true
        System.setProperty("ignoreDeprecations", "true")
        settingsFile.createNewFile()
        helper = new MultiProjectIntegrationHelper(getProjectDir(), settingsFile)
    }

    protected boolean fileExists(String path) {
        new File(projectDir, path).exists()
    }

    BuildResult runTasks(String... tasks) {
        BuildResult result = with(tasks).build()
        return checkForDeprecations(result)
    }

    BuildResult runTasksAndFail(String... tasks) {
        BuildResult result = with(tasks).buildAndFail()
        return checkForDeprecations(result)
    }

    private GradleRunner with(String... tasks) {
        def runner = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments(calculateArguments(tasks))
                .withDebug(debug)
                .withPluginClasspath()
                .forwardOutput()
        if (gradleVersion != null) {
            runner.withGradleVersion(gradleVersion)
        }
        runner
    }

}
