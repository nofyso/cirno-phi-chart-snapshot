package han.cirno.v1.render.`object`

import han.cirno.v1.render.phi_chart.CirnoPhiChart


class JudgeLineEvent(
    private val event: CirnoPhiChart.Event
) {
    val startTime
        get()=event.startTime
    val endTime
        get()=event.endTime

    sealed class GetStateResult{
        data class Ok(val value: Pair<Float,Float>): GetStateResult()
        data object TimeLate: GetStateResult()
        data object TimeEarly: GetStateResult()
    }

    /**
     * If single event, the second Float is always 0.0f
     */
    fun currentState(time:Float): GetStateResult {
        if (time>=endTime)
            return GetStateResult.TimeLate
        if (time<startTime)
            return GetStateResult.TimeEarly
        val timePercent=(time-startTime)/(endTime-startTime)
        return GetStateResult.Ok(
            when (event) {
                is CirnoPhiChart.Event.SpeedEvent -> event.value to 0.0f
                is CirnoPhiChart.Event.JudgeLineMoveEvent ->
                    event.start + (event.end - event.start) * timePercent to event.start2 + (event.end2 - event.start2) * timePercent
                is CirnoPhiChart.Event.JudgeLineRotateEvent ->
                    event.start + (event.end - event.start) * timePercent to 0.0f
                is CirnoPhiChart.Event.JudgeLineDisappearEvent ->
                    event.start + (event.end - event.start) * timePercent to 0.0f
            }
        )
    }
}
