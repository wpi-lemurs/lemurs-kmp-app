package com.lemurs.lemurs_app.ui.reusableComponents

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lemurs.lemurs_app.ui.theme.LemurWhite
import com.lemurs.lemurs_app.ui.theme.filledButtonColors


@Composable
fun StandardButton(
    buttonText: String,
    onClickValue: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    //onClickValue should look like the following: navController.navigate("survey_screen")
    Button(
        onClick = onClickValue,
        enabled = enabled,
        colors = filledButtonColors(),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
    ) {
        Text(
            text = buttonText,
            style = MaterialTheme.typography.bodyLarge,
            color = LemurWhite
        )
    }
}
