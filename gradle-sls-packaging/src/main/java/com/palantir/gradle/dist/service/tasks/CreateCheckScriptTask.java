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

package com.palantir.gradle.dist.service.tasks;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.palantir.gradle.dist.service.JavaServiceDistributionPlugin;
import com.palantir.gradle.dist.service.util.EmitFiles;
import java.io.IOException;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

public class CreateCheckScriptTask extends DefaultTask {
    private final Property<String> serviceName = getProject().getObjects().property(String.class);
    private final ListProperty<String> checkArgs = getProject().getObjects().listProperty(String.class);
    private final RegularFileProperty outputFile = getProject().getObjects().fileProperty();

    public CreateCheckScriptTask() {
        outputFile.set(getProject().getLayout().getBuildDirectory().file("monitoring/check.sh"));
    }

    @Input
    public final Property<String> getServiceName() {
        return serviceName;
    }

    @Input
    public final ListProperty<String> getCheckArgs() {
        return checkArgs;
    }

    @OutputFile
    public final RegularFileProperty getOutputFile() {
        return outputFile;
    }

    @TaskAction
    final void createInitScript() throws IOException {
        if (!checkArgs.get().isEmpty()) {
            EmitFiles.replaceVars(
                            JavaServiceDistributionPlugin.class.getResourceAsStream("/check.sh"),
                            getOutputFile().get().getAsFile().toPath(),
                            ImmutableMap.of(
                                    "@serviceName@", serviceName.get(),
                                    "@checkArgs@", Joiner.on(" ").join(checkArgs.get())))
                    .toFile()
                    .setExecutable(true);
        }
    }
}
