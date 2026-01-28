package com.lemurs.lemurs_app.ui.reusableComponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lemurs.lemurs_app.survey.Questions
import com.lemurs.lemurs_app.survey.SurveyQuestion
import com.lemurs.lemurs_app.ui.theme.LemurBlack
import com.lemurs.lemurs_app.ui.theme.LemurDarkGrey
import com.lemurs.lemurs_app.ui.theme.LemurGrey
import com.lemurs.lemurs_app.ui.theme.Typography

@Composable
fun QuestionFactory(
    question: Questions,
    onAnswerSelected: (String) -> Unit // Callback function to handle the selected answer
) {
    Column {
        val questionFormatted = SurveyQuestion(
            id = question.id,
            question = question.question,
            style = question.style,
            options = question.options
        )
        // Depending on the question type, we will render different components
        when (question.style) { // "open-response", "5-scale", "4-scale", "yes-no", "dropdown","time-picker"
            "category" -> CategoryHeader(questionFormatted)
            "parent-question" -> ParentQuestionHeader(questionFormatted)
            "parent-question-custom-4-scale" -> ParentQuestionHeader(questionFormatted)
            "custom-5-scale" -> RadioButtonQuestion(questionFormatted, onAnswerSelected)
            "5-scale" -> RadioButtonQuestion(
                questionFormatted,
                onAnswerSelected
            ) // 5 bubble options
            "5-scale-much" -> RadioButtonQuestion(questionFormatted, onAnswerSelected)     // 1 (not at all) – 5 (very much)
            "5-scale-intense" -> RadioButtonQuestion(questionFormatted, onAnswerSelected)  // 1 (not strong at all) – 5 (very intense)
            "5-scale-time" -> RadioButtonQuestion(questionFormatted, onAnswerSelected)     // 1 = not at all; 5 = all the time
            "4-scale-true" -> RadioButtonQuestion(questionFormatted, onAnswerSelected)     // 1 = not true at all, 4 = very true
            "4-scale-difficult" -> RadioButtonQuestion(questionFormatted, onAnswerSelected)// 0, Not difficult at all; 1, Somewhat difficult; 2, Very difficult; 3, Extremely difficult

            "5-scale-sleep" -> RadioButtonQuestion(questionFormatted, onAnswerSelected)    // 1 = very bad night of sleep; 5 = very good night of sleep
            "5-scale-zero" -> RadioButtonQuestion(questionFormatted, onAnswerSelected)
            "custom-4-scale" -> RadioButtonQuestion(questionFormatted, onAnswerSelected)
            "4-scale" -> RadioButtonQuestion(
                questionFormatted,
                onAnswerSelected
            ) // 4 bubble options
            "4-scale-zero" -> RadioButtonQuestion(questionFormatted, onAnswerSelected)     // 0, Not at all; 1, Several days; 2, More than half the days; 3, Nearly every day
            "open-response" -> TextEntryQuestion(questionFormatted, onAnswerSelected)
            "time-picker" -> TimePickerQuestion(questionFormatted, onAnswerSelected)
            "yes-no" -> RadioButtonQuestion(
                questionFormatted,
                onAnswerSelected
            ) // two bubble options
            "dropdown" -> DropdownQuestion(questionFormatted, onAnswerSelected)
            else -> {
                // Default case for unsupported question types
                Text(text = "Unsupported question type.")
            }
        }
    }
}

@Composable
fun CategoryHeader(question: SurveyQuestion) {
    Text(
        question.question,
        style = Typography.headlineLarge.copy(fontSize = 40.sp, lineHeight =  50.sp),
        modifier = Modifier.padding(top = 24.dp, bottom = 16.dp),
        color = LemurDarkGrey,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun ParentQuestionHeader(question: SurveyQuestion) {
    Text(
        question.question,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Start,
            fontSize = 18.sp
        ),
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
fun RadioButtonQuestion(
    question: SurveyQuestion,
    onAnswerSelected: (String) -> Unit
) {
    // Render radio buttons for multiple options
    Column {
        Row {
            var answerList: List<String> = listOf("")
            if (question.style == "5-scale") {
                answerList = listOf("Not at\nAll", "2", "3", "4", "Very\nMuch")
            } else if (question.style == "4-scale") {
                answerList = listOf("1", "2", "3", "4")
//            } else if (question.style == "5-scale-zero") {
//                answerList = listOf("0", "1", "2", "3", "4")
//            } else if (question.style == "4-scale-zero") {
//                answerList = listOf("0", "1", "2", "3")
            } else if (question.style == "yes-no") {
                answerList = listOf("Yes", "No")
            } else if (question.style == "custom-5-scale" || question.style == "custom-4-scale") {
                val out = ArrayList<String>()
                for (i in 0..<question.options!!.size) {
                    out.add(question.options!![i] ?: i.toString())
                }
                answerList = out
            } else if (question.style == "5-scale-much") {
                answerList = listOf("1\nnot at all", "2", "3", "4", "5\nvery much")
            }else if (question.style == "5-scale-intense") {
                answerList = listOf("1\nnot strong at all", "2", "3", "4", "5\nvery intense")
            }else if (question.style == "5-scale-time") {
                answerList = listOf("1\nnot at all", "2", "3", "4", "5\nall the time")
            }else if (question.style == "5-scale-sleep") {
                answerList = listOf("1\nvery bad night of sleep", "2", "3", "4", "5\nvery good night of sleep")
            }else if (question.style == "4-scale-zero") {
                answerList = listOf("not at all", "several days", "more than half the days", "nearly every day")
            }else if (question.style == "4-scale-difficult") {
                answerList = listOf("not difficult at all", "somewhat difficult", "very difficult", "extremely difficult")
            }else if (question.style == "4-scale-true") {
                answerList = listOf("1\nnot true at all", "2", "3", "4\nvery true")
            }
            BubbleSelectionQuestion(
                question.question,
                answerList.size,
                answerList,
                onAnswerSelected
            )
        }
    }
}

@Composable
fun TextEntryQuestion(
    question: SurveyQuestion,
    onAnswerSelected: (String) -> Unit
) {
    // Render a text input field for text-entry questions
    var answer = remember { "" }

    TextInputQuestion(
        question.question,
        "Your Answer",
        onAnswerSelected
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownQuestion(
    question: SurveyQuestion,
    onAnswerSelected: (String) -> Unit
) {
    // Render a dropdown menu for selection
    val selectedOption = remember { mutableStateOf("") }
    // Note: the ExposedDropDownMenuBox object is from an experimental library, and is necessary to get the desired look and functionality in the UI
    //     ExposedDropdownMenuBox(expanded = false, onExpandedChange = {}) {
    //         OutlinedTextField(
    //             value = selectedOption.value,
    //             onValueChange = { selectedOption.value = it },
    //             label = { Text("Select an option") },
    //             readOnly = true,
    //             trailingIcon = {
    //                 ExposedDropdownMenuDefaults.TrailingIcon(expanded = false)
    //             }
    //         )
    ExposedDropdownMenuBox(expanded = false, onExpandedChange = {}) {
        question.options?.forEach { option ->
            option?.let {
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        selectedOption.value = option
                        onAnswerSelected(option) // Invoke callback when an option is selected
                    }
                )
            }
        }
    }
}

@Composable
fun DropDownSelectionQuestion(
    questionText: String,
    answerChoices: List<String>,
    onAnswerSelected: () -> Unit
) {
    var selectedAnswer by rememberSaveable { mutableStateOf<String?>(null) } // Holds the selected answer

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp) // Adjust padding between questions
    ) {
        // Question text
        Text(
            text = questionText,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(bottom = 4.dp),
            textAlign = TextAlign.Start,
            color = LemurBlack
        )

        // Dropdown for answer choices
        CustomDropdownMenuBox(
            items = answerChoices,
            selectedItem = selectedAnswer,
            onItemSelected = { selected ->
                selectedAnswer = selected
                onAnswerSelected()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun TextInputQuestion(questionText: String, answerLabel: String, onAnswerFilled: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = Modifier
            .padding(vertical = 16.dp)
            .fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        )
        {
            Text(
                text = questionText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(bottom = 4.dp),
                textAlign = TextAlign.Start,
                color = LemurBlack
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            SimpleOutlinedTextField(label = answerLabel,
                value = text,
                onValueChange = { newText ->
                    text = newText
                    onAnswerFilled(text)
                })
        }
    }
}

@Composable
fun BubbleSelectionQuestion(
    questionText: String,
    numberOfOptions: Int,
    options: List<Any>,
    onOptionSelected: (String) -> Unit
) {
    var selectedOption by rememberSaveable { mutableStateOf<Any?>(null) }
    if (numberOfOptions > options.size) {
        throw IllegalArgumentException("Number of options cannot be greater than the number of options provided")
    }
    Column(
        modifier = Modifier
            .padding(vertical = 16.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = questionText,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
            modifier = Modifier.padding(bottom = 16.dp),
            textAlign = TextAlign.Start,
            color = LemurBlack
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            for (i in 0 until (options.size)) {
                val option = options[i]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    RadioButton(
                        selected = selectedOption == option,
                        onClick = {
                            selectedOption = option
                            onOptionSelected(i.toString())
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = option.toString(),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 14.sp),
                        textAlign = TextAlign.Center,
                        color = LemurGrey
                    )
                }
            }
        }
    }
}

@Composable
fun VerticalBubbleQuestionWithOtherText(
    questionText: String,
    numberOfOptions: Int,
    options: List<Any>,
    otherHeader: String,
    otherLabel: String,
    onOptionSelected: (String, String) -> Unit
) {
    var selectedOption by rememberSaveable { mutableStateOf<Any?>(null) }
    var otherText by rememberSaveable { mutableStateOf("") }
    var otherSelected = false
    if (numberOfOptions > options.size) {
        throw IllegalArgumentException("Number of options cannot be greater than the number of options provided")
    }
    Column(
        modifier = Modifier
            .padding(vertical = 16.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = questionText,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(bottom = 4.dp),
            textAlign = TextAlign.Start,
            color = LemurBlack
        )
//        Row(
//            horizontalArrangement = Arrangement.spacedBy(8.dp),
//            verticalAlignment = Alignment.Top,
//            modifier = Modifier.fillMaxWidth()
//        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        )
        {
            for (i in 0 until (options.size)) {
                val option = options[i]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = selectedOption == option,
                        onClick = {
                            selectedOption = option
                            onOptionSelected(i.toString(), "")
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = option.toString(),
//                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 14.sp),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal, lineHeight = 20.sp, textIndent = TextIndent(firstLine = 15.sp)),
//                        textAlign = TextAlign.Center,
                        color = LemurGrey
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = selectedOption == otherHeader,
                        onClick = {
                            selectedOption = otherHeader
                            onOptionSelected(numberOfOptions.toString(), otherText)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = otherHeader,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal, lineHeight = 20.sp, textIndent = TextIndent(firstLine = 15.sp)),
                        color = LemurGrey
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OtherTextField(
                        label = otherLabel,
                        value = otherText,
                        onValueChange = { newText ->
                            otherText = newText
                            onOptionSelected(numberOfOptions.toString(), otherText)
                        }
                    )
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerQuestion(
    question: SurveyQuestion,
    onAnswerSelected: (String) -> Unit
) {
    var selectedHour by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedMinute by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPeriod by rememberSaveable { mutableStateOf<String?>(null) }

    val hours = (1..12).toList()
    val minutes = (0..59).toList().map { it.toString().padStart(2, '0') }
    val periods = listOf("AM", "PM")

    // Function to format and send the answer only when all components are selected
    fun sendAnswerIfComplete() {
        if (selectedHour != null && selectedMinute != null && selectedPeriod != null) {
            val formattedHour = selectedHour.toString().padStart(2, '0')
            val formatted = "$formattedHour:$selectedMinute $selectedPeriod"
            onAnswerSelected(formatted)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = question.question,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hour Dropdown
            DropdownSelector(
                label = "Hour",
                options = hours.map { it.toString() },
                selected = selectedHour?.toString() ?: "",
                onSelected = {
                    selectedHour = it.toInt()
                    sendAnswerIfComplete()
                }
            )

            // Minute Dropdown
            DropdownSelector(
                label = "Minute",
                options = minutes,
                selected = selectedMinute ?: "",
                onSelected = {
                    selectedMinute = it
                    sendAnswerIfComplete()
                }
            )

            // AM/PM Dropdown
            DropdownSelector(
                label = "Period",
                options = periods,
                selected = selectedPeriod ?: "",
                onSelected = {
                    selectedPeriod = it
                    sendAnswerIfComplete()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSelector(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier.menuAnchor().width(90.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}