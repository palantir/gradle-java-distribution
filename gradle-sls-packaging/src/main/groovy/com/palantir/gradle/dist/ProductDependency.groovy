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

package com.palantir.gradle.dist

import com.fasterxml.jackson.annotation.JsonProperty
import com.palantir.sls.versions.SlsVersion
import com.palantir.sls.versions.SlsVersionMatcher
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@ToString
@EqualsAndHashCode
@CompileStatic
class ProductDependency implements Serializable {

    private static final long serialVersionUID = 1L

    @JsonProperty("product-group")
    String productGroup

    @JsonProperty("product-name")
    String productName

    @JsonProperty("minimum-version")
    String minimumVersion

    @JsonProperty("recommended-version")
    String recommendedVersion

    @JsonProperty("maximum-version")
    String maximumVersion

    ProductDependency() {}

    ProductDependency(
            String productGroup,
            String productName,
            String minimumVersion,
            String maximumVersion,
            String recommendedVersion) {
        this.productGroup = productGroup
        this.productName = productName
        this.minimumVersion = minimumVersion
        this.maximumVersion = maximumVersion
        this.recommendedVersion = recommendedVersion
        isValid()
    }

    def isValid() {
        if (productGroup == null) {
            throw new IllegalArgumentException("productGroup must be specified")
        }
        if (productName == null) {
            throw new IllegalArgumentException("productName must be specified")
        }
        if (minimumVersion == null) {
            throw new IllegalArgumentException("minimum version must be specified")
        }
        maximumVersion.with {
            if (it == null || !SlsVersion.check(it) && !SlsVersionMatcher.safeValueOf(it).isPresent()) {
                throw new IllegalArgumentException(
                        "maximumVersion must be valid SLS version or version matcher: " + it)
            }
        }
        [minimumVersion, recommendedVersion].each {
            if (!SlsVersion.check(it)) {
                throw new IllegalArgumentException(
                        "minimumVersion and recommendedVersion must be valid SLS versions: " + it)
            }
        }
        if (minimumVersion == maximumVersion) {
            throw new IllegalArgumentException("minimumVersion and maximumVersion must be different "
                    + "in product dependency on " + this.productName)
        }
    }

}
