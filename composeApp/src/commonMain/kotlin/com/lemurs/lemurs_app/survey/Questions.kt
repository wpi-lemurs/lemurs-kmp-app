package com.lemurs.lemurs_app.survey

import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.util.DemoMode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DangerAlertTrigger(
    val id: Int? = null,
    val questionId: Int? = null,
    @SerialName("question_id")
    val questionIdSnake: Int? = null,
    val threshold: Int? = null,
    @SerialName("triggerThreshold")
    val triggerThresholdCamel: Int? = null,
    @SerialName("trigger_threshold")
    val triggerThresholdSnake: Int? = null,
    val isActive: Boolean? = null,
    @SerialName("is_active")
    val isActiveSnake: Boolean? = null,
    val description: String? = null
) {
    val resolvedQuestionId: Int?
        get() = questionId ?: questionIdSnake

    val resolvedThreshold: Int?
        get() = threshold ?: triggerThresholdCamel ?: triggerThresholdSnake

    val resolvedIsActive: Boolean
        get() = isActive ?: isActiveSnake ?: true
}


@Serializable
data class QuestionRequirements(
    val isTriggerQuestion: Boolean = false,
    val triggerThreshold: Int? = null,
    @SerialName("is_trigger_question")
    val isTriggerQuestionSnake: Boolean? = null,
    @SerialName("trigger_threshold")
    val triggerThresholdSnake: Int? = null
) {
    val resolvedIsTriggerQuestion: Boolean
        get() = isTriggerQuestion || (isTriggerQuestionSnake == true)

    val resolvedTriggerThreshold: Int?
        get() = triggerThreshold ?: triggerThresholdSnake
}

@Serializable
data class Questions(
    val id: Int,
    val question: String,
    val style: String,
    val options: List<String?>?,
    val parentQuestionId: Int?,
    val prerequisiteQuestionId: Int?,
    val prerequisiteAnswer: String?,
    val isTriggerQuestion: Boolean = false,
    val triggerThreshold: Int? = null,
    @SerialName("is_trigger_question")
    val isTriggerQuestionSnake: Boolean? = null,
    @SerialName("trigger_threshold")
    val triggerThresholdSnake: Int? = null,
    val requirements: QuestionRequirements? = null
) {
    // Resolve trigger configuration from either flat fields or nested requirements payload.
    val resolvedIsTriggerQuestion: Boolean
        get() =
            isTriggerQuestion ||
                (isTriggerQuestionSnake == true) ||
                (requirements?.resolvedIsTriggerQuestion == true)

    val resolvedTriggerThreshold: Int?
        get() = triggerThreshold ?: triggerThresholdSnake ?: requirements?.resolvedTriggerThreshold
}

@Serializable
data class Surveys(
    val id: Int,
    val name: String,
    val questions: List<Questions>
)

/**
 * Demo mode hardcoded daily survey data
 * Contains sample questions for App Store review purposes
 */
private fun getDemoDailySurvey(): List<Surveys> {
    return listOf(
        Surveys(
            id = 1,
            name = "Daily Mood Survey",
            questions = listOf(
                Questions(
                    id = 1,
                    question = "How are you feeling right now?",
                    style = "5-scale",
                    options = listOf("Very Bad", "Bad", "Neutral", "Good", "Very Good"),
                    parentQuestionId = null,
                    prerequisiteQuestionId = null,
                    prerequisiteAnswer = null,
                    isTriggerQuestion = false,
                    triggerThreshold = null
                ),
                Questions(
                    id = 2,
                    question = "How stressed do you feel right now?",
                    style = "5-scale",
                    options = listOf("Not at all", "Slightly", "Moderately", "Very", "Extremely"),
                    parentQuestionId = null,
                    prerequisiteQuestionId = null,
                    prerequisiteAnswer = null,
                    isTriggerQuestion = false,
                    triggerThreshold = null
                ),
                Questions(
                    id = 3,
                    question = "How well did you sleep last night?",
                    style = "5-scale",
                    options = listOf("Very Poor", "Poor", "Fair", "Good", "Very Good"),
                    parentQuestionId = null,
                    prerequisiteQuestionId = null,
                    prerequisiteAnswer = null,
                    isTriggerQuestion = false,
                    triggerThreshold = null
                ),
                Questions(
                    id = 4,
                    question = "How motivated do you feel today?",
                    style = "5-scale",
                    options = listOf("Not at all", "Slightly", "Moderately", "Very", "Extremely"),
                    parentQuestionId = null,
                    prerequisiteQuestionId = null,
                    prerequisiteAnswer = null,
                    isTriggerQuestion = false,
                    triggerThreshold = null
                ),
                Questions(
                    id = 5,
                    question = "How connected do you feel to others right now?",
                    style = "5-scale",
                    options = listOf("Not at all", "Slightly", "Moderately", "Very", "Extremely"),
                    parentQuestionId = null,
                    prerequisiteQuestionId = null,
                    prerequisiteAnswer = null,
                    isTriggerQuestion = false,
                    triggerThreshold = null
                )
            )
        )
    )
}

/**
 * Demo mode hardcoded weekly survey data (PHQ-9 style)
 * Contains sample questions for App Store review purposes
 */
private fun getDemoWeeklySurvey(): List<Surveys> {
    return listOf(
        Surveys(
            id = 2,
            name = "PHQ-9 Weekly Survey",
            questions = listOf(
                Questions(
                    id = 59,
                    question = "Little interest or pleasure in doing things",
                    style = "4-scale",
                    options = listOf("Not at all", "Several days", "More than half the days", "Nearly every day"),
                    parentQuestionId = null,
                    prerequisiteQuestionId = null,
                    prerequisiteAnswer = null,
                    isTriggerQuestion = false,
                    triggerThreshold = null
                ),
                Questions(
                    id = 60,
                    question = "Feeling down, depressed, or hopeless",
                    style = "4-scale",
                    options = listOf("Not at all", "Several days", "More than half the days", "Nearly every day"),
                    parentQuestionId = null,
                    prerequisiteQuestionId = null,
                    prerequisiteAnswer = null,
                    isTriggerQuestion = false,
                    triggerThreshold = null
                ),
                Questions(
                    id = 61,
                    question = "Trouble falling or staying asleep, or sleeping too much",
                    style = "4-scale",
                    options = listOf("Not at all", "Several days", "More than half the days", "Nearly every day"),
                    parentQuestionId = null,
                    prerequisiteQuestionId = null,
                    prerequisiteAnswer = null,
                    isTriggerQuestion = false,
                    triggerThreshold = null
                ),
                Questions(
                    id = 62,
                    question = "Feeling tired or having little energy",
                    style = "4-scale",
                    options = listOf("Not at all", "Several days", "More than half the days", "Nearly every day"),
                    parentQuestionId = null,
                    prerequisiteQuestionId = null,
                    prerequisiteAnswer = null,
                    isTriggerQuestion = false,
                    triggerThreshold = null
                ),
                Questions(
                    id = 63,
                    question = "Poor appetite or overeating",
                    style = "4-scale",
                    options = listOf("Not at all", "Several days", "More than half the days", "Nearly every day"),
                    parentQuestionId = null,
                    prerequisiteQuestionId = null,
                    prerequisiteAnswer = null,
                    isTriggerQuestion = false,
                    triggerThreshold = null
                ),
                Questions(
                    id = 64,
                    question = "Feeling bad about yourself — or that you are a failure or have let yourself or your family down",
                    style = "4-scale",
                    options = listOf("Not at all", "Several days", "More than half the days", "Nearly every day"),
                    parentQuestionId = null,
                    prerequisiteQuestionId = null,
                    prerequisiteAnswer = null,
                    isTriggerQuestion = false,
                    triggerThreshold = null
                ),
                Questions(
                    id = 65,
                    question = "Trouble concentrating on things, such as reading the newspaper or watching television",
                    style = "4-scale",
                    options = listOf("Not at all", "Several days", "More than half the days", "Nearly every day"),
                    parentQuestionId = null,
                    prerequisiteQuestionId = null,
                    prerequisiteAnswer = null,
                    isTriggerQuestion = false,
                    triggerThreshold = null
                ),
                Questions(
                    id = 66,
                    question = "Moving or speaking so slowly that other people could have noticed? Or the opposite — being so fidgety or restless that you have been moving around a lot more than usual",
                    style = "4-scale",
                    options = listOf("Not at all", "Several days", "More than half the days", "Nearly every day"),
                    parentQuestionId = null,
                    prerequisiteQuestionId = null,
                    prerequisiteAnswer = null,
                    isTriggerQuestion = false,
                    triggerThreshold = null
                ),
                Questions(
                    id = 67,
                    question = "Thoughts that you would be better off dead or of hurting yourself in some way",
                    style = "4-scale",
                    options = listOf("Not at all", "Several days", "More than half the days", "Nearly every day"),
                    parentQuestionId = null,
                    prerequisiteQuestionId = null,
                    prerequisiteAnswer = null,
                    isTriggerQuestion = true,
                    triggerThreshold = 1
                ),
                Questions(
                    id = 69,
                    question = "If you checked off any problems, how difficult have these problems made it for you to do your work, take care of things at home, or get along with other people?",
                    style = "4-scale",
                    options = listOf("Not difficult at all", "Somewhat difficult", "Very difficult", "Extremely difficult"),
                    parentQuestionId = null,
                    prerequisiteQuestionId = null,
                    prerequisiteAnswer = null,
                    isTriggerQuestion = false,
                    triggerThreshold = null
                )
            )
        )
    )
}

suspend fun fetchAndParseDailySurvey(): List<Surveys> {
    // Return demo data if demo mode is active
    if (DemoMode.isActive) {
        Logger.withTag("Questions").d("Demo mode: returning hardcoded daily survey")
        return getDemoDailySurvey()
    }

    val api = SurveysApiImpl()
    return api.getDailySurvey()
}

suspend fun fetchAndParseWeeklySurvey(): List<Surveys> {
    // Return demo data if demo mode is active
    if (DemoMode.isActive) {
        Logger.withTag("Questions").d("Demo mode: returning hardcoded weekly survey")
        return getDemoWeeklySurvey()
    }

    val api = SurveysApiImpl()
    return api.getWeeklySurvey()
}

suspend fun fetchDangerAlertTriggers(): List<DangerAlertTrigger> {
    if (DemoMode.isActive) {
        Logger.withTag("Questions").d("Demo mode: skipping danger alert trigger fetch")
        return emptyList()
    }

    val api = SurveysApiImpl()
    return api.getDangerAlertTriggers()
}

suspend fun fetchDemographic() :List<Demographic>{
    // Return empty list in demo mode (demographics not needed for review)
    if (DemoMode.isActive) {
        Logger.withTag("Questions").d("Demo mode: skipping demographics fetch")
        return emptyList()
    }

    Logger.withTag("Questions").w("fetching demographic")
    val api = SurveysApiImpl()
    val dem = api.getDemographics()

    Logger.withTag("Questions").w("fetched demographic in Questions"+ dem.toString())
    return dem
}
