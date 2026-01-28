package com.lemurs.lemurs_app.ui.reusableComponents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.lemurs.lemurs_app.ui.theme.LemurDarkerGrey
import com.lemurs.lemurs_app.ui.theme.LemurWhite
import com.lemurs.lemurs_app.ui.theme.Typography

@Composable
fun <T> CustomDropdownMenuBox(
    items: List<T>,
    onItemSelected: (T) -> Unit, // Callback when an item is selected
    selectedItem: T?,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable { expanded = !expanded }
            .background(LemurWhite, RoundedCornerShape(8.dp))
            .border(1.dp, LemurDarkerGrey, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        // Selected Item Display
        Text(
            text = selectedItem?.toString() ?: "Select an item",
            fontSize = 16.sp,
            color = LemurDarkerGrey
        )

        // Dropdown Popup
        if (expanded) {
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { expanded = false }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        //.background(Color.White, RoundedCornerShape(8.dp))
                        //.border(1.dp, LemurDarkerGrey, RoundedCornerShape(8.dp))
                        .padding(horizontal = 24.dp, vertical = 36.dp)
                ) {
                    LazyColumn (modifier = Modifier.shadow(8.dp, RoundedCornerShape(8.dp)).background(Color.White, RoundedCornerShape(8.dp)).border(1.dp, LemurDarkerGrey, RoundedCornerShape(8.dp)).padding(12.dp)){
                        items(items) { item ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expanded = false
                                        onItemSelected(item)
                                    }
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = item.toString(),
                                    fontSize = Typography.bodyLarge.fontSize,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun <T> onItemSelected(item: T) {
//fill this in
}
