package han.cirno.v1.render

import han.cirno.v1.RenderResult
import han.cirno.v1.ReturnSongInfo
import han.cirno.v1.beans.SongInfo
import han.cirno.v1.render.`object`.JudgeLine
import han.cirno.v1.render.`object`.JudgeLine.Companion
import han.cirno.v1.render.phi_chart.CirnoPhiChart
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import kotlin.random.Random


fun render(
    songInfo: SongInfo,
    difficulty: CirnoPhiChart.Difficulty,
    timeInSecond:Float?,
    showCombo:Boolean=true,
    showScore:Boolean=true,
    showDifficulty: Boolean=true,
    showChartName:Boolean=true,
    showHitEffect:Boolean=true,
    showCopyright:Boolean=true,
    showIllustration:Boolean=true,
): RenderResult {
    val targetDifficulty = when(difficulty){
        CirnoPhiChart.Difficulty.EZ->songInfo.difficulties.ezDifficulty
        CirnoPhiChart.Difficulty.HD->songInfo.difficulties.hdDifficulty
        CirnoPhiChart.Difficulty.IN->songInfo.difficulties.inDifficulty
        CirnoPhiChart.Difficulty.AT->songInfo.difficulties.atDifficulty
    }
    if (targetDifficulty==0f)
        return RenderResult.Failed(
            RenderResult.ERROR_DIFFICULTY_NOT_FOUND,
            "No such difficulty: ${songInfo.id}/${difficulty.name}")
    val chartFile=File(songInfo.chartPath,difficulty.name+".json")
    if (!chartFile.exists())
        return RenderResult.Failed(
            RenderResult.ERROR_DIFFICULTY_NOT_FOUND,
            "No such difficulty: ${songInfo.id}/${difficulty.name}")
    val cirnoPhiChart=try{CirnoPhiChart(chartFile)}catch (e:Exception){
        return RenderResult.Failed(
            RenderResult.ERROR_CHART_RESOLVE_FAILED,
            "Chart file resolution error")
    }
    val judgeLines=cirnoPhiChart.judgeLines.map { JudgeLine(it) }
    val bufferedImage= BufferedImage(1920,1080, BufferedImage.TYPE_INT_RGB)
    val worldTimeInSecond=timeInSecond?:(maxSecond(judgeLines)*Random.nextFloat())
    judgeLines.forEach { it.update(worldTimeInSecond) }
    val graphics2D = bufferedImage.createGraphics()
    graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    graphics2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT)
    if (showIllustration){
        val illustrator = ImageIO.read(songInfo.illustrationPath)
        graphics2D.drawImage(illustrator,
            0,0,1920,1080,null)
        graphics2D.color=Color(0f,0f,0f,0.85f)
        graphics2D.fillRect(0,0,1920,1080)
        graphics2D.color=Color(1f,1f,1f,1f)
    }
    judgeLines.forEach {it.drawLine(graphics2D) }
    judgeLines.forEach {it.drawNotesPhase1(graphics2D, worldTimeInSecond) }
    judgeLines.forEach {it.drawNotesPhase2(graphics2D) }
    if (showHitEffect){
        val timeRange=worldTimeInSecond-0.5f..worldTimeInSecond
        judgeLines.flatMap {
            it.simulateAndGetClosing(timeRange)
        }.forEach {
            val (time,positions)=it
            val timePercent=1f-(worldTimeInSecond-time)/0.5f
            val currentFrameNum = timePercent * JudgeLine.HIT_EFFECT_COUNT
            val currentFrame= JudgeLine.hitEffectPerfect[currentFrameNum.toInt()]
            val frameWidth=currentFrame.width
            val frameHeight=currentFrame.height
            val scale=1.3f
            positions.forEach {position->
                val (x,y)=position
                graphics2D.draw(
                    currentFrame,
                    x,y,
                    0f,
                    frameWidth.toFloat()*scale,
                    frameHeight.toFloat()*scale,
                    center = true
                )
            }
        }
    }
    if (showCombo){
        val combo= calcCombo(worldTimeInSecond, judgeLines)
        run {
            if (combo<3)return@run
            graphics2D.drawFont(comboCountFont,combo.toString(),1920/2,100)
            graphics2D.drawFont(comboTextFont,"COMBO",1920/2,135)
        }
    }
    if (showScore){
        val score = calcScore(worldTimeInSecond, judgeLines)
        graphics2D.drawFont(scoreTextFont,score,1730,90)
    }
    if (showDifficulty){
        val difficultyCount=targetDifficulty.toInt()
        val difficultyName=difficulty.name
        graphics2D.drawFont(nameAndDifficultyTextFont,"$difficultyName Lv.$difficultyCount",1870,1045,2)
    }
    if (showChartName){
        graphics2D.drawFont(nameAndDifficultyTextFont,songInfo.name,50,1045,0)
    }
    if (showCopyright){
        graphics2D.color= Color(1f,1f,1f,0.5f)
        graphics2D.drawFont(comboTextFont,"nofyso/cirno-phi-chart-snapshot v0.1.0",1920/2,1070)
    }
    graphics2D.dispose()
    val bos=ByteArrayOutputStream()
    ImageIO.write(bufferedImage, "png", bos)
    return RenderResult.Success(
        RenderResult.SUCCESS,
        ReturnSongInfo(songInfo),
        worldTimeInSecond,
        Base64.getEncoder().encodeToString(bos.use { it.toByteArray() })
    )
}

private val rootFont by lazy {
    val resource = Companion::class.java.getResource("/static/font/font.ttf")!!
    resource.openStream().use { Font.createFont(Font.TRUETYPE_FONT,it )}
}
private val comboCountFont by lazy { rootFont.deriveFont(Font.PLAIN, 100f) }
private val comboTextFont by lazy { rootFont.deriveFont(Font.PLAIN, 30f) }
private val scoreTextFont by lazy { rootFont.deriveFont(Font.PLAIN, 70f) }
private val nameAndDifficultyTextFont by lazy { rootFont.deriveFont(Font.PLAIN, 40f) }

private fun Graphics2D.drawFont(font: Font,text:String,x:Int,y:Int,direction:Int=1){
    setFont(font)
    val textWidth = fontMetrics.stringWidth(text)
    val directionFix = when (direction) {
        0->0
        2-> -textWidth
        else -> -textWidth / 2
    }
    drawString(text, x+ directionFix, y)
}

private fun calcScore(worldTimeInSecond: Float, judgeLines: List<JudgeLine>): String {
    val combo = calcCombo(worldTimeInSecond, judgeLines)
    val maxCombo = calcMaxCombo(judgeLines)
    val score=1000000f*(combo/maxCombo.toFloat())
    return String.format("%07.0f", score)
}

private fun calcCombo(worldTimeInSecond:Float, judgeLines: List<JudgeLine>):Int=
    judgeLines.sumOf { it.getCombo(worldTimeInSecond) }

private fun calcMaxCombo(judgeLines: List<JudgeLine>):Int=
    judgeLines.sumOf { it.getNotes() }

private fun maxSecond(judgeLines: List<JudgeLine>):Float=
    judgeLines.maxOf { line->
        val secondsPerTick = line.secondsPerTick
        arrayOf(
            (line.notesAbove.lastOrNull()?.run { (time+holdTime).let { if(it>50000f)0f else it } })?:0f,
            (line.notesBelow.lastOrNull()?.run { (time+holdTime).let { if(it>50000f)0f else it } })?:0f,
            line.eventQueues.maxOf {queue-> ((queue.lastOrNull()?.endTime?.let { if(it>50000f)0f else it }))?:0f }
        ).max()*secondsPerTick
    }

fun Graphics2D.draw(
    overlayImage: BufferedImage,
    x:Float,
    y:Float,
    rotation:Float,
    width:Float,
    height:Float,
    center:Boolean=false
){
    val rotationAngle = Math.toRadians(rotation.toDouble())
    val pivotX: Double = width / 2.0
    val pivotY: Double = height / 2.0
    val transform = AffineTransform()
    transform.translate(0.0, 1080.0)
    transform.scale(1.0,-1.0)
    transform.translate(
        x.toDouble()-(if (!center)0.0 else width/2.0),
        y.toDouble()-(if (!center)0.0 else height/2.0))
    transform.rotate(rotationAngle, pivotX, pivotY)
    transform.scale((width/overlayImage.width).toDouble(), (height/overlayImage.height).toDouble())
    drawImage(overlayImage,transform,null)
}