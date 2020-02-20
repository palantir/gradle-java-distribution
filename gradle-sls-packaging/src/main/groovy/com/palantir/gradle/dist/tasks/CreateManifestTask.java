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

package com.palantir.gradle.dist.tasks;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.palantir.gradle.dist.BaseDistributionExtension;
import com.palantir.gradle.dist.ConfigureProductDependenciesTask;
import com.palantir.gradle.dist.ProductDependency;
import com.palantir.gradle.dist.ProductDependencyLockFile;
import com.palantir.gradle.dist.ProductDependencyMerger;
import com.palantir.gradle.dist.ProductId;
import com.palantir.gradle.dist.ProductType;
import com.palantir.gradle.dist.RecommendedProductDependencies;
import com.palantir.gradle.dist.RecommendedProductDependenciesPlugin;
import com.palantir.gradle.dist.SlsManifest;
import com.palantir.sls.versions.OrderableSlsVersion;
import com.palantir.sls.versions.SlsVersion;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.gradle.StartParameter;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.util.GFileUtils;

public class CreateManifestTask extends DefaultTask {
    private static final Logger log = Logging.getLogger(CreateManifestTask.class);
    public static final ObjectMapper jsonMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final Property<String> serviceName = getProject().getObjects().property(String.class);
    private final Property<String> serviceGroup = getProject().getObjects().property(String.class);
    private final Property<ProductType> productType = getProject().getObjects().property(ProductType.class);

    private final ListProperty<ProductDependency> productDependencies =
            getProject().getObjects().listProperty(ProductDependency.class);
    private final SetProperty<ProductId> ignoredProductIds =
            getProject().getObjects().setProperty(ProductId.class);
    private final Property<Configuration> productDependenciesConfig =
            getProject().getObjects().property(Configuration.class);

    private final MapProperty<String, Object> manifestExtensions =
            getProject().getObjects().mapProperty(String.class, Object.class);
    private File manifestFile;

    public CreateManifestTask() {
        dependsOn(otherProjectProductDependenciesTasks());
    }

    /**
     * A lazy collection of tasks that ensure the {@link Jar} task of any project dependencies from
     * {@link #productDependenciesConfig} is correctly populated with the recommended product dependencies of that
     * project, if any (specifically, if they apply the {@link RecommendedProductDependenciesPlugin}).
     */
    private Provider<FileCollection> otherProjectProductDependenciesTasks() {
        return productDependenciesConfig.map(productDeps -> {
            // Using a ConfigurableFileCollection simply because it implements Buildable and provides a convenient API
            // to wire up task dependencies to it in a lazy way.
            ConfigurableFileCollection emptyFileCollection = getProject().files();
            productDeps.getIncoming().getArtifacts().getArtifacts().stream()
                    .flatMap(artifact -> {
                        ComponentIdentifier id = artifact.getId().getComponentIdentifier();

                        // Depend on the ConfigureProductDependenciesTask, if it exists, which will wire up the jar
                        // manifest
                        // with recommended product dependencies.
                        if (id instanceof ProjectComponentIdentifier) {
                            Project dependencyProject = getProject()
                                    .getRootProject()
                                    .project(((ProjectComponentIdentifier) id).getProjectPath());
                            return Stream.of(
                                    dependencyProject.getTasks().withType(ConfigureProductDependenciesTask.class));
                        }
                        return Stream.empty();
                    })
                    .forEach(emptyFileCollection::builtBy);
            return emptyFileCollection;
        });
    }

    @Input
    final Property<String> getServiceName() {
        return serviceName;
    }

    @Input
    final Property<String> getServiceGroup() {
        return serviceGroup;
    }

    @Input
    final Property<ProductType> getProductType() {
        return productType;
    }

    @Input
    final MapProperty<String, Object> getManifestExtensions() {
        return manifestExtensions;
    }

    @Input
    final ListProperty<ProductDependency> getProductDependencies() {
        return productDependencies;
    }

    @Input
    final SetProperty<ProductId> getIgnoredProductIds() {
        return ignoredProductIds;
    }

    @Input
    final Set<String> getProductDependenciesConfig() {
        // HACKHACK serializable way of representing all dependencies
        return productDependenciesConfig.get().getIncoming().getResolutionResult().getAllComponents().stream()
                // intentionally using a lambda as otherwise we break gradle 4.10 support
                .map(result -> result.getId())
                .map(ComponentIdentifier::getDisplayName)
                .collect(Collectors.toSet());
    }

    final void setConfiguration(Provider<Configuration> config) {
        this.productDependenciesConfig.set(config);
    }

    @Input
    final String getProjectVersion() {
        return getProject().getVersion().toString();
    }

    @OutputFile
    final File getManifestFile() {
        return manifestFile;
    }

    final void setManifestFile(File manifestFile) {
        this.manifestFile = manifestFile;
    }

    @TaskAction
    final void createManifest() throws Exception {
        validateProjectVersion();
        if (manifestExtensions.get().containsKey("product-dependencies")) {
            throw new IllegalArgumentException("Use productDependencies configuration option instead of setting "
                    + "'product-dependencies' key in manifestExtensions");
        }

        Map<ProductId, ProductDependency> allProductDependencies = Maps.newHashMap();
        getProductDependencies().get().forEach(declaredDep -> {
            ProductId productId = new ProductId(declaredDep.getProductGroup(), declaredDep.getProductName());
            Preconditions.checkArgument(
                    !serviceGroup.get().equals(productId.getProductGroup())
                            || !serviceName.get().equals(productId.getProductName()),
                    "Invalid for product to declare an explicit dependency on itself, please remove: %s",
                    declaredDep);
            if (getIgnoredProductIds().get().contains(productId)) {
                throw new IllegalArgumentException(String.format(
                        "Encountered product dependency declaration that was also ignored for '%s', either remove the "
                                + "dependency or ignore",
                        productId));
            } else if (allProductDependencies.containsKey(productId)) {
                throw new IllegalArgumentException(
                        String.format("Encountered duplicate declared product dependencies for '%s'", productId));
            }
            allProductDependencies.put(productId, declaredDep);
        });

        // Merge all discovered and declared product dependencies
        discoverProductDependencies().forEach((productId, discoveredDependency) -> {
            if (getIgnoredProductIds().get().contains(productId)) {
                log.trace("Ignored product dependency for '{}'", productId);
                return;
            }
            allProductDependencies.merge(productId, discoveredDependency, (declaredDependency, newDependency) -> {
                ProductDependency mergedDependency =
                        ProductDependencyMerger.merge(declaredDependency, discoveredDependency);
                if (mergedDependency.equals(discoveredDependency)) {
                    getLogger()
                            .error(
                                    "Please remove your declared product dependency on '{}' because it is"
                                            + " already provided by a jar dependency:\n\n"
                                            + "\tProvided:     {}\n"
                                            + "\tYou declared: {}",
                                    productId,
                                    discoveredDependency,
                                    declaredDependency);
                }
                return mergedDependency;
            });
        });
        List<ProductDependency> productDeps = new ArrayList<>(allProductDependencies.values());

        if (productDeps.isEmpty()) {
            requireAbsentLockfile();
        } else {
            ensureLockfileIsUpToDate(productDeps);
        }

        jsonMapper.writeValue(
                getManifestFile(),
                SlsManifest.builder()
                        .manifestVersion("1.0")
                        .productType(productType.get())
                        .productGroup(serviceGroup.get())
                        .productName(serviceName.get())
                        .productVersion(getProjectVersion())
                        .putAllExtensions(manifestExtensions.get())
                        .putExtensions("product-dependencies", productDeps)
                        .build());
    }

    private void requireAbsentLockfile() {
        File lockfile = getLockfile();
        Path relativePath = getProject().getRootDir().toPath().relativize(lockfile.toPath());

        if (!lockfile.exists()) {
            return;
        }

        if (getProject().getGradle().getStartParameter().isWriteDependencyLocks()) {
            lockfile.delete();
            getLogger().lifecycle("Deleted {}", relativePath);
        } else {
            throw new GradleException(String.format(
                    "%s must not exist, please run `./gradlew %s --write-locks` to delete it",
                    relativePath, getName()));
        }
    }

    /**
     * Intentionally checking whether file exists as gradle's {@link org.gradle.api.tasks.Optional} only operates on
     * whether the method returns null or not. Otherwise, it will fail when the file doesn't exist.
     */
    @InputFile
    @org.gradle.api.tasks.Optional
    final File getLockfileIfExists() {
        File file = getLockfile();
        if (file.exists()) {
            return file;
        }
        return null;
    }

    private File getLockfile() {
        return getProject().file(ProductDependencyLockFile.LOCK_FILE);
    }

    private void ensureLockfileIsUpToDate(List<ProductDependency> productDeps) {
        File lockfile = getLockfile();
        Path relativePath = getProject().getRootDir().toPath().relativize(lockfile.toPath());
        String upToDateContents =
                ProductDependencyLockFile.asString(productDeps, collectProductsPublishedInRepo(), getProjectVersion());
        boolean lockfileExists = lockfile.exists();

        if (getProject().getGradle().getStartParameter().isWriteDependencyLocks()) {
            GFileUtils.writeFile(upToDateContents, lockfile);
            if (!lockfileExists) {
                getLogger().lifecycle("Created {}\n\t{}", relativePath, upToDateContents.replaceAll("\n", "\n\t"));
            } else {
                getLogger().lifecycle("Updated {}", relativePath);
            }
        } else {
            if (!lockfileExists) {
                throw new GradleException(String.format(
                        "%s does not exist, please run `./gradlew %s --write-locks` and commit the resultant file",
                        relativePath, getName()));
            } else {
                String fromDisk = GFileUtils.readFile(lockfile);
                Preconditions.checkState(
                        fromDisk.equals(upToDateContents),
                        "%s is out of date, please run `./gradlew %s --write-locks` to update it%s",
                        relativePath,
                        getName(),
                        diff(lockfile, upToDateContents).map(s -> ":\n" + s).orElse(""));
            }
        }
    }

    /** Provide a rich diff so the user understands what change will be made before they run --write-locks. */
    private Optional<String> diff(File existing, String upToDateContents) {
        try {
            File tempFile = Files.createTempFile("product-dependencies", "lock").toFile();
            GFileUtils.writeFile(upToDateContents, tempFile);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            getProject().exec(spec -> {
                spec.commandLine("diff", "-u", existing.getAbsolutePath(), tempFile.getAbsolutePath());
                spec.setStandardOutput(baos);
                spec.setIgnoreExitValue(true);
            });
            return Optional.of(
                    Streams.stream(Splitter.on("\n").split(new String(baos.toByteArray(), StandardCharsets.UTF_8)))
                            .skip(2)
                            .collect(Collectors.joining("\n")));
        } catch (IOException e) {
            getLogger().debug("Unable to provide diff", e);
            return Optional.empty();
        }
    }

    private Map<ProductId, ProductDependency> discoverProductDependencies() {
        Map<ProductId, ProductDependency> discoveredProductDependencies = Maps.newHashMap();
        productDependenciesConfig.get().getIncoming().getArtifacts().getArtifacts().stream()
                .flatMap(artifact -> {
                    String artifactName = artifact.getId().getDisplayName();
                    ComponentIdentifier id = artifact.getId().getComponentIdentifier();
                    Optional<String> pdeps = Optional.empty();

                    // Extract product dependencies directly from Jar task for in project dependencies
                    if (id instanceof ProjectComponentIdentifier) {
                        Project dependencyProject = getProject()
                                .getRootProject()
                                .project(((ProjectComponentIdentifier) id).getProjectPath());
                        if (dependencyProject.getPlugins().hasPlugin(JavaPlugin.class)) {
                            Jar jar = (Jar) dependencyProject.getTasks().getByName(JavaPlugin.JAR_TASK_NAME);

                            pdeps = Optional.ofNullable(jar.getManifest()
                                            .getEffectiveManifest()
                                            .getAttributes()
                                            .get(RecommendedProductDependencies.SLS_RECOMMENDED_PRODUCT_DEPS_KEY))
                                    .map(Object::toString);
                        }
                    } else {
                        if (!artifact.getFile().exists()) {
                            log.debug("Artifact did not exist: {}", artifact.getFile());
                            return Stream.empty();
                        } else if (!com.google.common.io.Files.getFileExtension(
                                        artifact.getFile().getName())
                                .equals("jar")) {
                            log.debug("Artifact is not jar: {}", artifact.getFile());
                            return Stream.empty();
                        }

                        Manifest manifest;
                        try {
                            ZipFile zipFile = new ZipFile(artifact.getFile());
                            ZipEntry manifestEntry = zipFile.getEntry("META-INF/MANIFEST.MF");
                            if (manifestEntry == null) {
                                log.debug("Manifest file does not exist in jar for '${coord}'");
                                return Stream.empty();
                            }
                            manifest = new Manifest(zipFile.getInputStream(manifestEntry));
                        } catch (IOException e) {
                            log.warn(
                                    "IOException encountered when processing artifact '{}', file '{}', {}",
                                    artifactName,
                                    artifact.getFile(),
                                    e);
                            return Stream.empty();
                        }

                        pdeps = Optional.ofNullable(manifest.getMainAttributes()
                                .getValue(RecommendedProductDependencies.SLS_RECOMMENDED_PRODUCT_DEPS_KEY));
                    }
                    if (!pdeps.isPresent()) {
                        log.debug(
                                "No product dependency found for artifact '{}', file '{}'",
                                artifactName,
                                artifact.getFile());
                        return Stream.empty();
                    }

                    try {
                        RecommendedProductDependencies recommendedDeps =
                                jsonMapper.readValue(pdeps.get(), RecommendedProductDependencies.class);
                        return recommendedDeps.recommendedProductDependencies().stream()
                                .map(recommendedDep ->
                                        new ProductDependency(
                                                recommendedDep.getProductGroup(),
                                                recommendedDep.getProductName(),
                                                recommendedDep.getMinimumVersion(),
                                                recommendedDep.getMaximumVersion(),
                                                recommendedDep.getRecommendedVersion()))
                                .peek(productDependency -> log.info(
                                        "Product dependency recommendation made by artifact '{}', file '{}', "
                                                + "dependency recommendation '{}'",
                                        artifactName,
                                        artifact,
                                        productDependency));
                    } catch (IOException | IllegalArgumentException e) {
                        log.debug(
                                "Failed to load product dependency for artifact '{}', file '{}', '{}'",
                                artifactName,
                                artifact,
                                e);
                        return Stream.empty();
                    }
                })
                .filter(this::isNotSelfProductDependency)
                .forEach(productDependency -> discoveredProductDependencies.merge(
                        new ProductId(productDependency.getProductGroup(), productDependency.getProductName()),
                        productDependency,
                        ProductDependencyMerger::merge));
        return discoveredProductDependencies;
    }

    private boolean isNotSelfProductDependency(ProductDependency dependency) {
        return !serviceGroup.get().equals(dependency.getProductGroup())
                || !serviceName.get().equals(dependency.getProductName());
    }

    private void validateProjectVersion() {
        String stringVersion = getProjectVersion();
        Preconditions.checkArgument(
                SlsVersion.check(stringVersion), "Project version must be a valid SLS version: %s", stringVersion);
        if (!OrderableSlsVersion.check(stringVersion)) {
            getProject()
                    .getLogger()
                    .warn(
                            "Version string in project {} is not orderable as per SLS specification: {}",
                            getProject().getName(),
                            stringVersion);
        }
    }

    private Set<ProductId> collectProductsPublishedInRepo() {
        // get products we publish via BaseDistributionExtension from all other projects
        return getProject().getRootProject().getAllprojects().stream()
                .flatMap(p -> Optional.ofNullable(p.getExtensions().findByType(BaseDistributionExtension.class))
                        .map(Stream::of)
                        .orElseGet(Stream::empty))
                .map(extension ->
                        new ProductId(
                                extension.getDistributionServiceGroup().get(),
                                extension.getDistributionServiceName().get()))
                .collect(Collectors.toSet());
    }

    public static TaskProvider<CreateManifestTask> createManifestTask(Project project, BaseDistributionExtension ext) {
        TaskProvider<CreateManifestTask> createManifest = project.getTasks()
                .register("createManifest", CreateManifestTask.class, task -> {
                    task.getServiceName().set(ext.getDistributionServiceName());
                    task.getServiceGroup().set(ext.getDistributionServiceGroup());
                    task.getProductType().set(ext.getProductType());
                    task.setManifestFile(new File(project.getBuildDir(), "/deployment/manifest.yml"));
                    task.getProductDependencies().set(ext.getProductDependencies());
                    task.setConfiguration(project.provider(ext::getProductDependenciesConfig));
                    task.getIgnoredProductIds().set(ext.getIgnoredProductDependencies());
                    task.getManifestExtensions().set(ext.getManifestExtensions());
                });
        project.getPluginManager().withPlugin("lifecycle-base", p -> {
            project.getTasks().named(LifecycleBasePlugin.CHECK_TASK_NAME).configure(task ->
                    task.dependsOn(createManifest));
        });

        // We want `./gradlew --write-locks` to magically fix up the product-dependencies.lock file
        // We can't do this at configuration time because it would mess up gradle-consistent-versions.
        StartParameter startParam = project.getGradle().getStartParameter();
        if (startParam.isWriteDependencyLocks() && !startParam.getTaskNames().contains("createManifest")) {
            List<String> taskNames = ImmutableList.<String>builder()
                    .addAll(startParam.getTaskNames())
                    .add("createManifest")
                    .build();
            startParam.setTaskNames(taskNames);
        }

        return createManifest;
    }
}
