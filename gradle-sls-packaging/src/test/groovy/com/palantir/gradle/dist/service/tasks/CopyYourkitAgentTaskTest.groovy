/*
 * (c) Copyright 2018 Palantir Technologies Inc. All rights reserved.
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

import com.palantir.gradle.dist.GradleTestSpec
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

class CopyYourkitAgentTaskTest extends GradleTestSpec {

    def 'copyYourkitAgent task is up to date if already run'() {
        setup:
        createUntarBuildFile(buildFile)

        when:
        BuildResult buildResult = run(':copyYourkitAgent').build()

        then:
        buildResult.task(':copyYourkitAgent').outcome == TaskOutcome.SUCCESS

        when:
        buildResult = run(':copyYourkitAgent').build()

        then:
        buildResult.task(':copyYourkitAgent').outcome == TaskOutcome.UP_TO_DATE
    }

    def 'Build produces libyjpagent file and yourkit license'() {
        given:
        createUntarBuildFile(buildFile)

        when:
        runSuccessfully(':build', ':distTar', ':untar')

        then:
        file('dist/service-name-0.0.1').exists()
        file('dist/service-name-0.0.1/service/lib/linux-x86-64/libyjpagent.so').exists()
        file('dist/service-name-0.0.1/service/lib/linux-x86-64/libyjpagent.so').getBytes().length > 0
        file('dist/service-name-0.0.1/service/lib/linux-x86-64/yourkit-license-redist.txt').exists()
        file('dist/service-name-0.0.1/service/lib/linux-x86-64/yourkit-license-redist.txt').getBytes().length > 0
    }

    protected runSuccessfully(String... tasks) {
        BuildResult buildResult = run(tasks).build()
        tasks.each { buildResult.task(it).outcome == TaskOutcome.SUCCESS }
        return buildResult
    }

    private static createUntarBuildFile(buildFile) {
        buildFile << '''
            plugins {
                id 'com.palantir.sls-java-service-distribution'
                id 'java'
            }

            project.group = 'service-group'

            repositories {
                jcenter()
                maven { url "http://palantir.bintray.com/releases" }
            }

            version '0.0.1'

            distribution {
                serviceName 'service-name'
                mainClass 'test.Test'
            }

            sourceCompatibility = '1.7'

            // most convenient way to untar the dist is to use gradle
            task untar (type: Copy) {
                from tarTree(resources.gzip("${buildDir}/distributions/service-name-0.0.1.sls.tgz"))
                into "${projectDir}/dist"
                dependsOn distTar
            }
        '''.stripIndent()
    }
}
