package han.cirno.v1

import han.cirno.v1.beans.DifficultyInfo
import han.cirno.v1.beans.SongInfo
import han.cirno.v1.beans.SongInfos
import han.cirno.v1.beans.Version
import han.cirno.v1.render.phi_chart.CirnoPhiChart
import han.cirno.v1.render.render
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.util.Base64

sealed class RenderResult{
    companion object{
        const val SUCCESS = 0
        const val ERROR_DIFFICULTY_NOT_FOUND=-2
        const val ERROR_CHART_RESOLVE_FAILED=-3
    }
    @Serializable
    data class Failed(val errorCode:Int,val errorMessage:String):RenderResult()

    @Serializable
    data class Success(
        val errorCode:Int,
        val songInfo: ReturnSongInfo,
        val timeInSecond:Float,
        val picture:String
    ):RenderResult()
}

@Serializable
data class ReturnSongInfo(
    val id:String,
    val name:String,
    val author:String,
    val illustrator:String,
    val difficulties: DifficultyInfo,
){
    constructor(songInfo: SongInfo) : this(
        songInfo.id,
        songInfo.name,
        songInfo.author,
        songInfo.illustrator,
        songInfo.difficulties
    )
}


fun Application.generatorRouting() {
    routing {
        get("v1/version"){
            call.respond(
                Version(
                    name = "cirno-phi-chart-snapshot",
                    version = "0.1.0",
                    code = 1,
                    note = "欸？？我是谁？我在哪？？我为什么会在Version里？？文档里不是这么写的啊？？！"
                )
            )
        }
        get("v1/snapshot") {
            val result = snapshot()
            call.respond(result)
        }
        get("v1/snapshotRaw") {
            when (val result = snapshot()) {
                is RenderResult.Success -> {
                    call.respondBytes(contentType = ContentType.Image.PNG) {
                        Base64.getDecoder().decode(result.picture)
                    }
                }
                is RenderResult.Failed->call.respond(HttpStatusCode.BadRequest,result)
            }
        }
    }
}

private fun RoutingContext.snapshot():RenderResult{
    val queryParameters = call.request.queryParameters
    val difficultyRaw=queryParameters["difficulty"]
    val id=queryParameters["id"]?: SongInfos.filter {
        !(difficultyRaw=="AT"&&it.difficulties.atDifficulty==0f)
    }.random().id
    val songInfo= SongInfos.find { it.id==id }!!
    val difficulty= CirnoPhiChart.Difficulty.valueOf(
        difficultyRaw?:CirnoPhiChart.Difficulty.entries.filter {
            !(songInfo.difficulties.atDifficulty==0f&&it==CirnoPhiChart.Difficulty.AT)
        }.random().name)
    val timeInSecond=queryParameters["time"]?.toFloatOrNull()
    val showCombo=queryParameters["showCombo"]?.toBoolean() ?: true
    val showScore=queryParameters["showScore"]?.toBoolean() ?: true
    val showDifficulty=queryParameters["showDifficulty"]?.toBoolean() ?: true
    val showChartName=queryParameters["showChartName"]?.toBoolean() ?: true
    val showHitEffect=queryParameters["showHitEffect"]?.toBoolean() ?: true
    val showIllustration=queryParameters["showIllustration"]?.toBoolean() ?: true
    val showCopyright=queryParameters["showCopyright"]?.toBoolean() ?: true
    return render(
        songInfo,difficulty,timeInSecond,
        showCombo=showCombo,
        showScore=showScore,
        showDifficulty=showDifficulty,
        showChartName=showChartName,
        showHitEffect=showHitEffect,
        showIllustration=showIllustration,
        showCopyright=showCopyright)
}