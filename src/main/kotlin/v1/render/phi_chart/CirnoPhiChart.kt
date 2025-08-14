package han.cirno.v1.render.phi_chart

import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets

class CirnoPhiChart(originalByteArray: ByteArray) {
    val formatVersion:Int
    val offset:Float
    val judgeLines:List<JudgeLine>

    constructor(file: File) : this(file.readBytes())

    constructor(inputStream: InputStream) : this(inputStream.readBytes())

    init {
        val jsonObject=JSONObject(String(originalByteArray, StandardCharsets.UTF_8))
        formatVersion=jsonObject.getInt("formatVersion")
        offset=jsonObject.getFloat("offset")
        val judgeLinesArray = jsonObject.getJSONArray("judgeLineList")
        judgeLines = judgeLinesArray.map {
            it as JSONObject
            JudgeLine(it)
        }
        processSameTime(judgeLines)
    }

    private fun processSameTime(judgeLines:List<JudgeLine>){
        val timeSet=HashSet<Int>()
        val sameTimeSet=HashSet<Int>()
        judgeLines.forEach {line->
            val query:(Note)->Unit={note->
                val time=note.time
                if (time in timeSet)
                    sameTimeSet.add(time)
                else
                    timeSet.add(time)
            }
            line.notesAbove.forEach(query)
            line.notesBelow.forEach(query)
        }
        judgeLines.forEach {line->
            val query:(Note)->Unit={note->
                if (note.time in sameTimeSet)
                    note.isHighLighted=true
            }
            line.notesAbove.forEach(query)
            line.notesBelow.forEach(query)
        }
    }

    class JudgeLine(judgeLineObject: JSONObject) {
        val bpm: Float = judgeLineObject.getFloat("bpm")
        val notesAbove:List<Note> = judgeLineObject.getJSONArray("notesAbove").map { Note(it as JSONObject) }
        val notesBelow:List<Note> = judgeLineObject.getJSONArray("notesBelow").map { Note(it as JSONObject) }
        val speedEvents:List<Event.SpeedEvent> = judgeLineObject.getJSONArray("speedEvents").map {
            Event.SpeedEvent(it as JSONObject)
        }
        val judgeLineMoveEvents:List<Event.JudgeLineMoveEvent> =
            judgeLineObject.getJSONArray("judgeLineMoveEvents").map {
                Event.JudgeLineMoveEvent(it as JSONObject)
            }
        val judgeLineRotateEvent:List<Event.JudgeLineRotateEvent> =
            judgeLineObject.getJSONArray("judgeLineRotateEvents").map {
                Event.JudgeLineRotateEvent(it as JSONObject)
            }
        val judgeLineDisappearEvent:List<Event.JudgeLineDisappearEvent> =
            judgeLineObject.getJSONArray("judgeLineDisappearEvents").map {
                Event.JudgeLineDisappearEvent(it as JSONObject)
            }
    }

    class Note(noteObject:JSONObject){
        val type:NoteType=NoteType.parse(noteObject.getInt("type"))!!
        val time:Int=noteObject.getInt("time")
        val positionX:Float=noteObject.getFloat("positionX")
        val holdTime:Float=noteObject.getFloat("holdTime")
        val speed:Float=noteObject.getFloat("speed")
        val floorPosition:Float=noteObject.getFloat("floorPosition")
        var isHighLighted:Boolean=false
    }

    sealed class Event(eventObject: JSONObject){
        val startTime:Float=eventObject.getFloat("startTime")
        val endTime:Float=eventObject.getFloat("endTime")

        class SpeedEvent(eventObject:JSONObject):Event(eventObject){
            val value:Float=eventObject.getFloat("value")
        }

        class JudgeLineMoveEvent(eventObject:JSONObject):Event(eventObject){
            val start:Float=eventObject.getFloat("start")
            val end:Float=eventObject.getFloat("end")
            val start2:Float=eventObject.getFloat("start2")
            val end2:Float=eventObject.getFloat("end2")
        }

        class JudgeLineRotateEvent(eventObject:JSONObject):Event(eventObject){
            val start:Float=eventObject.getFloat("start")
            val end:Float=eventObject.getFloat("end")
        }

        class JudgeLineDisappearEvent(eventObject:JSONObject):Event(eventObject){
            val start:Float=eventObject.getFloat("start")
            val end:Float=eventObject.getFloat("end")
        }
    }

    enum class NoteType(val typeValue:Int){
        Tap(1),
        Drag(2),
        Hold(3),
        Flick(4);
        companion object{
            fun parse(typeValue:Int):NoteType?=
                entries.find { it.typeValue == typeValue }
        }
    }

    enum class Difficulty{
        EZ,
        HD,
        IN,
        AT,
    }
}
