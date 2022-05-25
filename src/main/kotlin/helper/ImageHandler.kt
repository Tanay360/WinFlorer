package helper

import java.awt.GraphicsEnvironment
import java.awt.Image
import java.awt.image.BufferedImage
import javax.swing.Icon
import javax.swing.ImageIcon

fun Icon.toImage(): Image = run {
    if (this is ImageIcon) {
        return image
    } else {
        val w = iconWidth
        val h = iconHeight
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val gd = ge.defaultScreenDevice
        val gc = gd.defaultConfiguration
        val image = gc.createCompatibleImage(w, h)
        val g = image.createGraphics()
        paintIcon(null, g, 0, 0)
        g.dispose()
        return image
    }
}

fun Image.toBufferedImage(): BufferedImage = run {
    if (this is BufferedImage) {
        return this
    } else {
        val image = BufferedImage(getWidth(null), getHeight(null), BufferedImage.TYPE_INT_ARGB)
        val b2gr = image.createGraphics()
        b2gr.drawImage(this, 0, 0, null)
        b2gr.dispose()
        return image
    }
}
