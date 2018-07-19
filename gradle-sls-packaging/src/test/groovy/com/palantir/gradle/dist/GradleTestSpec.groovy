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

package com.palantir.gradle.dist

import com.google.common.collect.ImmutableList
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import nebula.test.multiproject.MultiProjectIntegrationHelper
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class GradleTestSpec extends Specification {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    File projectDir
    File buildFile
    File settingsFile
    MultiProjectIntegrationHelper helper

    def setup() {
        projectDir = folder.newFolder()
        File propertiesFile = file("gradle.properties")
        propertiesFile << "org.gradle.daemon=false"
        buildFile = file("build.gradle")
        settingsFile = new File(projectDir, 'settings.gradle')
        helper = new MultiProjectIntegrationHelper(projectDir, settingsFile)
        settingsFile << "rootProject.name = 'root-project'${System.getProperty('line.separator')}"
        println("Build directory: \n" + projectDir.absolutePath)
    }

    protected run(String... tasks) {
        return GradleRunner.create()
                .forwardOutput()
                .withProjectDir(projectDir)
                .withArguments(ImmutableList.<String> builder()
                    .addAll(Arrays.asList(tasks))
                    .add("--stacktrace")
                    .build())
                .withPluginClasspath()
                .withDebug(true)
    }

    protected runSuccessfully(String... tasks) {
        BuildResult buildResult = run(tasks).build()
        tasks.each { buildResult.task(it).outcome == TaskOutcome.SUCCESS }
        return buildResult
    }

    protected String execWithOutput(String... tasks) {
        StringBuffer sout = new StringBuffer(), serr = new StringBuffer()
        Process proc = new ProcessBuilder().command(tasks).directory(projectDir).start()
        proc.consumeProcessOutput(sout, serr)
        int result = proc.waitFor()
        int expected = 0
        Assert.assertEquals(sprintf("Expected command '%s' to exit with '%d'", tasks.join(' '), expected), expected, result)
        return sout.toString()
    }

    protected int execWithExitCode(String... tasks) {
        Process proc = new ProcessBuilder().command(tasks).directory(projectDir).start()
        int result = proc.waitFor()
        return result
    }

    protected String execAllowFail(String... tasks) {
        new ProcessBuilder().command(tasks).directory(projectDir)
                .start()
                .waitFor()
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected File createFile(String path, File baseDir = projectDir) {
        File file = file(path, baseDir)
        assert !file.exists()
        directory(file.parent, baseDir)
        assert file.createNewFile()
        return file
    }

    protected File file(String path, File baseDir = projectDir) {
        def splitted = path.split('/')
        def directory = splitted.size() > 1 ? directory(splitted[0..-2].join('/'), baseDir) : baseDir
        def file = new File(directory, splitted[-1])
        return file
    }

    protected File directory(String path, File baseDir = projectDir) {
        return new File(baseDir, path).with {
            mkdirs()
            return it
        }
    }
}
