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

package com.palantir.gradle.dist.pod

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.palantir.gradle.dist.GradleIntegrationSpec
import org.gradle.testkit.runner.BuildResult

class PodDistributionPluginIntegrationSpec extends GradleIntegrationSpec {

    def 'manifest file contains expected fields'() {
        given:
        createUntarBuildFile(buildFile)
        buildFile << '''
            distribution {
                podName 'pod-name'
            }
        '''.stripIndent()

        when:
        runSuccessfully(':configTar', ':untar')

        then:
        String manifest = file('dist/pod-name-0.0.1/deployment/manifest.yml', projectDir).text
        manifest.contains('"manifest-version": "1.0"')
        manifest.contains('"product-group": "service-group"')
        manifest.contains('"product-name": "pod-name"')
        manifest.contains('"product-version": "0.0.1"')
        manifest.contains('"product-type": "pod.v1"')
    }

    def 'podName defaults to project name '() {
        given:
        buildFile << '''
            plugins {
                id 'com.palantir.sls-pod-distribution'
            }

            version "0.0.1"
            project.group = 'service-group'

            distribution {
                service "bar-service", {
                  productGroup = "com.palantir.foo"
                  productName = "bar"
                  productVersion = "1.0.0"
                }
            }

            // most convenient way to untar the dist is to use gradle
            task untar (type: Copy) {
                from tarTree(resources.gzip("${buildDir}/distributions/root-project-0.0.1.pod.config.tgz"))
                into "${projectDir}/dist"
                dependsOn configTar
            }
        '''.stripIndent()

        when:
        runSuccessfully(':configTar', ':untar')

        then:
        String manifest = file('dist/root-project-0.0.1/deployment/manifest.yml', projectDir).text
        manifest.contains('"manifest-version": "1.0"')
        manifest.contains('"product-group": "service-group"')
        manifest.contains('"product-name": "root-project"')
        manifest.contains('"product-version": "0.0.1"')
        manifest.contains('"product-type": "pod.v1"')
    }

    def 'pod file contains expected fields'() {
        given:
        createUntarBuildFile(buildFile)
        buildFile << '''
            distribution {
                podName 'pod-name'

                service "bar-service", {
                  productGroup = "com.palantir.foo"
                  productName = "bar"
                  productVersion = "1.0.0"
                  volumeMap = ["bar-volume": "random-volume"]
                }
                service "baz-service", {
                  productGroup = "com.palantir.foo"
                  productName = "baz"
                  productVersion = "1.0.0"
                  volumeMap = ["baz-volume": "random-volume"]
                }

                volume "random-volume", {
                  desiredSize = "10G"
                }
            }
        '''.stripIndent()

        when:
        runSuccessfully(':configTar', ':untar')

        then:
        String pod = file('dist/pod-name-0.0.1/deployment/pod.yml', projectDir).text

        // verify pod YAML file contents
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
        def podYaml = mapper.readTree(pod)

        podYaml.has("services")
        podYaml.get("services").has("bar-service")
        podYaml.get("services").has("baz-service")
        podYaml.get("services").get("bar-service").get("product-name").asText().contains("bar")
        podYaml.get("services").get("bar-service").get("product-group").asText().contains("com.palantir.foo")
        podYaml.has("volumes")
        podYaml.get("volumes").get("random-volume").get("desired-size").asText().contains("10G")
    }

    def 'pod file creation fails with bad service names'() {
        given:
        createUntarBuildFile(buildFile)
        buildFile << '''
            distribution {
                podName 'pod-name'

                service "barService", {
                  productGroup = "com.palantir.foo"
                  productName = "bar"
                  productVersion = "1.0.0"
                  volumeMap = ["bar-volume": "random-volume"]
                }
                service "bazService", {
                  productGroup = "com.palantir.foo"
                  productName = "baz"
                  productVersion = "1.0.0"
                  volumeMap = ["baz-volume": "random-volume"]
                }

                volume "random-volume", {
                  desiredSize = "10G"
                }
            }
        '''.stripIndent()

        when:
        BuildResult buildResult = run(':configTar').buildAndFail()

        then:
        buildResult.getOutput().contains("Pod validation failed for service barService: service names must be kebab case")
    }

    def 'pod file creation fails with no product group'() {
        given:
        createUntarBuildFile(buildFile)
        buildFile << '''
            distribution {
                podName 'pod-name'
                service "bar-service", {
                  productName = "bar"
                  productVersion = "1.0.0"
                  volumeMap = ["bar-volume": "random-volume"]
                }
                volume "random-volume", {
                  desiredSize = "10G"
                }
            }
        '''.stripIndent()

        when:
        BuildResult buildResult = run(':configTar').buildAndFail()

        then:
        buildResult.getOutput().contains("Pod validation failed for service bar-service: product group must be specified for pod service")
    }

    def 'pod file creation fails with no product name'() {
        given:
        createUntarBuildFile(buildFile)
        buildFile << '''
            distribution {
                podName 'pod-name'
                service "bar-service", {
                  productGroup = "com.palantir.foo"
                  productVersion = "1.0.0"
                  volumeMap = ["bar-volume": "random-volume"]
                }
                volume "random-volume", {
                  desiredSize = "10G"
                }
            }
        '''.stripIndent()

        when:
        BuildResult buildResult = run(':configTar').buildAndFail()

        then:
        buildResult.getOutput().contains("Pod validation failed for service bar-service: product name must be specified for pod service")
    }

    def 'pod file creation fails with no product version '() {
        given:
        createUntarBuildFile(buildFile)
        buildFile << '''
            distribution {
                podName 'pod-name'
                service "bar-service", {
                  productGroup = "com.palantir.foo"
                  productName = "bar"
                  volumeMap = ["bar-volume": "random-volume"]
                }
                volume "random-volume", {
                  desiredSize = "10G"
                }
            }
        '''.stripIndent()

        when:
        BuildResult buildResult = run(':configTar').buildAndFail()

        then:
        buildResult.getOutput().contains("Pod validation failed for service bar-service: product version must be specified and be a valid SLS version for pod service")
    }

    def 'pod file creation fails with invalid product version '() {
        given:
        createUntarBuildFile(buildFile)
        buildFile << '''
            distribution {
                podName 'pod-name'
                service "bar-service", {
                  productGroup = "com.palantir.foo"
                  productName = "bar"
                  productVersion = "not-a-valid-sls-version"
                  volumeMap = ["bar-volume": "random-volume"]
                }
                volume "random-volume", {
                  desiredSize = "10G"
                }
            }
        '''.stripIndent()

        when:
        BuildResult buildResult = run(':configTar').buildAndFail()

        then:
        buildResult.getOutput().contains("Pod validation failed for service bar-service: product version must be specified and be a valid SLS version for pod service")
    }

    def 'pod file creation fails with bad volume mappings'() {
        given:
        createUntarBuildFile(buildFile)
        buildFile << '''
            distribution {
                podName 'pod-name'

                service "bar-service", {
                  productGroup = "com.palantir.foo"
                  productName = "bar"
                  productVersion = "1.0.0"
                  volumeMap = ["bar-volume": "random-volume"]
                }
                service "baz-service", {
                  productGroup = "com.palantir.foo"
                  productName = "baz"
                  productVersion = "1.0.0"
                  volumeMap = ["baz-volume": "not-a-defined-volume"]
                }

                volume "random-volume", {
                  desiredSize = "10G"
                }
            }
        '''.stripIndent()

        when:
        BuildResult buildResult = run(':configTar').buildAndFail()

        then:
        buildResult.getOutput().contains("Pod validation failed for service baz-service: service volume mapping cannot contain undeclared volumes")
    }

    def 'pod file creation fails with volume name too long'() {
        given:
        createUntarBuildFile(buildFile)
        buildFile << '''
            distribution {
                podName 'pod-name'

                service "bar-service", {
                  productGroup = "com.palantir.foo"
                  productName = "bar"
                  productVersion = "1.0.0"
                  volumeMap = ["bar-volume": "aaaaaaaaaaaaaaaaaaaaaaaaaa"]
                }

                volume "aaaaaaaaaaaaaaaaaaaaaaaaaa", {
                  desiredSize = "10G"
                }
            }
        '''.stripIndent()

        when:
        BuildResult buildResult = run(':configTar').buildAndFail()

        then:
        buildResult.getOutput().contains("Pod validation failed for volume aaaaaaaaaaaaaaaaaaaaaaaaaa: volume names must be fewer than 25 characters")
    }

    def 'pod file creation fails with bad volume name'() {
        given:
        createUntarBuildFile(buildFile)
        buildFile << '''
            distribution {
                podName 'pod-name'

                service "bar-service", {
                  productGroup = "com.palantir.foo"
                  productName = "bar"
                  productVersion = "1.0.0"
                  volumeMap = ["bar-volume": "Not-A-Valid-Volume"]
                }

                volume "Not-A-Valid-Volume", {
                  desiredSize = "10G"
                }
            }
        '''.stripIndent()

        when:
        BuildResult buildResult = run(':configTar').buildAndFail()

        then:
        buildResult.getOutput().contains("Pod validation failed for volume Not-A-Valid-Volume: volume name does not conform to the required regex")
    }

    def 'pod file creation fails with bad volume desired size'() {
        given:
        createUntarBuildFile(buildFile)
        buildFile << '''
            distribution {
                podName 'pod-name'

                service "bar-service", {
                  productGroup = "com.palantir.foo"
                  productName = "bar"
                  productVersion = "1.0.0"
                  volumeMap = ["bar-volume": "random-volume"]
                }

                volume "random-volume", {
                  desiredSize = "10 GiB"
                }
            }
        '''.stripIndent()

        when:
        BuildResult buildResult = run(':configTar').buildAndFail()

        then:
        buildResult.getOutput().contains("Pod validation failed for volume random-volume: volume desired size of 10 GiB does not conform to the required regex ^\\d+?(M|G|T)\$")
    }

    private static createUntarBuildFile(buildFile) {
        buildFile << '''
            plugins {
                id 'com.palantir.sls-pod-distribution'
            }

            version "0.0.1"
            project.group = 'service-group'

            // most convenient way to untar the dist is to use gradle
            task untar (type: Copy) {
                from tarTree(resources.gzip("${buildDir}/distributions/pod-name-0.0.1.pod.config.tgz"))
                into "${projectDir}/dist"
                dependsOn configTar
            }
        '''.stripIndent()
    }
}

