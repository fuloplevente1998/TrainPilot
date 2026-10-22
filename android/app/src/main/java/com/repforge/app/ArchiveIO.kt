package __PACKAGE_NAME__

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Biztonságos ZIP fájlkezelő modul.
 * Zip Slip elleni védelemmel és pufferelt I/O műveletekkel.
 */
object ArchiveIO {
    private const val BUFFER_SIZE = 8192

    @Throws(IOException::class)
    fun zipDirectory(sourceDir: File, zipFile: File) {
        val files = mutableListOf<Pair<String, File>>()
        collectFiles(sourceDir, "", files)

        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            for ((relativePath, file) in files) {
                val entry = ZipEntry(relativePath)
                entry.time = file.lastModified()
                zos.putNextEntry(entry)
                FileInputStream(file).use { fis ->
                    fis.copyTo(zos, BUFFER_SIZE)
                }
                zos.closeEntry()
            }
        }
    }

    private fun collectFiles(dir: File, currentPrefix: String, result: MutableList<Pair<String, File>>) {
        val list = dir.listFiles() ?: return
        for (f in list) {
            val entryPath = if (currentPrefix.isEmpty()) f.name else "$currentPrefix/${f.name}"
            if (f.isDirectory) {
                collectFiles(f, entryPath, result)
            } else {
                result.add(entryPath to f)
            }
        }
    }

    @Throws(IOException::class)
    fun unzip(zipFile: File, targetDir: File) {
        if (!targetDir.exists()) targetDir.mkdirs()
        val canonicalDestDirPath = targetDir.canonicalPath

        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val newFile = File(targetDir, entry.name)
                val canonicalDestPath = newFile.canonicalPath
                if (!canonicalDestPath.startsWith(canonicalDestDirPath + File.separator) && canonicalDestPath != canonicalDestDirPath) {
                    throw SecurityException("Illegális zip útvonal (Zip Slip kísérlet észlelve): ${entry.name}")
                }

                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos, BUFFER_SIZE)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
