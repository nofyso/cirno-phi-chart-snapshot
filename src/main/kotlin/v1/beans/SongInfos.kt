package han.cirno.v1.beans

import kotlinx.serialization.Serializable
import java.io.File

data class SongInfo(
    val id:String,
    val name:String,
    val author:String,
    val illustrator:String,
    val difficulties: DifficultyInfo,
    val chartPath:File,
    val illustrationPath:File
)

@Serializable
data class DifficultyInfo(
    val ezDifficulty:Float,
    val hdDifficulty:Float,
    val inDifficulty:Float,
    val atDifficulty:Float
)

val SongInfos= mutableListOf<SongInfo>()