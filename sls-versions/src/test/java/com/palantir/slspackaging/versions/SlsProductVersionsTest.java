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

package com.palantir.slspackaging.versions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public final class SlsProductVersionsTest {

    @Test
    public void testOrderableVersionDetection() {
        assertThat(SlsProductVersions.isOrderableVersion("2.0.0-20-gaaaaaa")).isTrue();
        assertThat(SlsProductVersions.isOrderableVersion("2.0.0-20-gaaaaaa")).isTrue();
        assertThat(SlsProductVersions.isOrderableVersion("1.2.4")).isTrue();
        assertThat(SlsProductVersions.isOrderableVersion("2.0.0-rc1")).isTrue();
        assertThat(SlsProductVersions.isOrderableVersion("2.0.0-rc1-2-gbbbbbbb")).isTrue();

        assertThat(SlsProductVersions.isOrderableVersion("2.0.0-rc.1")).isFalse();
        assertThat(SlsProductVersions.isOrderableVersion("2.0.0-rc1-b-gaaaaaaa")).isFalse();
        assertThat(SlsProductVersions.isOrderableVersion(" 2.0.0")).isFalse();
        assertThat(SlsProductVersions.isOrderableVersion("2.0.0 ")).isFalse();
        assertThat(SlsProductVersions.isOrderableVersion("2.0.0-foo")).isFalse();
    }

    @Test
    public void testNonOrderableVersionDetection() {
        assertThat(SlsProductVersions.isNonOrderableVersion("2.0.0-20-gaaaaaa")).isTrue();
        assertThat(SlsProductVersions.isNonOrderableVersion("2.0.0-20-gaaaaaa.dirty")).isTrue();
        assertThat(SlsProductVersions.isNonOrderableVersion("2.0.0-rc1")).isTrue();
        assertThat(SlsProductVersions.isNonOrderableVersion("2.0.0-rc1.dirty")).isTrue();
        assertThat(SlsProductVersions.isNonOrderableVersion("2.0.0-rc1-2-gbbbbbbb")).isTrue();
        assertThat(SlsProductVersions.isNonOrderableVersion("2.0.0-rc1-2-gbbbbbbb.dirty")).isTrue();
        assertThat(SlsProductVersions.isNonOrderableVersion("2.0.0-foo")).isTrue();
        assertThat(SlsProductVersions.isNonOrderableVersion("2.0.0-foo.dirty")).isTrue();
        assertThat(SlsProductVersions.isNonOrderableVersion("2.0.0-foo-g20-gaaaaaa")).isTrue();
        assertThat(SlsProductVersions.isNonOrderableVersion("2.0.0-foo-g20-gaaaaaa.dirty")).isTrue();
        assertThat(SlsProductVersions.isNonOrderableVersion("2.0.0.dirty")).isTrue();
        assertThat(SlsProductVersions.isNonOrderableVersion("1.2.4")).isTrue();

        assertThat(SlsProductVersions.isNonOrderableVersion("2.0.0-rc.1")).isFalse();
        assertThat(SlsProductVersions.isNonOrderableVersion("2.0.0-foo.bar")).isFalse();
        assertThat(SlsProductVersions.isNonOrderableVersion(" 2.0.0")).isFalse();
        assertThat(SlsProductVersions.isNonOrderableVersion("2.0.0 ")).isFalse();
    }

    @Test
    public void testMatcherDetection() {
        assertThat(SlsProductVersions.isMatcher("2.0.x")).isTrue();
        assertThat(SlsProductVersions.isMatcher("2.x.x")).isTrue();
        assertThat(SlsProductVersions.isMatcher("x.x.x")).isTrue();

        assertThat(SlsProductVersions.isMatcher("2.0.0")).isTrue();
        assertThat(SlsProductVersions.isMatcher("1.x.x.x")).isFalse();
        assertThat(SlsProductVersions.isMatcher("x.y.z")).isFalse();
        assertThat(SlsProductVersions.isMatcher("x.x.x-rc1")).isFalse();
        assertThat(SlsProductVersions.isMatcher("x.x.x-1-gaaaaaa")).isFalse();
        assertThat(SlsProductVersions.isMatcher("x.x.x-foo")).isFalse();
        assertThat(SlsProductVersions.isMatcher("x.x.3")).isFalse();
        assertThat(SlsProductVersions.isMatcher("x.2.3")).isFalse();
        assertThat(SlsProductVersions.isMatcher("x.2.x")).isFalse();
        assertThat(SlsProductVersions.isMatcher("1.x.3")).isFalse();
    }

    @Test
    public void testValidVersionDetected() {
        assertThat(SlsProductVersions.isValidVersion("1.2.4")).isTrue();  // orderable
        assertThat(SlsProductVersions.isValidVersion("2.0.0-foo-g20-gaaaaaa")).isTrue();  // non-orderable

        assertThat(SlsProductVersions.isValidVersion("2.x.x")).isFalse();
        assertThat(SlsProductVersions.isValidVersion(" 2.0.0")).isFalse();
        assertThat(SlsProductVersions.isValidVersion("2.0.0 ")).isFalse();
    }

    @Test
    public void testValidVersionOrMatcherDetected() {
        assertThat(SlsProductVersions.isValidVersionOrMatcher("1.2.4")).isTrue();  // orderable
        assertThat(SlsProductVersions.isValidVersionOrMatcher("2.0.0-foo-g20-gaaaaaa")).isTrue();  // non-orderable
        assertThat(SlsProductVersions.isValidVersionOrMatcher("2.x.x")).isTrue(); // matcher

        assertThat(SlsProductVersions.isValidVersionOrMatcher(" 2.0.0")).isFalse();
        assertThat(SlsProductVersions.isValidVersionOrMatcher("2.0.0 ")).isFalse();
    }
}
