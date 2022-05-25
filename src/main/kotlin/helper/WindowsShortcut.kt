package helper

import java.io.*
import java.nio.charset.StandardCharsets
import java.text.ParseException
import java.util.*
import kotlin.experimental.and


/**
 * Represents a Windows shortcut (typically visible to Java only as a '.lnk' file).
 *
 * Retrieved 2011-09-23 from http://stackoverflow.com/questions/309495/windows-shortcut-lnk-parser-in-java/672775#672775
 * Originally called LnkParser
 *
 * Written by: (the stack overflow users, obviously!)
 * Apache Commons VFS dependency removed by crysxd (why were we using that!?) https://github.com/crysxd
 * Headerified, refactored and commented by Code Bling http://stackoverflow.com/users/675721/code-bling
 * Network file support added by Stefan Cordes http://stackoverflow.com/users/81330/stefan-cordes
 * Adapted by Sam Brightman http://stackoverflow.com/users/2492/sam-brightman
 * Support for additional strings (description, relative_path, working_directory, command_line_arguments) added by Max Vollmer https://stackoverflow.com/users/9199167/max-vollmer
 * Based on information in 'The Windows Shortcut File Format' by Jesse Hager &lt;jessehager@iname.com&gt;
 * And somewhat based on code from the book 'Swing Hacks: Tips and Tools for Killer GUIs'
 * by Joshua Marinacci and Chris Adamson
 * ISBN: 0-596-00907-0
 * http://www.oreilly.com/catalog/swinghks/
 */
class WindowsShortcut(file: File?) {
    /**
     * Tests if the shortcut points to a directory.
     * @return true if the 'directory' bit is set in this shortcut, false otherwise
     */
    var isDirectory = false
        private set

    /**
     * Tests if the shortcut points to a local resource.
     * @return true if the 'local' bit is set in this shortcut, false otherwise
     */
    var isLocal = false
        private set

    /**
     * @return the name of the filesystem object pointed to by this shortcut
     */
    var realFilename: String? = null
        private set

    /**
     * @return a description for this shortcut, or null if no description is set
     */
    var description: String? = null
        private set

    /**
     * @return the relative path for the filesystem object pointed to by this shortcut, or null if no relative path is set
     */
    var relativePath: String? = null
        private set

    /**
     * @return the working directory in which the filesystem object pointed to by this shortcut should be executed, or null if no working directory is set
     */
    var workingDirectory: String? = null
        private set

    /**
     * @return the command line arguments that should be used when executing the filesystem object pointed to by this shortcut, or null if no command line arguments are present
     */
    var commandLineArguments: String? = null
        private set

    /**
     * Gobbles up link data by parsing it and storing info in member fields
     * @param link all the bytes from the .lnk file
     */
    @Throws(ParseException::class)
    private fun parseLink(link: ByteArray) {
        try {
            if (!isMagicPresent(link)) throw ParseException("Invalid shortcut; magic is missing", 0)

            // get the flags byte
            val flags = link[0x14]

            // get the file attributes byte
            val file_atts_offset = 0x18
            val file_atts = link[file_atts_offset]
            val is_dir_mask = 0x10.toByte()
            isDirectory = file_atts and is_dir_mask > 0

            // if the shell settings are present, skip them
            val shell_offset = 0x4c
            val has_shell_mask = 0x01.toByte()
            var shell_len = 0
            if (flags and has_shell_mask > 0) {
                // the plus 2 accounts for the length marker itself
                shell_len = bytesToWord(link, shell_offset) + 2
            }

            // get to the file settings
            val file_start = 0x4c + shell_len
            val file_location_info_flag_offset_offset = 0x08
            val file_location_info_flag = link[file_start + file_location_info_flag_offset_offset].toInt()
            isLocal = file_location_info_flag and 2 == 0
            // get the local volume and local system values
            //final int localVolumeTable_offset_offset = 0x0C;
            val basename_offset_offset = 0x10
            val networkVolumeTable_offset_offset = 0x14
            val finalname_offset_offset = 0x18
            val finalname_offset = link[file_start + finalname_offset_offset] + file_start
            val finalname = getNullDelimitedString(link, finalname_offset)
            if (isLocal) {
                val basename_offset = link[file_start + basename_offset_offset] + file_start
                val basename = getNullDelimitedString(link, basename_offset)
                realFilename = basename + finalname
            } else {
                val networkVolumeTable_offset = link[file_start + networkVolumeTable_offset_offset] + file_start
                val shareName_offset_offset = 0x08
                val shareName_offset = (link[networkVolumeTable_offset + shareName_offset_offset]
                        + networkVolumeTable_offset)
                val shareName = getNullDelimitedString(link, shareName_offset)
                realFilename = shareName + "\\" + finalname
            }

            // parse additional strings coming after file location
            val file_location_size = bytesToDword(link, file_start)
            var next_string_start = file_start + file_location_size
            val has_description = 4.toByte()
            val has_relative_path = 8.toByte()
            val has_working_directory = 16.toByte()
            val has_command_line_arguments = 32.toByte()

            // if description is present, parse it
            if (flags and has_description > 0) {
                val string_len = bytesToWord(link, next_string_start) * 2 // times 2 because UTF-16
                description = getUTF16String(link, next_string_start + 2, string_len)
                next_string_start = next_string_start + string_len + 2
            }

            // if relative path is present, parse it
            if (flags and has_relative_path > 0) {
                val string_len = bytesToWord(link, next_string_start) * 2 // times 2 because UTF-16
                relativePath = getUTF16String(link, next_string_start + 2, string_len)
                next_string_start = next_string_start + string_len + 2
            }

            // if working directory is present, parse it
            if (flags and has_working_directory > 0) {
                val string_len = bytesToWord(link, next_string_start) * 2 // times 2 because UTF-16
                workingDirectory = getUTF16String(link, next_string_start + 2, string_len)
                next_string_start = next_string_start + string_len + 2
            }

            // if command line arguments are present, parse them
            if (flags and has_command_line_arguments > 0) {
                val string_len = bytesToWord(link, next_string_start) * 2 // times 2 because UTF-16
                commandLineArguments = getUTF16String(link, next_string_start + 2, string_len)
                next_string_start = next_string_start + string_len + 2
            }
        } catch (e: ArrayIndexOutOfBoundsException) {
            throw ParseException("Could not be parsed, probably not a valid WindowsShortcut", 0)
        }
    }

    companion object {
        /**
         * Provides a quick test to see if this could be a valid link !
         * If you try to instantiate a new WindowShortcut and the link is not valid,
         * Exceptions may be thrown and Exceptions are extremely slow to generate,
         * therefore any code needing to loop through several files should first check this.
         *
         * @param file the potential link
         * @return true if may be a link, false otherwise
         * @throws IOException if an IOException is thrown while reading from the file
         */
        @Throws(IOException::class)
        fun isPotentialValidLink(file: File): Boolean {
            val minimum_length = 0x64
            val fis: InputStream = FileInputStream(file)
            var isPotentiallyValid = false
            isPotentiallyValid = try {
                (file.isFile
                        && file.name.lowercase(Locale.getDefault()).endsWith(".lnk")
                        && fis.available() >= minimum_length && isMagicPresent(getBytes(fis, 32)))
            } finally {
                fis.close()
            }
            return isPotentiallyValid
        }

        /**
         * Gets all the bytes from an InputStream
         * @param in the InputStream from which to read bytes
         * @return array of all the bytes contained in 'in'
         * @throws IOException if an IOException is encountered while reading the data from the InputStream
         */
        @Throws(IOException::class)
        private fun getBytes(`in`: InputStream): ByteArray {
            return getBytes(`in`, null)
        }

        /**
         * Gets up to max bytes from an InputStream
         * @param in the InputStream from which to read bytes
         * @param max maximum number of bytes to read
         * @return array of all the bytes contained in 'in'
         * @throws IOException if an IOException is encountered while reading the data from the InputStream
         */
        @Throws(IOException::class)
        private fun getBytes(`in`: InputStream, max: Int?): ByteArray {
            // read the entire file into a byte buffer
            var max = max
            val bout = ByteArrayOutputStream()
            val buff = ByteArray(256)
            while (max == null || max > 0) {
                val n = `in`.read(buff)
                if (n == -1) {
                    break
                }
                bout.write(buff, 0, n)
                if (max != null) max -= n
            }
            `in`.close()
            return bout.toByteArray()
        }

        private fun isMagicPresent(link: ByteArray): Boolean {
            val magic = 0x0000004C
            val magic_offset = 0x00
            return link.size >= 32 && bytesToDword(link, magic_offset) == magic
        }

        private fun getNullDelimitedString(bytes: ByteArray, off: Int): String {
            var len = 0
            // count bytes until the null character (0)
            while (true) {
                if (bytes.get(off + len) == 0.toByte()) {
                    break
                }
                len++
            }
            return String(bytes, off, len)
        }

        private fun getUTF16String(bytes: ByteArray, off: Int, len: Int): String {
            return String(bytes, off, len, StandardCharsets.UTF_16LE)
        }

        /*
     * convert two bytes into a short note, this is little endian because it's
     * for an Intel only OS.
     */
        private fun bytesToWord(bytes: ByteArray, off: Int): Int {
            return bytes[off + 1].toInt() and 0xff shl 8 or (bytes[off].toInt() and 0xff)
        }

        private fun bytesToDword(bytes: ByteArray, off: Int): Int {
            return bytesToWord(bytes, off + 2) shl 16 or bytesToWord(bytes, off)
        }
    }

    init {
        val `in`: InputStream = FileInputStream(file)
        try {
            parseLink(getBytes(`in`))
        } finally {
            `in`.close()
        }
    }
}
