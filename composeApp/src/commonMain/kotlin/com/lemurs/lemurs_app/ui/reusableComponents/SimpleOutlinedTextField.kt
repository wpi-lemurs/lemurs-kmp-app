package com.lemurs.lemurs_app.ui.reusableComponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lemurs.lemurs_app.ui.theme.LemurDarkerGrey
import com.lemurs.lemurs_app.ui.theme.inputFieldColors

@Composable
fun SimpleOutlinedTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                label,
                color = LemurDarkerGrey
            )
        },
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = inputFieldColors(),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
fun OtherTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                label,
                color = LemurDarkerGrey,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
            )
        },
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = inputFieldColors(),
        shape = RoundedCornerShape(8.dp)
    )

}
