package han.cirno.v1.render.`object`

import han.cirno.v1.render.draw
import han.cirno.v1.render.phi_chart.CirnoPhiChart
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.math.max


class JudgeLine(
    judgeLine: CirnoPhiChart.JudgeLine) {
    val eventQueues:Array<Array<JudgeLineEvent>> =
        arrayOf(
            judgeLine.judgeLineMoveEvents.map { JudgeLineEvent(it) }.toTypedArray(),
            judgeLine.judgeLineRotateEvent.map { JudgeLineEvent(it) }.toTypedArray(),
            judgeLine.judgeLineDisappearEvent.map { JudgeLineEvent(it) }.toTypedArray(),
        )
    private val eventCursors:Array<Int> = arrayOf(0,0,0)
    private val speedEvents= judgeLine.speedEvents.toTypedArray()
    private val bpm=judgeLine.bpm
    val notesAbove=judgeLine.notesAbove
    val notesBelow=judgeLine.notesBelow
    val secondsPerTick=60f/bpm/32f

    private lateinit var phase1CacheLazy:List<Pair<CirnoPhiChart.Note,Boolean>>
    private lateinit var phase1CacheState:Triple<Float,Float,Float>

    private var x:Float=0f
    private var y:Float=0f
    private var rotation:Float=0f
    private var alpha:Float=0f

    companion object {
        private fun getNoteFile(name:String):BufferedImage=
            getFile("/static/note/$name")
        private fun getFile(path:String):BufferedImage{
            val resource = Companion::class.java.getResource(path)!!
            return ImageIO.read(resource)
        }
        private fun tint(
            image: BufferedImage,
            tintColorO:Color,
            intensity:Float=1.0f
        ):BufferedImage{
            val tintColor=tintColorO.rgb
            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    val pixel: Int = image.getRGB(x, y)
                    val a = (pixel shr 24) and 0xFF
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    val tr = (r * (1 - intensity) + ((tintColor shr 16) and 0xFF) * intensity).toInt()
                    val tg = (g * (1 - intensity) + ((tintColor shr 8) and 0xFF) * intensity).toInt()
                    val tb = (b * (1 - intensity) + (tintColor and 0xFF) * intensity).toInt()
                    image.setRGB(x, y, Color(tr, tg, tb,a).rgb)
                }
            }
            return image
        }
        val tapNoteTexture:BufferedImage by lazy { getNoteFile("tap.png") }
        val dragNoteTexture:BufferedImage by lazy { getNoteFile("drag.png") }
        val holdNoteHeadTexture:BufferedImage by lazy { getNoteFile("hold_head.png") }
        val holdNoteBodyTexture:BufferedImage by lazy { getNoteFile("hold_body.png") }
        val holdNoteEndTexture:BufferedImage by lazy { getNoteFile("hold_end.png") }
        val flickNoteTexture:BufferedImage by lazy { getNoteFile("flick.png") }
        val tapNoteHLTexture:BufferedImage by lazy { getNoteFile("tap_hl.png") }
        val dragNoteHLTexture:BufferedImage by lazy { getNoteFile("drag_hl.png") }
        val holdNoteHeadHLTexture:BufferedImage by lazy { getNoteFile("hold_head_hl.png") }
        val holdNoteBodyHLTexture:BufferedImage by lazy { getNoteFile("hold_body_hl.png") }
        val flickNoteHLTexture:BufferedImage by lazy { getNoteFile("flick_hl.png") }
        val line:BufferedImage by lazy { getFile("/static/line/line.png") }
        private val hitEffect:Array<BufferedImage> by lazy {
            Array(HIT_EFFECT_COUNT){
                getFile("/static/click/click$it.png")
            }
        }
        val hitEffectPerfect:Array<BufferedImage> by lazy {
            val color=Color(0xE2,0xD6,0x94,0xFF)
            Array(HIT_EFFECT_COUNT){
                tint(hitEffect[it],color)
            }
        }
        const val HIT_EFFECT_COUNT=30
        const val UNIT_WIDTH= MathUtil.WIDTH/18f
        const val UNIT_HEIGHT= MathUtil.HEIGHT*.6f
    }

    fun update(worldTimeInSecond:Float){
        val worldCurrentTick = worldTimeInSecond /secondsPerTick
        processProperty(0,worldCurrentTick){
            x=1920f*it.first
            y=1080f*it.second
        }
        processProperty(1,worldCurrentTick){
            rotation=it.first
        }
        processProperty(2,worldCurrentTick){
            alpha=it.first
        }
    }

    fun getCombo(worldTimeInSecond: Float):Int{
        val worldCurrentTick = worldTimeInSecond /secondsPerTick
        val query:(CirnoPhiChart.Note)->Int={
            if(it.time+it.holdTime<=worldCurrentTick)1
            else 0
        }
        return notesAbove.sumOf(query)+notesBelow.sumOf(query)
    }

    fun getNotes():Int=notesAbove.size+notesBelow.size

    /**
     * @return List<(timeInTick, List<Loc>)>
     */
    fun simulateAndGetClosing(
        timeRangeInSecond:ClosedRange<Float>,
        thresholdInSecond:Float=0.01f
    ):List<Pair<Float,List<Pair<Float,Float>>>>{
        val timeRangeInTick=
            (timeRangeInSecond.start/secondsPerTick).toInt()..(timeRangeInSecond.endInclusive/secondsPerTick).toInt()
        val filter:(CirnoPhiChart.Note)->Boolean={ it.time in timeRangeInTick }
        val targetTickArray=arrayOf(
            *notesAbove.filter(filter).distinctBy { it.time }.map{it.time}.toTypedArray(),
            *notesBelow.filter(filter).distinctBy { it.time }.map{it.time}.toTypedArray()
        )
        return targetTickArray.map {targetTick->
            val thresholdInTick=thresholdInSecond/secondsPerTick
            val range=(targetTick-thresholdInTick).toInt()..(targetTick+thresholdInTick).toInt()
            update(targetTick*secondsPerTick)
            fun query(notes:List<CirnoPhiChart.Note>)=
                notes.filter { it.time in range }.map { getFootPos(it.positionX).let { n->n[0] to n[1] } }.toTypedArray()
            targetTick*secondsPerTick to listOf(*query(notesAbove),*query(notesBelow))
        }
    }

    fun drawLine(graphics2D: Graphics2D) {
        graphics2D.composite = AlphaComposite.SrcOver.derive(alpha.coerceIn(0f..1f))
        graphics2D.draw(line,x,y,MathUtil.fixDegree(rotation), line.width.toFloat(), line.height.toFloat(),true)
        graphics2D.color = Color.white
        graphics2D.composite = AlphaComposite.SrcOver.derive(1f)
    }

    fun drawNotesPhase1(graphics2D: Graphics2D, worldTimeInSecond: Float) {
        val worldCurrentTick = worldTimeInSecond /secondsPerTick
        val lineY=getLineY(worldCurrentTick)
        val scale=0.25f
        fun drawHoldNote(note: CirnoPhiChart.Note,above:Boolean){
            val headTexture=if (note.isHighLighted) holdNoteHeadHLTexture else holdNoteHeadTexture
            val bodyTexture=if (note.isHighLighted) holdNoteBodyHLTexture else holdNoteBodyTexture
            val endTexture= holdNoteEndTexture
            val headPosition = note.floorPosition - lineY
            val bodyHeight=note.holdTime*note.speed*secondsPerTick- max(0f,-headPosition)
            val bodyPosition = note.floorPosition+bodyHeight/2f-lineY+ max(0f,-headPosition)
            if (bodyPosition<=-bodyHeight/2f)return
            val headAndBodyWidth=headTexture.width
            val endWidthRaw=endTexture.width.toFloat()
            val endHeightRaw=endTexture.height.toFloat()
            val headHeight=headTexture.height
            val tempPos= getFootPos(note.positionX)
            val headPos= MathUtil.getPosOutOfLine(
                tempPos[0],tempPos[1],
                MathUtil.fixDegree(rotation+if(above)90f else -90f),
                headPosition * UNIT_HEIGHT
            )
            val bodyPos= MathUtil.getPosOutOfLine(
                tempPos[0],tempPos[1],
                MathUtil.fixDegree(rotation+if(above)90f else -90f),
                bodyPosition * UNIT_HEIGHT +if(bodyPosition<=0)headHeight*scale else 0f
            )
            val endPos= MathUtil.getPosOutOfLine(
                tempPos[0],tempPos[1],
                MathUtil.fixDegree(rotation+if(above)90f else -90f),
                (bodyPosition+bodyHeight/2f) * UNIT_HEIGHT
                    +if(bodyPosition<=0)headHeight*scale/2f else 0f
                    +endHeightRaw*scale/2f
            )
            if (note.time>worldCurrentTick){
                graphics2D.draw(
                    headTexture,
                    headPos[0]-headAndBodyWidth*scale/2f,
                    headPos[1]-headHeight*scale/2f,
                    MathUtil.fixDegree(rotation+if (!above)0f else 180f),
                    headAndBodyWidth*scale,
                    headHeight*scale,
                )
            }
            val bodyHeightRaw=bodyHeight* UNIT_HEIGHT
            graphics2D.draw(
                bodyTexture,
                bodyPos[0]-headAndBodyWidth*scale/2f,
                bodyPos[1]-bodyHeightRaw/2f,
                MathUtil.fixDegree(rotation+if (!above)0f else 180f),
                headAndBodyWidth*scale,
                bodyHeightRaw,
            )
            graphics2D.draw(
                endTexture,
                endPos[0]-endWidthRaw*scale/2f,
                endPos[1]-endHeightRaw*scale/2f,
                MathUtil.fixDegree(rotation+if (!above)0f else 180f),
                endWidthRaw*scale,
                endHeightRaw*scale,
            )
        }
        val lazyDraw= arrayListOf<Pair<CirnoPhiChart.Note,Boolean>>()
        fun drawNoteList(notes:List<CirnoPhiChart.Note>,above:Boolean){
            notes.forEach {
                when(it.type){
                    CirnoPhiChart.NoteType.Tap,CirnoPhiChart.NoteType.Drag,CirnoPhiChart.NoteType.Flick->
                        lazyDraw.add(it to above)
                    CirnoPhiChart.NoteType.Hold->
                        drawHoldNote(it,above)
                }
            }
        }
        drawNoteList(notesAbove,true)
        drawNoteList(notesBelow,false)
        phase1CacheLazy=lazyDraw
        phase1CacheState=Triple(worldCurrentTick,lineY,scale)
    }

    fun drawNotesPhase2(graphics2D: Graphics2D){
        val (worldCurrentTick,lineY,scale)=phase1CacheState
        fun drawNormalNote(note: CirnoPhiChart.Note, above:Boolean){
            val fl = note.floorPosition - lineY
            if (note.time<=worldCurrentTick)return
            if (fl<-0.01f)return
            val texture=when (note.type){
                CirnoPhiChart.NoteType.Tap-> if(note.isHighLighted) tapNoteHLTexture else tapNoteTexture
                CirnoPhiChart.NoteType.Drag-> if(note.isHighLighted) dragNoteHLTexture else dragNoteTexture
                CirnoPhiChart.NoteType.Flick-> if(note.isHighLighted) flickNoteHLTexture else flickNoteTexture
                else->throw IllegalStateException("This should not happen")
            }
            val tempPos= getFootPos(note.positionX)
            val realPos= MathUtil.getPosOutOfLine(
                tempPos[0],tempPos[1],
                MathUtil.fixDegree(rotation+if(above)90f else -90f),
                fl * UNIT_HEIGHT *note.speed
            )
            val width=texture.width
            val height=texture.height
            graphics2D.draw(
                texture,
                realPos[0]-width*scale/2f,
                realPos[1]-height*scale/2f,
                rotation,
                width*scale,
                height*scale,
            )
        }
        phase1CacheLazy.forEach { drawNormalNote(it.first,it.second) }
    }

    private fun getFootPos(positionX:Float):FloatArray=
        MathUtil.getPosOutOfLine(
            x,y,
            MathUtil.fixDegree(rotation),
            positionX* UNIT_WIDTH
        )

    private fun getLineY(worldCurrentTick: Float):Float{
        var t=0f
        for(event in speedEvents){
            if (event.endTime>worldCurrentTick&&event.startTime>worldCurrentTick)break
            if(event.startTime<worldCurrentTick&&worldCurrentTick<event.endTime){
                val duration=event.endTime-event.startTime
                val percent=(worldCurrentTick-event.startTime)/duration
                t+=duration*percent*event.value
                break
            }
            if (event.endTime<worldCurrentTick)
                t+=(event.endTime-event.startTime)*event.value
        }
        return t*secondsPerTick
    }

    private fun processProperty(
        index:Int,
        worldCurrentTick:Float,
        callback:(Pair<Float,Float>)->Unit
    ){
        val judgeLineEvents = eventQueues[index]
        while (true) {
            val currentEvent = judgeLineEvents.getOrNull(eventCursors[index])?:break
            when(val state = currentEvent.currentState(worldCurrentTick)){
                is JudgeLineEvent.GetStateResult.Ok-> {
                    callback(state.value)
                    break
                }
                is JudgeLineEvent.GetStateResult.TimeLate->
                    eventCursors[index]++
                is JudgeLineEvent.GetStateResult.TimeEarly->
                    eventCursors[index]--
            }
        }
    }
}
