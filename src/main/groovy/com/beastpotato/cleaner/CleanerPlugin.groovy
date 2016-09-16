package com.beastpotato.cleaner


import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.api.BaseVariant
import com.android.ide.common.res2.ResourceSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection

/**
 * Created by Oleksiy on 9/13/2016.
 */

/**
 * For more info run with "gradlew cleanerTask -info"
 */
class CleanerPlugin implements Plugin<Project> {
    def static final DIR_JAVA = "java"
    def static final DIR_LAYOUT = "layout"
    def static final DIR_RES = "res"
    def static final DIR_VALUES = "values"
    def static final TASK_NAME = "cleanerTask"

    @Override
    void apply(Project project) {
        println("<<<<<<<< CleanerPlugin >>>>>>>>>")
        project.afterEvaluate {
            if (project.getPlugins().hasPlugin(AppPlugin.class)) {
                project.getExtensions().findByType(AppExtension.class).getApplicationVariants().each {
                    setup(project, it);
                }
            }
        }
    }

    def static setup(Project project, BaseVariant variant) {
        try {
            def mergeResourcesTask = variant.getMergeResources()
            def inputResourceSets = mergeResourcesTask.getInputResourceSets();
            String resSetName = "${variant.getName()}${TASK_NAME.capitalize()}".toString()
            ResourceSet.class.getConstructors().each {
                println(it)
            }
            def resourceSet = new ResourceSet(resSetName, true)//constructor differs once ran from android studio
            resourceSet.addSource(new File(project.getBuildDir(), getXmlBasePath(variant)))
            inputResourceSets.add(resourceSet)
            mergeResourcesTask.setInputResourceSets(inputResourceSets)
            mergeResourcesTask.dependsOn createGenerifyTask(project, variant)
        } catch (Exception e) {
            e.printStackTrace()
        }
    }

    def static Task createGenerifyTask(Project project, BaseVariant variant) {
        def sOutputDir = new File(project.getBuildDir(), getJavaPath(variant))
        def task = project.task("${TASK_NAME.toLowerCase()}${variant.getName().capitalize()}", type: CleanerTask) {
            sourceOutputDir = sOutputDir
            layoutsOutputDir = new File(project.getBuildDir(), getXmlFolderPath(variant, DIR_LAYOUT))
            valuesOutputDir = new File(project.getBuildDir(), getXmlFolderPath(variant, DIR_VALUES))
            resInputDirs = getResDirectories(project, variant)
            packageName = project.android.defaultConfig.applicationId
            logger = project.logger
        }

        variant.registerJavaGeneratingTask(task, sOutputDir)
        variant.addJavaSourceFoldersToModel(sOutputDir)

        return task
    }

    def static FileCollection getResDirectories(Project project, BaseVariant variant) {
        project.files(variant.sourceSets*.resDirectories.flatten())
    }

    def static String getXmlBasePath(BaseVariant variant) {
        return "cleaner/${variant.getName()}/${DIR_RES}"
    }

    def static String getXmlFolderPath(BaseVariant variant, String folder) {
        return "${getXmlBasePath(variant)}/${folder}"
    }

    def static String getJavaPath(BaseVariant variant) {
        return "generated/source/cleaner/${variant.getName()}/${DIR_JAVA}"
    }
}
