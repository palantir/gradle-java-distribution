package com.palantir.slspackaging.versions;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public final class SlsProductVersionsTest {

    @Test
    public void testOrderableVersionDetection() {
        assertThat(SlsProductVersions.isOrderableVersion("2.0.0-20-gaaaaaa")).isTrue();
        assertThat(SlsProductVersions.isOrderableVersion("2.0.0-20-gaaaaaa")).isTrue();
        assertThat(SlsProductVersions.isOrderableVersion("1.2.4")).isTrue();
        assertThat(SlsProductVersions.isOrderableVersion("2.0.0-beta1")).isTrue();
        assertThat(SlsProductVersions.isOrderableVersion("2.0.0-rc1")).isTrue();
        assertThat(SlsProductVersions.isOrderableVersion("2.0.0-beta1")).isTrue();

        assertThat(SlsProductVersions.isOrderableVersion(" 2.0.0")).isFalse();
        assertThat(SlsProductVersions.isOrderableVersion("2.0.0 ")).isFalse();
        assertThat(SlsProductVersions.isOrderableVersion("2.0.0-foo")).isFalse();
    }

    @Test
    public void testNonOrderableVersionDetection() {
        assertThat(SlsProductVersions.isNonOrderableVersion("2.0.0-20-gaaaaaa")).isTrue();
        assertThat(SlsProductVersions.isNonOrderableVersion("2.0.0-20-gaaaaaa")).isTrue();
        assertThat(SlsProductVersions.isNonOrderableVersion("2.0.0-beta1")).isTrue();
        assertThat(SlsProductVersions.isNonOrderableVersion("2.0.0-rc1")).isTrue();
        assertThat(SlsProductVersions.isNonOrderableVersion("2.0.0-beta1")).isTrue();
        assertThat(SlsProductVersions.isNonOrderableVersion("2.0.0-foo")).isTrue();
        assertThat(SlsProductVersions.isNonOrderableVersion("2.0.0-foo-g20-gaaaaaa")).isTrue();
        assertThat(SlsProductVersions.isNonOrderableVersion("2.0.0-foo-g20-gaaaaaa.dirty")).isTrue();

        assertThat(SlsProductVersions.isNonOrderableVersion("1.2.4")).isFalse();
        assertThat(SlsProductVersions.isNonOrderableVersion(" 2.0.0")).isFalse();
        assertThat(SlsProductVersions.isNonOrderableVersion("2.0.0 ")).isFalse();
    }

    @Test
    public void testValidVersionDetected() {
        assertThat(SlsProductVersions.isValidVersion("1.2.4")).isTrue();  // orderable
        assertThat(SlsProductVersions.isValidVersion("2.0.0-foo-g20-gaaaaaa")).isTrue();  // non-orderable

        assertThat(SlsProductVersions.isValidVersion(" 2.0.0")).isFalse();
        assertThat(SlsProductVersions.isValidVersion("2.0.0 ")).isFalse();
    }
}
