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

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
@CompileStatic
class ProductId implements Serializable {

    private static final long serialVersionUID = 1L

    String productGroup
    String productName

    ProductId(String productGroup, String productName) {
        this.productGroup = productGroup
        this.productName = productName
    }

    ProductId(String productId) {
        def split = productId.split(":")
        if (split.length != 2) {
            throw new IllegalArgumentException("Invalid product ID: " + split)
        }
        this.productGroup = split[0]
        this.productName = split[1]
    }

    @Override
    String toString() {
        return productGroup + ":" + productName
    }
}
