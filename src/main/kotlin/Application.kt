package han.cirno

import com.opencsv.CSVReader
import han.cirno.v1.beans.DifficultyInfo
import han.cirno.v1.beans.SongInfo
import han.cirno.v1.beans.SongInfos
import han.cirno.v1.beans.Config
import han.cirno.v1.generatorRouting
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

private fun Application.loadConfig(){
    val cpcsConfig=File("cpcsConfig.json")
    log.info("Loading config")
    if (!cpcsConfig.exists()) {
        cpcsConfig.createNewFile()
        val value = Config(
            "res/info.csv",
            "res/illustrations",
            "res/charts"
        )
        cpcsConfig.writeText(Json.encodeToString(Config.serializer(), value))
    }
    val config=Json.decodeFromString<Config>(cpcsConfig.readText())
    log.info("info.csv: ${config.infoCsvFile}")
    log.info("charts: ${config.chartsPath}")
    log.info("illustrations: ${config.illustrationPath}")
    val infoCsvFile=File(config.infoCsvFile)
    if (!infoCsvFile.exists())
        throw FileNotFoundException("info.csv file does not exist")
    SongInfos.clear()
    CSVReader(FileReader(infoCsvFile)).use { reader->
        val all = reader.readAll()
        all.drop(1).forEach {
            val values=it
            val id=values[0]
            val name=values[1]
            val author=values[2]
            val illustrator=values[3]
            val difficultyInfo= DifficultyInfo(
                values[4].toFloatOrNull()?:0f,
                values[5].toFloatOrNull()?:0f,
                values[6].toFloatOrNull()?:0f,
                values[7].toFloatOrNull()?:0f
            )
            val chartPath=arrayOf(
                File(config.chartsPath, id),
                File(config.chartsPath, "$id.0")
            ).firstOrNull { f -> f.isDirectory && f.exists() }
                ?:throw FileNotFoundException("Chart path of $id not found")
            val illustrationPath=arrayOf(
                File(config.illustrationPath, "$id.png"),
                File(config.illustrationPath, "$id.0.png")
            ).firstOrNull { f -> !f.isDirectory && f.exists() }
                ?:throw FileNotFoundException("Illustration path of $id not found")
            SongInfos.add(SongInfo(id,name,author,illustrator,difficultyInfo,chartPath,illustrationPath))
        }
    }
    log.info("${SongInfos.size} of entries pre-loaded")
}

fun Application.module() {
    configureHTTP()
    generatorRouting()
    install(ContentNegotiation){
        json()
    }
    loadConfig()
}
