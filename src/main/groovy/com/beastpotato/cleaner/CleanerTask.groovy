package com.beastpotato.cleaner

import groovy.io.FileType
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Created by Oleksiy on 9/13/2016.
 */
class CleanerTask extends DefaultTask {
    // Output directory to generate Java files into
    @OutputDirectory
    File sourceOutputDir

    // Output directory to generate XML layout files into
    @OutputDirectory
    File layoutsOutputDir

    // Output directory to generate XML value files into
    @OutputDirectory
    File valuesOutputDir

    // Collection of input files to read XML resources from
    @InputFiles
    FileCollection resInputDirs

    // Fully qualified package name of the project
    @Input
    String packageName

    Logger logger

    @TaskAction
    void cleanerTask() {
        project.logger.info("Hello from CleanerTask")
        println("=============CleanerTask============")
        println("resource input directories:")
        resInputDirs.each {
            println(it.absolutePath)
        }
        println("source output directory: $sourceOutputDir")
        println("layout output directory: $layoutsOutputDir")
        println("values output directory: $valuesOutputDir")
        println("package name: $packageName")
        List<String> hardCodedStrings = processLayouts(resInputDirs)
        println("hardcoded string resources:")
        hardCodedStrings.each {
            println(it)
        }
        def cleanHardCodedStrings = new ArrayList<String>()
        hardCodedStrings.each {
            if (!cleanHardCodedStrings.contains(it))
                cleanHardCodedStrings.add(it)
        }
        println("generating strings.xml file...")
        generateStringResFile(cleanHardCodedStrings)
        println("cleaning up layouts...")
        replaceHardcodedInLayouts(resInputDirs, cleanHardCodedStrings)
        println("=============CleanerTask FINISHED============")
    }

    void replaceHardcodedInLayouts(FileCollection files, List<String> hardcodedString) {
        files.each {
            File layoutFile = new File("${it}/layout")
            if (layoutFile.exists()) {
                it.eachFileRecurse(FileType.FILES, { file ->
                    println(file.name)
                    if (file.name.endsWith(".xml"))
                        replaceHardcodedInLayout(file, hardcodedString)
                })
            }
        }
    }

    void replaceHardcodedInLayout(File f, List<String> hardcodedString) {
        println(f)
        Node node = new XmlParser().parse(f)
        replaceHardcodedInNode(node, hardcodedString)
        def fileName = f.getAbsoluteFile().getName()
        def file = new File("${layoutsOutputDir}/${fileName}")
        file.createNewFile()
        file.withWriter { writer -> new XmlNodePrinter(new PrintWriter(writer)).print(node) }
        println("done")
    }

    def replaceHardcodedInNode(Node node, List<String> hardcodedStrings) {
        try {
            def attrs = node.attributes()
            attrs.findAll {
                (it.getKey() as String).contains("{http://schemas.android.com/apk/res/android}text") && !it.getValue().toString().contains("@string/")
            }.each {
                if (hardcodedStrings.contains(it.value.toString())) {
                    println(it)
                    attrs.remove(it.key)
                    attrs.put("android:text", "@string/cln_str_${hardcodedStrings.indexOf(it.value.toString())}")
                }
            }
            node.children().each { Node it ->
                replaceHardcodedInNode(it, hardcodedStrings)
            }
        } catch (Exception e) {
            e.printStackTrace()
        }
    }

    void generateStringResFile(List<String> strings) {
        def stringRes = new File("${valuesOutputDir}/strings.xml")
        StringBuilder stringComp = new StringBuilder()
        strings.eachWithIndex { it, i ->
            String line = "<string name=\"cln_str_${i}\">${it}</string>\n"
            println(line)
            stringComp.append(line)
        }
        stringRes.withWriter { writer ->
            writer.write("""
<resources>
    ${stringComp}
</resources>
            """)
        }
    }

    List<String> processLayouts(FileCollection files) {
        List<String> hardcodedStrings = new ArrayList<>()
        files.each {
            File file = new File("${it}/layout")
            if (file.exists()) {
                file.eachFileRecurse(FileType.FILES, {
                    hardcodedStrings.addAll(processLayout(it))
                })
            }
        }
        return hardcodedStrings
    }

    List<String> processLayout(File file) {
        Node node = new XmlParser().parse(file)
        return processNode(node)
    }

    List<String> processNode(Node node) {
        List<String> hcRes = node.attributes().findAll {
            (it.getKey() as String).contains("{http://schemas.android.com/apk/res/android}text") && !it.getValue().toString().contains("@string/")
        }.collect {
            it.value.toString()
        }
        node.children().each { Node child -> hcRes.addAll(processNode(child)) }
        return hcRes
    }
}
