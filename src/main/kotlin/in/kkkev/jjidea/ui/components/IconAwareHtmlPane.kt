package `in`.kkkev.jjidea.ui.components

import com.intellij.icons.AllIcons
import com.intellij.ui.ColorUtil
import com.intellij.ui.components.JBHtmlPane
import com.intellij.ui.components.JBHtmlPaneConfiguration
import com.intellij.ui.components.JBHtmlPaneStyleConfiguration
import com.intellij.util.ui.UIUtil
import com.intellij.vcsUtil.VcsUtil
import `in`.kkkev.jjidea.jj.ChangeId
import `in`.kkkev.jjidea.jj.invalidate
import `in`.kkkev.jjidea.ui.common.JujutsuIcons
import `in`.kkkev.jjidea.vcs.jujutsuRepository
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.util.regex.Pattern
import javax.swing.Icon
import javax.swing.event.HyperlinkEvent
import kotlin.reflect.KClass

private val CHANGE_ID_URL_PARSER = Pattern.compile("^jjc://([^?]+)\\?(.+)$")

/**
 * An HTML pane that can resolve icons from a set of icon libraries, including IDEA's icons
 * [com.intellij.icons.AllIcons].
 */
class IconAwareHtmlPane : JBHtmlPane(
    JBHtmlPaneStyleConfiguration(),
    JBHtmlPaneConfiguration { iconResolver = IconResolver::resolveIcon }
) {
    init {
        isOpaque = false
        addHyperlinkListener { e ->
            if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                with(CHANGE_ID_URL_PARSER.matcher(e.description)) {
                    if (matches()) {
                        VcsUtil.getFilePath(this.group(1), true)
                            .jujutsuRepository
                            .invalidate(ChangeId(this.group(2)))
                    }
                }
            }
        }
    }
}

object IconResolver {
    val icons = listOf(
        AllIcons::class,
        JujutsuIcons::class
    ).map { it.allIcons }.fold(emptyMap<String, Icon>()) { a, i -> a + i }

    fun resolveIcon(key: String): Icon? {
        val parts = key.split("#", limit = 2)
        val icon = icons[parts[0]] ?: return null
        val hexColor = parts.getOrNull(1) ?: return icon
        return ColoredIcon(icon, ColorUtil.fromHex(hexColor))
    }

    private val KClass<*>.allIcons
        get() = java
            .nestMembers
            .flatMap { it.fields.toList() }
            .filter { it.type.isAssignableFrom(Icon::class.java) }
            .associate {
                "${it.declaringClass.name.replace(qualifiedName!!, simpleName!!).replace('$', '.')}.${it.name}" to
                    (it.get(it.declaringClass) as Icon)
            }
}

/**
 * An icon that recolors a source icon to a target color using HSB recoloring.
 * Preserves transparency and relative brightness of the source icon.
 * Caches the rendered result by scale factor for HiDPI support.
 */
private class ColoredIcon(private val source: Icon, private val color: Color) : Icon {
    @Volatile
    private var cached: Pair<Double, BufferedImage>? = null

    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        val g2 = g as Graphics2D
        val scale = g2.transform.scaleX
        val entry = cached
        val img = if (entry != null && entry.first == scale) {
            entry.second
        } else {
            BufferedImage(iconWidth, iconHeight, BufferedImage.TYPE_INT_ARGB).also {
                val ig = it.createGraphics()
                source.paintIcon(c, ig, 0, 0)
                ig.dispose()
                applyHsbRecolor(it, color)
                cached = scale to it
            }
        }
        UIUtil.drawImage(g, img, x, y, null)
    }

    override fun getIconWidth() = source.iconWidth
    override fun getIconHeight() = source.iconHeight
}

private fun applyHsbRecolor(img: BufferedImage, color: Color) {
    val base = Color.RGBtoHSB(color.red, color.green, color.blue, null)
    for (y in 0 until img.height) {
        for (x in 0 until img.width) {
            val rgba = img.getRGB(x, y)
            if ((rgba ushr 24) == 0) continue
            val hsb = Color.RGBtoHSB((rgba shr 16) and 0xff, (rgba shr 8) and 0xff, rgba and 0xff, null)
            val rgb = Color.HSBtoRGB(base[0], base[1] * hsb[1], base[2] * hsb[2])
            img.setRGB(x, y, (rgba and 0xFF000000.toInt()) or (rgb and 0xFFFFFF))
        }
    }
}
