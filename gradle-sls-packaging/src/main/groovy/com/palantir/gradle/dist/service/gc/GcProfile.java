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

package com.palantir.gradle.dist.service.gc;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface GcProfile extends Serializable {
    long serialVersionUID = 1L;

    @VisibleForTesting
    Map<String, Class<? extends GcProfile>> PROFILE_NAMES = ImmutableMap.of(
            "throughput", GcProfile.Throughput.class,
            "response-time", GcProfile.ResponseTime.class,
            "hybrid", GcProfile.Hybrid.class);

    List<String> gcJvmOpts();

    class Throughput implements GcProfile {
        @Override
        public final List<String> gcJvmOpts() {
            return ImmutableList.of("-XX:+UseParallelOldGC");
        }
    }

    class ResponseTime implements GcProfile {
        private int initiatingOccupancyFraction = 68;

        @Override
        public final List<String> gcJvmOpts() {
            return ImmutableList.of("-XX:+UseParNewGC",
                    "-XX:+UseConcMarkSweepGC",
                    "-XX:+UseCMSInitiatingOccupancyOnly",
                    "-XX:CMSInitiatingOccupancyFraction=" + initiatingOccupancyFraction,
                    "-XX:+CMSClassUnloadingEnabled",
                    "-XX:+ExplicitGCInvokesConcurrent",
                    "-XX:+ClassUnloadingWithConcurrentMark",
                    "-XX:+CMSScavengeBeforeRemark",
                    // 'UseParNewGC' was removed in Java10: https://bugs.openjdk.java.net/browse/JDK-8173421
                    "-XX:+IgnoreUnrecognizedVMOptions");
        }

        public final void initiatingOccupancyFraction(int occupancyFraction) {
            this.initiatingOccupancyFraction = occupancyFraction;
        }
    }

    class Hybrid implements GcProfile {
        @Override
        public final List<String> gcJvmOpts() {
            return ImmutableList.of(
                    "-XX:+UseG1GC",
                    "-XX:+UseStringDeduplication");
        }
    }

    class ResponseTime11 implements GcProfile {
        @Override
        public final List<String> gcJvmOpts() {
            return ImmutableList.of(
                    // https://wiki.openjdk.java.net/display/shenandoah/Main
                    "-XX:+UseShenandoahGC",
                    // "forces concurrent cycle instead of Full GC on System.gc()"
                    "-XX:+ExplicitGCInvokesConcurrent",
                    "-XX:+ClassUnloadingWithConcurrentMark");
        }
    }
}
