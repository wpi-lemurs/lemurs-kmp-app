package com.lemurs.lemurs_app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.ui.reusableComponents.QuestionFactory
import com.lemurs.lemurs_app.ui.viewmodel.SubmissionViewModel
import com.lemurs.lemurs_app.ui.viewmodel.WeeklyQuestionsViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WeeklyQuestionsScreen(onNavigateTo: (String) -> Unit) {
  val viewModel: WeeklyQuestionsViewModel = koinViewModel()
  val logger = Logger.withTag("WeeklyQuestionsScreen")
  val submissionViewModel: SubmissionViewModel = koinInject()
  // Don't clear items here - it interferes with submission screen display
  // submissionViewModel.clearSurveyItems()
  // Show a loading indicator while the survey data is being fetched
  if (viewModel.surveys.value == null) {
    CircularProgressIndicator()
  } else {
    val onNextButtonClicked: () -> Unit = {
      submissionViewModel.markItemCompleted("PHQ-9", "2.00")
      onNavigateTo(LemurScreen.Writing.name)
      viewModel.executeRequests()
    }

    logger.w(" the survey count is: ${viewModel.surveyAnswers.value.size}")
    val survey = viewModel.surveys.value!![0]

    val interactiveQuestions =
      survey.questions.filter {
        // these are types from the daily question and the the last type is the one for the weekly
        // questions
        // blanket cover to make sure they aren't included in the button validation
        it.style != "category" &&
          it.style != "parent-question" &&
          it.style != "parent-question-custom-4-scale"
      }

    val isCompleted by
      remember(survey.questions, viewModel.surveyAnswers.value[survey.id]) {
        derivedStateOf {
          interactiveQuestions.all { question ->
            viewModel.surveyAnswers.value[survey.id]?.containsKey(question.id) == true
          }
        }
      }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
      Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxHeight().fillMaxWidth(0.8F),
      ) {
        // Display each question dynamically using QuestionFactory
        survey.questions.forEach { question ->
          QuestionFactory(
            question = question,
            onAnswerSelected = { answer ->
              val oldOuter = viewModel.surveyAnswers.value
              val newOuter = HashMap(oldOuter)

              // Copied and pasted from daily survey logic

              // made copy of the "inner" map for this survey ID
              val oldInner = newOuter[survey.id] ?: HashMap()
              val newInner = HashMap(oldInner)
              // placed  new answer in the copied inner map and updated it
              newInner[question.id] = answer
              newOuter[survey.id] = newInner
              viewModel.surveyAnswers.value = newOuter

              survey.questions.forEach { q ->
                logger.w {
                  "Question ${q.id} type is: ${q.style} answered: " +
                    "${viewModel.surveyAnswers.value[survey.id]?.containsKey(q.id)}" +
                    "\nis completed: $isCompleted"
                }
              }
            },
          )
        }
      }
      BottomBar(onBottomBarClick = onNextButtonClicked, isClickable = isCompleted)
    }
  }
}
