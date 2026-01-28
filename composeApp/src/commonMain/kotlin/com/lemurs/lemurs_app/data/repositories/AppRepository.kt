package com.lemurs.lemurs_app.data.repositories

import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.api.LemursApiServiceImpl.AudioDataRequest
import com.lemurs.lemurs_app.data.api.LemursApiServiceImpl.SurveyDataRequest
import com.lemurs.lemurs_app.data.api.LemursApiServiceImpl.WrittenResponseRequest
import com.lemurs.lemurs_app.data.datasource.health.HealthRemoteDataSource
import com.lemurs.lemurs_app.data.dtos.CaloriesDataDto
import com.lemurs.lemurs_app.data.dtos.DistanceDataDto
import com.lemurs.lemurs_app.data.dtos.SpeedDataDto
import com.lemurs.lemurs_app.data.dtos.StepsDataDto
import com.lemurs.lemurs_app.data.dtos.WeightDataDto
import com.lemurs.lemurs_app.data.local.activeData.Audio
import com.lemurs.lemurs_app.data.local.activeData.AudioDAO
import com.lemurs.lemurs_app.data.local.activeData.SurveyResponseDAO
import com.lemurs.lemurs_app.data.local.activeData.SurveyResponse
import com.lemurs.lemurs_app.data.local.activeData.Written
import com.lemurs.lemurs_app.data.local.activeData.WrittenDAO
import com.lemurs.lemurs_app.survey.Answers
import com.lemurs.lemurs_app.survey.CompletedSurveys
import com.lemurs.lemurs_app.survey.SurveySubmission
import com.lemurs.lemurs_app.survey.postDailySurvey
import com.lemurs.lemurs_app.survey.postWeeklySurvey
import com.lemurs.lemurs_app.ui.viewmodel.WeeklySurveyResponse
import io.ktor.client.call.body
import io.ktor.http.isSuccess
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

class AppRepository(
    private val healthRemoteDataSource: HealthRemoteDataSource,
    private val surveyResponseDAO: SurveyResponseDAO,
    private val audioDAO: AudioDAO,
    private val writingDAO: WrittenDAO
) {
    suspend fun submitData(input: SurveyDataRequest): Boolean {

        Logger.w("AppRepository") {
            "Entering --------- \n " + "submitData called with type: ${input.type}, data: ${input.data}"
        }

        return healthRemoteDataSource.submitData(input).isSuccess
    }

    suspend fun saveWriting(input: WrittenResponseRequest): Boolean {
        Logger.w("AppRepository") {
            "Entering --------- \n " + "saveWriting() called with time: ${input.timestamp}, questionId: ${input.writtenQuestionId}..., surveyResponseId: ${input.surveyResponseId}, writtenData: ${input.writtenData}"
        }
        return healthRemoteDataSource.saveWriting(input).isSuccess
    }


    suspend fun submitAudioData(input: AudioDataRequest): Boolean {
        Logger.w("AppRepository") {
            "Entering --------- \n " + "submitAudioData called with time: ${input.timestamp}, audioByte64: ${input.audioByte64.take(50)}..., surveyResponseId: ${input.surveyResponseId}, audioQuestionId: ${input.audioQuestionId}"
        }
        return healthRemoteDataSource.submitAudioData(input).isSuccess
    }


    suspend fun sendWeightData(weightRecord: WeightDataDto): Boolean {
        return healthRemoteDataSource.sendWeightData(weightRecord).isSuccess
    }

    suspend fun sendStepsData(stepsData: StepsDataDto): Boolean {
        return healthRemoteDataSource.sendStepsData(stepsData).isSuccess
    }
    suspend fun sendDistanceData(distanceRecord: DistanceDataDto): Boolean {
        return healthRemoteDataSource.sendDistanceData(distanceRecord).isSuccess
    }

    suspend fun sendCaloriesData(calorieRecord: CaloriesDataDto): Boolean {
        return healthRemoteDataSource.sendCaloriesData(calorieRecord).isSuccess
    }

    suspend fun sendSpeedData(speedRecord: SpeedDataDto): Boolean {
        return healthRemoteDataSource.sendSpeedData(speedRecord).isSuccess
    }

    // Save response locally
    suspend fun saveSurveyResponseLocally(response: SurveyResponse) {
        surveyResponseDAO.insert(response)
    }
    suspend fun saveAudioResponseLocally(response: Audio) {
        audioDAO.insert(response)
    }
    suspend fun saveWrittenResponseLocally(response: Written) {
        writingDAO.insert(response)
    }


    // Check WiFi connectivity (stub, implement platform-specific)
    suspend fun isWifiAvailable(): Boolean {
        // TODO: Implement platform-specific WiFi check
        return true
    }

    // Send response to server
    suspend fun sendSurveyResponseToServer(submission: SurveySubmission, type: Int): Int {
        when (type) {
            0, 1 -> {
                if (postDailySurvey(submission).status.isSuccess()) {
                    return 0
                }
                return -1
            }
            2 -> {
                //Handle Weekly Survey
                val httpResponse = postWeeklySurvey(submission)
                if (httpResponse.status.isSuccess()) {
                    val responseBody = httpResponse.body<WeeklySurveyResponse>()
                    return responseBody.surveyResponseId
                } else {
                    return -1
                }
            }

            else -> throw error("Invalid type: $type")
        }
    }

    // Helper to parse answers JSON to SurveyDataRequest
    fun parseSurveyResponseToSubmission(response: SurveyResponse): SurveySubmission? {
        return try {
            // 1. Decode the JSON string back into a map of answer IDs to answer values.
            val answersMap: Map<String, String> = Json.decodeFromString(response.answers)

            // 2. Convert the map into a List<Answers> as required by CompletedSurveys.
            val answersList = answersMap.map { (answerId, answerValue) ->
                Answers(id = answerId.toInt(), answer = answerValue)
            }

            // 3. Create the CompletedSurveys object from the response data.
            val completedSurvey = CompletedSurveys(id = response.getID(), answers = answersList)

            // 4. Parse the timestamp string into an Instant.
            val submissionTimestamp = Instant.parse(response.timestamp)

            // 5. Parse the notification time string into an Instant.
            val notificationTime = Instant.parse(response.notificationTime)

            // 6. Construct the final SurveySubmission object.
            SurveySubmission(
                timestamp = submissionTimestamp,
                surveys = listOf(completedSurvey), // Wrap the single survey in a list
                notificationStart = notificationTime
            )
        } catch (e: Exception) {
            // Use a specific logger tag for better debugging.
            Logger.withTag("AppRepository").e("Failed to parse SurveyResponse to SurveySubmission: ${e.message}")
            null
        }
    }

    // Main function to handle survey response
    suspend fun handleSurveyResponse(): Boolean {
        val unsentResponses = surveyResponseDAO.getAll()
        var allSent = true
        for (survey in unsentResponses) {
            if (survey.type != 2) {
                //turn survey into survey submission
                val submission = parseSurveyResponseToSubmission(survey)
                if (submission != null) {
                    val sent = sendSurveyResponseToServer(submission, survey.type)
                    if (sent != -1) {
                        surveyResponseDAO.delete(survey)
                    } else {
                        allSent = false
                    }
                } else {
                    allSent = false
                }
            } else if (survey.type == 2) {
                if (!survey.submitted){
                    val submission = parseSurveyResponseToSubmission(survey)
                    if (submission != null) {
                        val surveyId = sendSurveyResponseToServer(submission, survey.type)
                        if (surveyId != -1) {
                            surveyResponseDAO.modifySubmission(survey, surveyId)
                            var audioSent = false
                            var writtenSent = false
                            try {
                                val audio = audioDAO.getAudioDataBySurveyResponseId(-survey.getID())
                                if (audio != null) {
                                    val audioSubmission = AudioDataRequest(
                                        timestamp = audio.date,
                                        audioQuestionId = audio.questionId,
                                        audioByte64 = audio.audioByte64,
                                        surveyResponseId = surveyId
                                    )
                                    audioSent = submitAudioData(audioSubmission)
                                    if (audioSent) {
                                        audioDAO.delete(audio)
                                    }
                                }
                            } catch ( e: Exception){
                                audioSent = false
                                Logger.withTag("AppRepository").e("Failed to send audio data: ${e.message}")
                            }
                            try {
                                val written = writingDAO.getWrittenDataBySurveyResponseId(-survey.getID())
                                if (written != null) {
                                    val writtenSubmission = WrittenResponseRequest(
                                        surveyResponseId = surveyId,
                                        writtenQuestionId = written.questionNumber,
                                        writtenData = written.response,
                                        timestamp = written.date
                                    )
                                    writtenSent = saveWriting(writtenSubmission)
                                    if (writtenSent) {
                                        writingDAO.delete(written)
                                    }
                                }
                            } catch (e : Exception){
                                writtenSent = false
                                Logger.withTag("AppRepository").e("Failed to send written data: ${e.message}")
                            }
                            Logger.withTag("AppRepository").d("Audio sent: $audioSent, Written sent: $writtenSent")
                            if (audioSent && writtenSent) {
                                // if both sent delete survey otherwise leave
                                // survey around to retry could be caused by failure to send or by lack or submission on user part
                                surveyResponseDAO.delete(survey)
                            } else {
                                allSent = false
                            }
                        } else{
                            allSent = false
                        }
                    } else {
                        allSent = false
                    }
                } else {
                    var audioSent = true
                    var writtenSent = true
                    try {
                        val audio = audioDAO.getAudioDataBySurveyResponseId(-survey.getID())
                        if (audio != null) {
                            val audioSubmission = AudioDataRequest(
                                timestamp = audio.date,
                                audioQuestionId = audio.questionId,
                                audioByte64 = audio.audioByte64,
                                surveyResponseId = survey.surveyResponseId
                            )
                            audioSent = submitAudioData(audioSubmission)
                            if (audioSent) {
                                audioDAO.delete(audio)
                            }
                        }
                    } catch ( e: Exception){
                        audioSent = false
                        Logger.withTag("AppRepository").e("Failed to send audio data: ${e.message}")
                    }
                    try {
                        val written = writingDAO.getWrittenDataBySurveyResponseId(-survey.getID())
                        if (written != null) {
                            val writtenSubmission = WrittenResponseRequest(
                                surveyResponseId = survey.surveyResponseId,
                                writtenQuestionId = written.questionNumber,
                                writtenData = written.response,
                                timestamp = written.date
                            )
                            writtenSent = saveWriting(writtenSubmission)
                            if (writtenSent) {
                                writingDAO.delete(written)
                            }
                        }
                    } catch (e : Exception){
                        writtenSent = false
                        Logger.withTag("AppRepository").e("Failed to send written data: ${e.message}")
                    }
                    Logger.withTag("AppRepository").d("Audio sent: $audioSent, Written sent: $writtenSent")
                    if (audioSent && writtenSent) {
                        //if both sent or both don't exist, delete survey since it
                        // tried two times and it means that the audio and/or written do not exist (skipped)
                        surveyResponseDAO.delete(survey)
                    } else { //if at least 1 is false it means it exists but failed to send so keep survey and retry
                        allSent = false
                    }
                }
            }
        }
        return allSent
    }
}
