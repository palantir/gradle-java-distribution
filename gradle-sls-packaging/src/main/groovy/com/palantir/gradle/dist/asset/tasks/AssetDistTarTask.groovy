package com.palantir.gradle.dist.asset.tasks

import com.palantir.gradle.dist.asset.AssetDistributionPlugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar

class AssetDistTarTask {

    static Tar createAssetDistTarTask(Project project, String taskName) {
        Tar tarTask = project.tasks.create(taskName, Tar) { p ->
            p.group = AssetDistributionPlugin.GROUP_NAME
            p.description = "Creates a compressed, gzipped tar file that contains required static assets."
            // Set compression in constructor so that task output has the right name from the start.
            p.compression = Compression.GZIP
            p.extension = 'sls.tgz'
        }
        return tarTask
    }

    static void configure(Tar distTar, String serviceName, Map<String, String> assetDirs) {
        distTar.configure {
            setBaseName(serviceName)
            // do the things that the java plugin would otherwise do for us
            def version = String.valueOf(project.version)
            setVersion(version)
            setDestinationDir(new File("${project.buildDir}/distributions"))
            String archiveRootDir = serviceName + '-' + version

            from("${project.projectDir}/deployment") {
                into "${archiveRootDir}/deployment"
            }

            into("${archiveRootDir}/deployment") {
                from("${project.buildDir}/deployment")
            }

            assetDirs.entrySet().each { entry ->
                from(project.file(entry.getKey())) {
                    into("${archiveRootDir}/asset/${entry.getValue()}")
                }
            }
        }
    }
}
