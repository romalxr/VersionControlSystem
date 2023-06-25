package svcs

import java.io.File
import kotlin.io.path.Path

const val FOLDER_NAME = "vcs"
const val COMMITS_FOLDER_NAME = "vcs/commits"
const val CONFIG_NAME = "config.txt"
const val INDEX_NAME = "index.txt"
const val LOG_NAME = "log.txt"

fun main(args: Array<String>) {
    init()
    if (args.isEmpty()) {
        showHelp()
        return
    }
    when(val command = args.first().lowercase().trim()) {
        "config"    -> doConfig(args)
        "add"       -> doAdd(args)
        "log"       -> doLog()
        "commit"    -> doCommit(args)
        "checkout"  -> doCheckout(args)
        "--help"    -> showHelp()
        else        -> println("'$command' is not a SVCS command.")
    }
}

fun init() {
    File(FOLDER_NAME).mkdir()
    File(COMMITS_FOLDER_NAME).mkdir()
}

fun doCheckout(args: Array<String>) {
    if (args.size == 1) {
        println("Commit id was not passed.")
    } else {
        val commitDir = File("$COMMITS_FOLDER_NAME/${args[1]}")
        if (commitDir.exists()) {
            commitDir.listFiles()
                ?.forEach { it.copyTo(File(it.name), overwrite = true) }
            println("Switched to commit ${args[1]}.")
        } else {
            println("Commit does not exist.")
        }
    }
}

fun doCommit(args: Array<String>) {
    if (args.size == 1) {
        println("Message was not passed.")
    } else if (buildID() == lastCommitID()) {
        println("Nothing to commit.")
    } else {
        val commitID = buildID()
        val commitDir = File("$COMMITS_FOLDER_NAME/$commitID")
        commitDir.mkdir()
        indexFile().readLines().forEach {
            File(it).copyTo(File("$commitDir/$it"))
        }

        val fileLog = logFile()
        fileLog.readText().run {
            fileLog.writeText("${commitInfo(commitID, args[1])}\n$this")
        }
        println("Changes are committed.")
    }
}

fun doLog() {
    val logFile = logFile()
    val log = logFile.readText()
    if (log.isBlank()) println("No commits yet.")
    else println(log)
}

fun doConfig(args: Array<String>) {
    val configFile = configFile()
    if (args.size > 1) {
        configFile.writeText(args[1])
        val userName = configFile.readText()
        println("The username is $userName.")
    } else {
        val userName = configFile.readText()
        if (userName.isBlank()) println("Please, tell me who you are.")
        else println("The username is $userName.")
    }
}

fun doAdd(args: Array<String>) {
    val indexFile = indexFile()
    if (args.size > 1) {
        if (File(args[1]).exists()) {
            val indexes = indexFile.readLines().toMutableList()
            indexes.add(args[1])
            indexFile.writeText(indexes.joinToString("\n"))
            println("The file '${args[1]}' is tracked.")
        } else println("Can't find '${args[1]}'.")
    } else {
        val indexContent = indexFile.readText()
        if (indexContent.isBlank()) println("Add a file to the index.")
        else println("Tracked files:\n$indexContent")
    }
}

fun buildID(): String {
    val indexFile = indexFile()
    val indexFileContent = indexFile.readLines()
    if (indexFileContent.isEmpty()) {
        return ""
    }
    return indexFileContent
        .joinToString { File(it).readText() }
        .hashCode()
        .toString()
        .replace("-","")
}

fun lastCommitID(): String {
    val logFile = logFile()
    logFile.readLines().run {
        return if (this.isEmpty()) "" else this.first().substringAfter(' ')
    }
}

fun commitInfo(hashID: String, message: String): String {
    val fileConfig = configFile()
    return """
            commit $hashID
            Author: ${fileConfig.readText()}
            $message
            
        """.trimIndent()
}

private fun logFile(): File {
    val logFile = Path(FOLDER_NAME, LOG_NAME).toFile()
    if (!logFile.exists()) logFile.createNewFile()
    return logFile
}

private fun configFile(): File {
    val fileConfig = Path(FOLDER_NAME, CONFIG_NAME).toFile()
    if (!fileConfig.exists()) fileConfig.createNewFile()
    return fileConfig
}

private fun indexFile(): File {
    val indexFile = Path(FOLDER_NAME, INDEX_NAME).toFile()
    if (!indexFile.exists()) indexFile.createNewFile()
    return indexFile
}

fun showHelp() {
    println("""
        These are SVCS commands:
        config     Get and set a username.
        add        Add a file to the index.
        log        Show commit logs.
        commit     Save changes.
        checkout   Restore a file.
    """.trimIndent())
}
