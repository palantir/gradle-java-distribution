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
package com.palantir.gradle.dist.service

import com.palantir.gradle.dist.BaseDistributionExtension
import org.gradle.api.Project;

class ServiceDistributionExtension extends BaseDistributionExtension {

    private final Project project

    private String mainClass
    private List<String> args = []
    private List<String> checkArgs = []
    private List<String> defaultJvmOpts = []
    private Map<String,String> env = [:]
    private boolean enableManifestClasspath = false
    private String javaHome = null
    private List<String> excludeFromVar = ['log', 'run']

    ServiceDistributionExtension(Project project) {
        super(project)
        this.project = project
        setProductType("service.v1")
    }

    public void mainClass(String mainClass) {
        this.mainClass = mainClass
    }

    public void args(String... args) {
        this.args.addAll(args)
    }

    public void setArgs(Iterable<String> args) {
        this.args = args.toList()
    }

    public void checkArgs(String... checkArgs) {
        this.checkArgs.addAll(checkArgs)
    }

    public void setCheckArgs(Iterable<String> checkArgs) {
        this.checkArgs = checkArgs.toList()
    }

    public void defaultJvmOpts(String... defaultJvmOpts) {
        this.defaultJvmOpts.addAll(defaultJvmOpts)
    }

    public void setDefaultJvmOpts(Iterable<String> defaultJvmOpts) {
        this.defaultJvmOpts = defaultJvmOpts.toList()
    }

    public void enableManifestClasspath(boolean enableManifestClasspath) {
        this.enableManifestClasspath = enableManifestClasspath
    }

    public void javaHome(String javaHome) {
        this.javaHome = javaHome
    }

    public void setEnv(Map<String, String> env) {
        this.env = env
    }

    public void env(Map<String, String> env) {
        this.env.putAll(env)
    }

    public void excludeFromVar(String... excludeFromVar) {
        this.excludeFromVar.addAll(excludeFromVar)
    }

    public void setExcludeFromVar(Iterable<String> excludeFromVar) {
        this.excludeFromVar = excludeFromVar.toList()
    }

    public String getMainClass() {
        return mainClass
    }

    public List<String> getArgs() {
        return args
    }

    public List<String> getCheckArgs() {
        return checkArgs
    }

    public List<String> getDefaultJvmOpts() {
        return defaultJvmOpts
    }

    public boolean isEnableManifestClasspath() {
        return enableManifestClasspath
    }

    public Map<String, String> getEnv() {
        return env
    }

    public String getJavaHome() {
        return javaHome
    }

    public List<String> getExcludeFromVar() {
        return excludeFromVar
    }
}
