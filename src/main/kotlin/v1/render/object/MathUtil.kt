package han.cirno.v1.render.`object`

import kotlin.math.cos
import kotlin.math.sin

object MathUtil {
    const val WIDTH=1920.0f
    const val HEIGHT=1080.0f

    /**
     * Fix a degree from 0 to 360
     * @param degree    angle, DEG
     * @return            Fixed degree
     */
    fun fixDegree(degree: Float): Float {
        return if (degree < 0) {
            if ((degree + 360) < 0) degree + 360 else fixDegree(
                degree + 360
            )
        } else if (degree > 360) {
            if ((degree - 360) < 360) degree - 360 else fixDegree(
                degree - 360
            )
        } else {
            degree
        }
    }

    /**
     * Get a point that vertically away from a line
     * @param lineX        lineX
     * @param lineY        lineY
     * @param degree    angle, DEG
     * @param distance    distance to line
     * @return            The point
     */
    fun getPosOutOfLine(lineX: Float, lineY: Float, degree: Float, distance: Float): FloatArray {
        val rad = Math.toRadians(degree.toDouble())
        val cos = cos(rad)
        val sin = sin(rad)
        val da = cos * distance
        val db = sin * distance
        return floatArrayOf(lineX + da.toFloat(), lineY + db.toFloat())
    }

}
