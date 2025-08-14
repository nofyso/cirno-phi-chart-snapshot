package han.cirno.v1.beans

import kotlinx.serialization.Serializable

@Serializable
data class Version(
    val name:String,
    val version:String,
    val code:Int,
    val note:String,
)

@Serializable
data class Config(
    val infoCsvFile:String,
    val illustrationPath:String,
    val chartsPath:String,
)
