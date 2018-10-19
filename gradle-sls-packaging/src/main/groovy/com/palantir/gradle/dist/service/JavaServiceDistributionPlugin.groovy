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
package com.palantir.gradle.dist.service

import com.palantir.gradle.dist.asset.AssetDistributionPlugin
import com.palantir.gradle.dist.pod.PodDistributionPlugin
import com.palantir.gradle.dist.service.tasks.CopyLauncherBinariesTask
import com.palantir.gradle.dist.service.tasks.CopyYourkitAgentTask
import com.palantir.gradle.dist.service.tasks.CopyYourkitLicenseTask
import com.palantir.gradle.dist.service.tasks.CreateCheckScriptTask
import com.palantir.gradle.dist.service.tasks.CreateInitScriptTask
import com.palantir.gradle.dist.service.tasks.CreateStartScriptsTask
import com.palantir.gradle.dist.service.tasks.DistTarTask
import com.palantir.gradle.dist.service.tasks.LaunchConfigTask
import com.palantir.gradle.dist.service.tasks.ManifestClasspathJarTask
import com.palantir.gradle.dist.service.tasks.RunTask
import com.palantir.gradle.dist.tasks.ConfigTarTask
import com.palantir.gradle.dist.tasks.CreateManifestTask
import groovy.transform.CompileStatic
import org.gradle.api.InvalidUserCodeException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.Tar
import org.gradle.jvm.application.tasks.CreateStartScripts

@CompileStatic
class JavaServiceDistributionPlugin implements Plugin<Project> {

    static final String GROUP_NAME = "Distribution"
    static final String SLS_CONFIGURATION_NAME = "sls"

    void apply(Project project) {
        if (project.getPlugins().hasPlugin(AssetDistributionPlugin)) {
            throw new InvalidUserCodeException("The plugins 'com.palantir.sls-asset-distribution' and 'com.palantir.sls-java-service-distribution' cannot be used in the same Gradle project.")
        }
        if (project.getPlugins().hasPlugin(PodDistributionPlugin)) {
            throw new InvalidUserCodeException("The plugins 'com.palantir.sls-pod-distribution' and 'com.palantir.sls-java-service-distribution' cannot be used in the same Gradle project.")
        }
        project.plugins.apply('java')
        project.extensions.create('distribution', JavaServiceDistributionExtension, project)

        project.configurations.create('goJavaLauncherBinaries')
        project.dependencies.with {
            add('goJavaLauncherBinaries', 'com.palantir.launching:go-java-launcher:1.5.1')
            add('goJavaLauncherBinaries', 'com.palantir.launching:go-init:1.5.1')
        }

        def distributionExtension = project.extensions.findByType(JavaServiceDistributionExtension)

        // Create tasks
        Task manifestClasspathJar = ManifestClasspathJarTask.createManifestClasspathJarTask(project, "manifestClasspathJar")
        project.afterEvaluate {
            manifestClasspathJar.onlyIf { distributionExtension.isEnableManifestClasspath() }
        }

        CreateStartScripts startScripts = CreateStartScriptsTask.createStartScriptsTask(project, 'createStartScripts')
        project.afterEvaluate {
            CreateStartScriptsTask.configure(
                    startScripts,
                    distributionExtension.mainClass,
                    distributionExtension.serviceName,
                    distributionExtension.defaultJvmOpts,
                    distributionExtension.enableManifestClasspath)
        }

        CopyLauncherBinariesTask copyLauncherBinaries = project.tasks.create('copyLauncherBinaries', CopyLauncherBinariesTask)

        LaunchConfigTask launchConfig = project.tasks.create('createLaunchConfig', LaunchConfigTask)
        project.afterEvaluate {
            launchConfig.configure(
                    distributionExtension.mainClass,
                    distributionExtension.serviceName,
                    distributionExtension.args,
                    distributionExtension.checkArgs,
                    distributionExtension.gc,
                    distributionExtension.defaultJvmOpts,
                    distributionExtension.addJava8GCLogging,
                    distributionExtension.javaHome,
                    distributionExtension.env,
                    project.tasks.getByName(JavaPlugin.JAR_TASK_NAME).outputs.files + project.configurations.getByName('runtimeClasspath'))
        }

        CreateInitScriptTask initScript = project.tasks.create('createInitScript', CreateInitScriptTask)
        project.afterEvaluate {
            initScript.configure(distributionExtension.serviceName)
        }

        CreateCheckScriptTask checkScript = project.tasks.create('createCheckScript', CreateCheckScriptTask)
        project.afterEvaluate {
            checkScript.configure(distributionExtension.serviceName, distributionExtension.checkArgs)
        }

        CopyYourkitAgentTask yourkitAgent = project.tasks.create('copyYourkitAgent', CopyYourkitAgentTask)
        CopyYourkitLicenseTask yourkitLicense = project.tasks.create('copyYourkitLicense', CopyYourkitLicenseTask)

        distributionExtension.productDependenciesConfig = project.configurations.getByName("runtime")

        CreateManifestTask manifest = project.tasks.create('createManifest', CreateManifestTask)
        project.afterEvaluate {
            manifest.configure(
                    distributionExtension.serviceName,
                    distributionExtension.serviceGroup,
                    distributionExtension.productType,
                    distributionExtension.manifestExtensions,
                    distributionExtension.serviceDependencies,
                    distributionExtension.productDependenciesConfig,
                    distributionExtension.ignoredProductIds)
        }

        Tar distTar = DistTarTask.createDistTarTask(project, 'distTar')
        project.afterEvaluate {
            DistTarTask.configure(
                    distTar,
                    project,
                    distributionExtension.serviceName,
                    distributionExtension.excludeFromVar,
                    distributionExtension.isEnableManifestClasspath())
        }

        Tar configTar = ConfigTarTask.createConfigTarTask(project, 'configTar', distributionExtension.productType)
        project.afterEvaluate {
            ConfigTarTask.configure(configTar, project, distributionExtension.serviceName)
        }

        JavaExec run = RunTask.createRunTask(project, 'run')
        project.afterEvaluate {
            RunTask.configure(run, distributionExtension.mainClass, distributionExtension.args, distributionExtension.defaultJvmOpts,)
        }

        // Create configuration and exported artifacts
        project.configurations.create(SLS_CONFIGURATION_NAME)
        project.artifacts.add(SLS_CONFIGURATION_NAME, distTar)

        // Configure tasks
        distTar.dependsOn startScripts, initScript, checkScript, yourkitAgent, yourkitLicense, copyLauncherBinaries, launchConfig, manifest, manifestClasspathJar
        configTar.dependsOn manifest
    }
}
