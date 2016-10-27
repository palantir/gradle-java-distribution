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

import java.nio.file.Files

import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.palantir.gradle.javadist.JavaDistributionPlugin

import groovy.transform.EqualsAndHashCode

class LaunchConfigTask extends BaseTask {

    LaunchConfigTask() {
        group = JavaDistributionPlugin.GROUP_NAME
        description = "Generates launcher-static.yml and launcher-check.yml configurations."
    }

    @EqualsAndHashCode
    public static class StaticLaunchConfig {
        // keep in sync with StaticLaunchConfig struct in go-java-launcher
        String configType = "java"
        int configVersion = 1
        String mainClass
        String javaHome
        List<String> classpath
        List<String> jvmOpts
        List<String> args
    }

    @Input
    Iterable<String> getArgs() {
        return distributionExtension().args
    }

    @Input
    Iterable<String> getCheckArgs() {
        return distributionExtension().checkArgs
    }

    @OutputFile
    public File getStaticLauncher() {
        return new File("scripts/launcher-static.yml")
    }

    @OutputFile
    public File getCheckLauncher() {
        return new File("scripts/launcher-check.yml")
    }

    @TaskAction
    void createConfig() {
        writeConfig(createConfig(getArgs()), getStaticLauncher())
        writeConfig(createConfig(getCheckArgs()), getCheckLauncher())
    }

    void writeConfig(StaticLaunchConfig config, File scriptFile) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
        def outfile = project.buildDir.toPath().resolve(scriptFile.toPath())
        Files.createDirectories(outfile.parent)
        outfile.withWriter { it ->
            mapper.writeValue(it, config)
        }
    }

    StaticLaunchConfig createConfig(List<String> args) {
        StaticLaunchConfig config = new StaticLaunchConfig()
        config.mainClass = distributionExtension().mainClass
        config.javaHome = distributionExtension().javaHome ?: ""
        config.args = args
        if (distributionExtension().isEnableManifestClasspath()) {
            config.classpath = relativizeToServiceLibDirectory(
                    project.tasks.getByName('manifestClasspathJar').outputs.files)
        } else {
            config.classpath = relativizeToServiceLibDirectory(
                    project.tasks[JavaPlugin.JAR_TASK_NAME].outputs.files + project.configurations.runtime)
        }
        config.jvmOpts = distributionExtension().defaultJvmOpts
        return config
    }

    private static List<String> relativizeToServiceLibDirectory(FileCollection files) {
        def output = []
        files.each { output.add("service/lib/" + it.name) }
        return output
    }
}
