package com.lemurs.lemurs_app.ui.screens

//import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
//import androidx.navigation.NavController
import com.lemurs.lemurs_app.data.api.MicrosoftApiAuthorizationService
import com.lemurs.lemurs_app.data.api.WebAPIAuthorizationService
import com.lemurs.lemurs_app.microsoftService
import com.lemurs.lemurs_app.ui.theme.LemurDarkGrey
import com.lemurs.lemurs_app.ui.theme.LemurDarkerGrey
import com.lemurs.lemurs_app.ui.theme.LemurLightGrey
import com.lemurs.lemurs_app.ui.theme.LemurWhite
import kotlinx.coroutines.launch

//import com.lemurs.lemurs_app.ui.theme.Lemurs_AppTheme


val web = WebAPIAuthorizationService()
val microsoftService = MicrosoftApiAuthorizationService(web)

@Composable
fun ProfileScreen(
    onNavigateTo: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 16.dp),
                color = LemurDarkGrey
            )
            Spacer(modifier = Modifier.height(20.dp))
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "User Profile",
                modifier = Modifier
                    .size(128.dp),
                tint = LemurLightGrey
            )

        }
        Button(
            onClick = {
                microsoftService.removeAccount()
            },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .border(1.dp, LemurDarkerGrey, RoundedCornerShape(8.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = LemurWhite)
        ) {
            Text(
                text = "Logout",
                color = LemurDarkerGrey,
                style = MaterialTheme.typography.bodyLarge
            )

        }
    }
}

/*
//@Preview
@Composable
fun ProfilePreview() {
    Lemurs_AppTheme {
        ProfileScreen(
            onHomeButtonClicked = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}*/