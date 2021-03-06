/*
 * (c) Copyright 2021 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.gradle.dist.artifacts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.api.artifacts.transform.CacheableTransform;
import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CacheableTransform
public abstract class SelectSingleFile implements TransformAction<FileExtractParameter> {
    private static final Logger log = LoggerFactory.getLogger(SelectSingleFile.class);

    @PathSensitive(PathSensitivity.NAME_ONLY)
    @InputArtifact
    public abstract Provider<FileSystemLocation> getInputArtifact();

    @Override
    public final void transform(TransformOutputs outputs) {
        File rootDir = getInputArtifact().get().getAsFile();
        Path pathToExtract =
                rootDir.toPath().resolve(getParameters().getPathToExtract().get());

        if (!Files.exists(pathToExtract)) {
            log.debug("Could not find '{}' in {}", pathToExtract, rootDir);
            return;
        }

        Path outputFile = outputs.file("SelectSingleFile-output").toPath();
        try {
            Files.copy(pathToExtract, outputFile);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to copy '%s'", pathToExtract), e);
        }
    }
}
