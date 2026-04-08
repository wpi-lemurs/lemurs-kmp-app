package com.lemurs.lemurs_app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import androidx.navigation.NavController
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.survey.Demographic
import com.lemurs.lemurs_app.ui.reusableComponents.BubbleSelectionQuestion
import com.lemurs.lemurs_app.ui.reusableComponents.DropDownSelectionQuestion
import com.lemurs.lemurs_app.ui.reusableComponents.TextInputQuestion
import com.lemurs.lemurs_app.ui.reusableComponents.VerticalBubbleQuestionWithOtherText
import com.lemurs.lemurs_app.ui.theme.LemurDarkGrey
import com.lemurs.lemurs_app.ui.viewmodel.DemographicsViewModel
//import org.koin.compose.viewmodel.koinViewModel
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.IO
//import kotlinx.coroutines.launch
import kotlinx.coroutines.launch


@Composable
fun DemographicsScreen(viewModel: DemographicsViewModel,
                       onNavigateTo: (String) -> Unit) {

    val logger = Logger.withTag("DemographicsScreen")

    val isCompleted = rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // States to track if each question is answered
    val isQuestion1Answered = rememberSaveable { mutableStateOf(false) }
    val isQuestion2Answered = rememberSaveable { mutableStateOf(false) }
    val isQuestion3Answered = rememberSaveable { mutableStateOf(false) }
    val isQuestion4Answered = rememberSaveable { mutableStateOf(false) }
    val answers = rememberSaveable { mutableStateOf(ArrayList<Demographic>())}

    // Update isCompleted once all questions are answered
    LaunchedEffect(
        isQuestion1Answered.value,
        isQuestion2Answered.value,
        isQuestion3Answered.value,
        isQuestion4Answered.value,
        answers.value
    ) {
        isCompleted.value =
            isQuestion1Answered.value && isQuestion2Answered.value && isQuestion3Answered.value && isQuestion4Answered.value
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Demographics",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp, lineHeight =  50.sp),
                color = LemurDarkGrey,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            //What sex were you assigned at birth –
            //
            //(a) male, (b) female, (c) intersex, (d) another term describes me better (text entry field)
            //
            //
            //
            //What is your gender identity –
            //
            //(a) male, (b) female, (c) nonbinary, (d) another term describes me better (text entry field)
            //
            //
            //
            //How do you identify your sexual orientation:
            //
            //(a) gay, (b) lesbian, (c) bisexual, (d)heterosexual/straight, (e) another term describes me better (text entry field)

            // Question 1 - Age Input
            TextInputQuestion(
                questionText = "What is your age?",
                answerLabel = "Enter your age",
                onAnswerFilled = fun(answer: String): Unit {
                    isQuestion1Answered.value = answer != ""
                    answers.value.add(Demographic("age", answer))
                    logger.w("answers with age: "+ answers)
                }
            )

            // Question 2 - Bubble Selection Gender at birth
            //  //(a) male, (b) female, (c) intersex, (d) another term describes me better (text entry field)
            VerticalBubbleQuestionWithOtherText(
                questionText = "What sex were you assigned at birth?",
                numberOfOptions = 3,
                options = listOf("Male", "Female", "Intersex"),
                otherHeader = "Another term describes me better:",
                otherLabel = "Enter other sex",
                onOptionSelected = fun(answer: String, otherAnswer: String): Unit { isQuestion2Answered.value = true
                    var answerVal = "Other"
                    if(answer.equals("0")){
                        answerVal = "Male"
                    }else if (answer.equals("1")){
                        answerVal = "Female"
                    }else if (answer.equals("2")){
                        answerVal = "Intersex"
                    }else if (answer.equals("3")){
                        answerVal = otherAnswer
                    }
                    answers.value.add(Demographic("gender at birth", answerVal))
                    logger.w("answers with gender at birth: "+ answers)}
            )

            // Question 3 - Bubble Selection gender identity
            //(a) male, (b) female, (c) nonbinary, (d) another term describes me better (text entry field)
            VerticalBubbleQuestionWithOtherText(
                questionText = "What is your gender identity?",
                numberOfOptions = 3,
                options = listOf("Male", "Female", "Nonbinary"),
                otherHeader = "Another term describes me better:",
                otherLabel = "Enter other gender identity",
                onOptionSelected = fun(answer: String, otherAnswer: String): Unit { isQuestion3Answered.value = true
                    var answerVal = "Other"
                    if(answer.equals("0")){
                        answerVal = "Male"
                    }else if (answer.equals("1")){
                        answerVal = "Female"
                    }else if (answer.equals("2")){
                        answerVal = "Nonbinary"
                    }else if (answer.equals("3")){
                        answerVal = otherAnswer
                    }
                    answers.value.add(Demographic("gender identity", answerVal))
                    logger.w("answers with gender: "+ answers)}
            )

            // Question 4 - Bubble Selection lgbt
            //(a) gay, (b) lesbian, (c) bisexual, (d)heterosexual/straight, (e) another term describes me better (text entry field)
            VerticalBubbleQuestionWithOtherText(
                questionText = "How do you identify your sexual orientation?",
                numberOfOptions = 4,
                options = listOf("Gay", "Lesbian", "Bisexual", "Heterosexual/Straight"),
                otherHeader = "Another term describes me better:",
                otherLabel = "Enter other sexual orientation",
                onOptionSelected = fun(answer: String, otherAnswer: String): Unit{ isQuestion4Answered.value = true
                    var answerVal = false
                    if(answer.equals("0")){
                        answerVal = true
                    }else if (answer.equals("1")){
                        answerVal = true
                    }else if (answer.equals("2")){
                        answerVal = true
                    }else if (answer.equals("3")){
                        answerVal = false
                    }else if (answer.equals("4")){
                        answerVal = true
                    }
                    answers.value.add(Demographic("lgbt", answerVal.toString()))
                    logger.w("answers with lgbt: "+ answers)}
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(alignment = Alignment.BottomCenter)
        ) {
            //This is the bottom bar, change the isComplete.value to true when survey is completed
            BottomBar(
                onBottomBarClick = {
                    coroutineScope.launch {
                        logger.w("answers:" + answers.value)
                        viewModel.submitDemographics(answers.value)
                        onNavigateTo(LemurScreen.Main.name)
                    }
                },
                isClickable = isCompleted.value
            )
        }
    }
}
/*
//@Preview
@Composable
fun DemographicsPreview() {
    Lemurs_AppTheme {
        DemographicsScreen(
            onNextButtonClicked = {},
            onHomeButtonClicked = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}*/