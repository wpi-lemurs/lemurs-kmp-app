package com.lemurs.lemurs_app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
import com.lemurs.lemurs_app.ui.reusableComponents.EarningsBreakdownCard
import com.lemurs.lemurs_app.ui.theme.LemurButtonBlue
import com.lemurs.lemurs_app.ui.theme.LemurButtonGrey
import com.lemurs.lemurs_app.ui.theme.LemurDarkGrey
import com.lemurs.lemurs_app.ui.theme.LemurDarkerGrey
import com.lemurs.lemurs_app.ui.theme.LemurWhite
import com.lemurs.lemurs_app.ui.viewmodel.SubmissionViewModel
import com.lemurs.lemurs_app.ui.viewmodel.WeeklyQuestionsViewModel
import com.lemurs.lemurs_app.ui.viewmodel.WeeklySurveyViewModel
import lemurs_app.composeapp.generated.resources.Res
import org.koin.compose.koinInject


@Composable
fun SubmissionScreen(
    onNavigateTo: (String) -> Unit,
    submissionViewModel: SubmissionViewModel = koinInject(),
    weeklyQuestionsViewModel: WeeklyQuestionsViewModel = koinInject()
) {
    val surveyItems by submissionViewModel.surveyItems.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Content: Centered "This Submission" and the Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "This Submission",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp),
                color = LemurDarkGrey,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(50.dp))
            EarningsBreakdownCard(items = surveyItems)
        }
        Box(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Button(
                onClick = {
                    onNavigateTo(LemurScreen.Main.name)
                    weeklyQuestionsViewModel.clearRequests()
                          },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LemurButtonBlue,
                    disabledContainerColor = LemurButtonGrey,
                    disabledContentColor = LemurDarkerGrey
                ),
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(8.dp))
                    .height(36.dp)
                    .fillMaxWidth(0.8f)
                    .align(Alignment.Center)
                //.padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Return Home",
                    color = LemurWhite,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    textAlign = TextAlign.Center
                )
            }
        }
        //Spacer(modifier = Modifier.height(50.dp))
    }
}

