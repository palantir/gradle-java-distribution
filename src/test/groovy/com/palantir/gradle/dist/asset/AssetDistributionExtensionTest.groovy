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

package com.palantir.gradle.dist.asset

import spock.lang.Specification

class AssetDistributionExtensionTest extends Specification {
    def 'collection modifiers are cumulative'() {
        given:
        def ext = new AssetDistributionExtension(null)

        when:
        ext.with {
            assetDir "path/to/src", "relocated/dest"
            assetDir "path/to/src2", "relocated/dest2"
        }

        then:
        ext.assetDirs == ["path/to/src": "relocated/dest", "path/to/src2": "relocated/dest2"]
    }

    def 'collection setters replace existing data'() {
        given:
        def ext = new AssetDistributionExtension(null)

        when:
        ext.with {
            assetDir "path/to/src", "relocated/dest"
            setAssetDirs(["path/to/src2": "relocated/dest2"])
        }

        then:
        ext.assetDirs == ["path/to/src2": "relocated/dest2"]
    }
}
