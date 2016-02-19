/*
 * Copyright 2015 Palantir Technologies
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
package com.palantir.gradle.javadist

import org.gradle.api.Project
import org.gradle.api.file.FileCollection

class DistributionExtension {

    private String serviceName
    private String mainClass
    private List<String> args = []
    private List<String> defaultJvmOpts = []
    private boolean enableManifestClasspath = false
    private String javaHome = null
    private FileCollection classpath

    DistributionExtension(Project project) {
        classpath = project.configurations.runtime + project.tasks.jar.outputs.files
    }

    public void serviceName(String serviceName) {
        this.serviceName = serviceName
    }

    public void mainClass(String mainClass) {
        this.mainClass = mainClass
    }

    public List<String> args(String... args) {
        this.args = Arrays.asList(args)
    }

    public List<String> defaultJvmOpts(String... defaultJvmOpts) {
        this.defaultJvmOpts = Arrays.asList(defaultJvmOpts)
    }

    public void enableManifestClasspath(boolean enableManifestClasspath) {
        this.enableManifestClasspath = enableManifestClasspath
    }

    public void javaHome(String javaHome) {
        this.javaHome = javaHome
    }

    public void classpath(FileCollection classpath) {
        this.classpath = classpath
    }

    public String getServiceName() {
        return serviceName
    }

    public String getMainClass() {
        return mainClass
    }

    public List<String> getArgs() {
        return args
    }

    public List<String> getDefaultJvmOpts() {
        return defaultJvmOpts
    }

    public boolean isEnableManifestClasspath() {
        return enableManifestClasspath
    }

    public String getJavaHome() {
        return javaHome
    }

    public FileCollection getClasspath() {
        return classpath
    }

}
