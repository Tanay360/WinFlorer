package helper

import java.text.CharacterIterator
import java.text.StringCharacterIterator


fun humanReadableByteCountSI(bytes: Long): String {
    var byteLong = bytes
    if (-1000 < byteLong && byteLong < 1000) {
        return "$byteLong B"
    }
    val ci: CharacterIterator = StringCharacterIterator("kMGTPE")
    while (byteLong <= -999950 || byteLong >= 999950) {
        byteLong /= 1000
        ci.next()
    }
    return String.format("%.1f %cB", byteLong / 1000.0, ci.current())
}

