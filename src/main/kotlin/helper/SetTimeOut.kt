package helper

import java.util.*

fun setTimeOut(timer: Long, timeout: () -> Unit) {
    Timer(UUID.randomUUID().toString(), false).schedule(object : TimerTask() {
        override fun run() {
            timeout()
        }
    }, timer)
}