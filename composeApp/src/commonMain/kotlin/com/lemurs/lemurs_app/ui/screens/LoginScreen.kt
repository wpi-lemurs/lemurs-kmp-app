package com.lemurs.lemurs_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
//import org.jetbrains.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//import androidx.navigation.NavHostController
import com.lemurs.lemurs_app.data.api.MicrosoftApiAuthorizationService
import com.lemurs.lemurs_app.data.api.WebAPIAuthorizationService
import com.lemurs.lemurs_app.ui.theme.LemurBlue
import com.lemurs.lemurs_app.ui.theme.LemurBrightBlue
import com.lemurs.lemurs_app.ui.theme.LemurButtonBlue
import com.lemurs.lemurs_app.ui.theme.LemurDarkerGrey
import com.lemurs.lemurs_app.ui.theme.LemurGrey
import com.lemurs.lemurs_app.ui.theme.LemurLightBlue
import com.lemurs.lemurs_app.ui.theme.LemurWhite
import lemurs_app.composeapp.generated.resources.Res
import lemurs_app.composeapp.generated.resources.lemuricon
import lemurs_app.composeapp.generated.resources.microsoft_icon
import lemurs_app.composeapp.generated.resources.umass_logo2_g
import lemurs_app.composeapp.generated.resources.wpi_logo_g
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import com.lemurs.lemurs_app.util.UriOpener

@Composable
fun LoginScreen(
    onNavigateTo: (String) -> Unit = {},
    microsoftService: MicrosoftApiAuthorizationService,
    uriOpener: UriOpener
) {
    microsoftService.initClient(onNavigateTo)
//    val uriHandler = LocalUriHandler.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(LemurWhite, LemurBrightBlue)
                )
            )
            // Android only WindowInsets.safeDrawing removed
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            // Lemur Icon
            Box(
                modifier = Modifier
                    .sizeIn(maxWidth = 175.dp, maxHeight = 175.dp)
                    .background(Color.Unspecified, shape = RoundedCornerShape(32.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.lemuricon),
                    contentDescription = "Lemur Icon",
                    tint = LemurLightBlue,
                    modifier = Modifier
                        .fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Sign In To Your Account",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = LemurButtonBlue,
                    lineHeight = 40.sp,
                    fontSize = 40.sp
                ),
                modifier = Modifier.padding(top = 16.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Microsoft Login Button
            Button(
                onClick = { microsoftService.acquireToken() },
                colors = ButtonDefaults.buttonColors(containerColor = LemurWhite),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(4.dp),
                modifier = Modifier
                    .padding(8.dp)
                    .height(60.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.microsoft_icon),
                        contentDescription = "Microsoft Icon",
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(32.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "Sign in with Microsoft",
                        color = LemurDarkerGrey,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal, fontSize = 18.sp),
                        modifier = Modifier.align(Alignment.CenterVertically).fillMaxWidth(1f).wrapContentSize()
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            //Spacer(modifier = Modifier.height(48.dp))
            Column(
                modifier = Modifier
                    .fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.padding(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.wpi_logo_g),
                        contentDescription = "WPI logo",
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentHeight()
                    )

                    Spacer(modifier = Modifier.width(32.dp))

                    Icon(
                        painter = painterResource(Res.drawable.umass_logo2_g),
                        contentDescription = "UMass logo",
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentHeight()
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = LemurDarkerGrey,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    text = "Terms and Conditions",
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clickable { uriOpener.openUri("https://emutivo.wpi.edu/") }
                        .padding(8.dp)
                )
            }
        }
    }
}