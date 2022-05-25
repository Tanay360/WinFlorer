package db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.filechooser.FileSystemView

class ArrayDB(destFolder: String) {
    private val keyFolder = File(FileSystemView.getFileSystemView().defaultDirectory.path, destFolder)

    init {
        if (!keyFolder.exists()) {
            keyFolder.mkdir()
        }
    }

    suspend fun insertItem(key: String, value: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            val file = File(keyFolder, "$key.txt")
            if (!file.exists()) {
                file.createNewFile()
            }
            val items = file.readText().trim().split("\n").filter { it.isNotBlank() }.toMutableSet()
            items.add(value)
            file.writeText(items.joinToString("\n"))
        }.getOrNull() != null
    }

    suspend fun removeItem(key: String, value: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            val file = File(keyFolder, "$key.txt")
            if (!file.exists()) {
                return@runCatching false
            }
            val items = file.readText().trim().split("\n").filter { it.isNotBlank() }.toMutableSet()
            if (items.contains(value)) {
                items.remove(value)
                file.writeText(items.joinToString("\n"))
                return@runCatching true
            }
            return@runCatching false
        }.getOrDefault(false)
    }

    suspend fun getAllItems(key: String): Array<String> = withContext(Dispatchers.IO) {
        val file = File(keyFolder, "$key.txt")
        if (!file.exists()) {
            return@withContext arrayOf()
        }
        val arr = file.readText().trim().split("\n").filter { it.isNotBlank() }.toMutableSet()
        var flags = false
        arr.forEach { str ->
            if (File(str).run { !exists() || !isDirectory }) {
                arr.remove(str)
                flags = true
            }
        }
        if (flags) {
            file.writeText(arr.joinToString("\n"))
        }
        arr.toTypedArray()
    }

    suspend fun removeAllItems(key: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            val file = File(keyFolder, "$key.txt")
            if (file.exists()) {
                file.delete()
            }
        }.getOrNull() != null
    }
}