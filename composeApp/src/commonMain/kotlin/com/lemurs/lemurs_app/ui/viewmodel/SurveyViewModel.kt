package com.lemurs.lemurs_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.lemurs.lemurs_app.data.repositories.AppRepository
import com.lemurs.lemurs_app.survey.SurveyQuestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SurveyViewModel(private val appRepository: AppRepository) : ViewModel() {

    private val _surveyQuestions = MutableStateFlow<List<SurveyQuestion>>(emptyList())
    val surveyQuestions: StateFlow<List<SurveyQuestion>> = _surveyQuestions

    private val _surveyResult = MutableStateFlow<Result<Unit>?>(null)
    val surveyResult: StateFlow<Result<Unit>?> = _surveyResult
}
//  // Function to fetch and map survey questions to SurveyQuestion for UI
//  fun fetchSurvey(isWeekly: Boolean) {
//    viewModelScope.launch {
//      try {
//        val surveys: Surveys = if (isWeekly) {
//          fetchAndParseWeeklySurvey()
//        } else {
//          fetchAndParseDailySurvey()
//        }
//
//        val mappedQuestions = surveys.questions.map { question ->
//          SurveyQuestion(
//            id = question.id,
//            question = question.question,
//            style = question.style,
//            options = question.options
//          )
//        }
//        _surveyQuestions.value = mappedQuestions
//      } catch (e: Exception) {
//        _surveyResult.value = Result.failure(e)
//      }
//    }
//  }

// Function to submit the survey data after user responses
//  fun submitSurvey() {
//    viewModelScope.launch {
//      try {
//        val completedSurveys = _surveyQuestions.value.map { surveyQuestion ->
//          CompletedSurveys(
//            id = surveyQuestion.id,
//            answers = listOf(
//              Answers(
//                id = surveyQuestion.id,
//                answers = (surveyQuestion.selectedAnswer ?: "").toString()
//              )
//            )
//          )
//        }
//      }

//        val surveySubmission = SurveySubmission(
//          timestamp = System.currentTimeMillis().toString(),
//          surveys = completedSurveys
//        )

// Post the survey submission (you need to call your API here)
// Example: postDailySurvey(surveySubmission)
//        val response = postDailySurvey(surveySubmission)
//
//        if (response.status.value == 200) {
//          _surveyResult.value = Result.success(Unit)
//        } else {
//          _surveyResult.value = Result.failure(Exception("Failed to submit survey"))
//        }
//      } catch (e: Exception) {
//        _surveyResult.value = Result.failure(e)
//      }
//    }
//  }
//}
