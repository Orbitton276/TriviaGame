package com.trivia.multi.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.trivia.multi.R
import java.util.Locale

object Common {

    @Composable
    fun Brush(): Brush {
        val gradientBrush = Brush.horizontalGradient(
//            colors = listOf(Color(0xff1553f6), Color(0xff542dd6)) // Purple to Teal
                            colors = listOf(Color(0xffb66cd1), Color(0xff1e9bb7)) // Lavender to Teal
//            colors = listOf(Color(0xff9b44e6), Color(0xff78c6e3)) // Bright Purple to Light Blue

        )
        return gradientBrush
    }


    @Composable
    fun GameButton(
        text: String,
        modifier: Modifier = Modifier,
        isLoading: Boolean = false, // New parameter for showing the progress bar
        onClick: () -> Unit = {}
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .clickable(enabled = !isLoading) { // Disable clicks when loading
                    onClick()
                }
                .padding(vertical = 12.dp, horizontal = 16.dp)
                .background(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = 2.dp,
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(vertical = 12.dp, horizontal = 16.dp) // Inner padding
        ) {
            if (isLoading) {
                GameLoadingAnimation(Modifier.size(24.dp))
            } else {
                Text(
                    text = text.uppercase(Locale.getDefault()),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center
                )
            }
        }
    }


    @Composable
    fun GameTitle(text: String, modifier: Modifier = Modifier) {
        Text(
            text = text.uppercase(Locale.getDefault()),
//            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontFamily = FontFamily.Serif,
            textAlign = TextAlign.Center,
            style = TextStyle(fontSize = 48.sp),
            modifier = modifier
                .padding(vertical = 12.dp, horizontal = 16.dp) // Adds padding around the text
                .background(
                    color = Color.Transparent, // Transparent background for the outline
                    shape = RoundedCornerShape(8.dp) // Rounded corners
                )
                .padding(vertical = 12.dp, horizontal = 16.dp) // Adds padding inside the button
                .wrapContentHeight()
        )
    }

    @Composable
    fun GameText(text: String, fontSize: TextUnit = 24.sp, modifier: Modifier = Modifier) {
        Text(
            text = text.uppercase(Locale.getDefault()),
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
                color = Color.White
            ),

            modifier = modifier
                .padding(vertical = 5.dp, horizontal = 10.dp) // Adds padding around the text
                .background(
                    color = Color.Transparent, // Transparent background for the outline
                    shape = RoundedCornerShape(8.dp) // Rounded corners
                )
                .padding(vertical = 5.dp, horizontal = 10.dp) // Adds padding inside the button
        )
    }

    @Composable
    fun GameListText(text: String, fontSize: TextUnit = 24.sp, modifier: Modifier = Modifier) {
        Text(
            text = text.uppercase(Locale.getDefault()),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontFamily = FontFamily.Serif,
            modifier = modifier
//                .fillMaxWidth()
                .background(
                    color = Color.Transparent, // Transparent background for the outline
                    shape = RoundedCornerShape(8.dp) // Rounded corners
                )

        )
    }

    @Composable
    fun GameQuestionButton(
        text: String,
        enabled: Boolean,
        selectedAnswer: Boolean,
        correct: Boolean,
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {}
    ) {
        Text(
            text = text.uppercase(Locale.getDefault()),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Color.White else Color.DarkGray,
            fontFamily = FontFamily.Serif,
            textAlign = TextAlign.Center,
            modifier = modifier
                .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp) // Adds padding around the text
                .background(
                    color = Color.Transparent, // Transparent background for the outline
                    shape = RoundedCornerShape(8.dp) // Rounded corners
                )
                .border(
                    width = 2.dp, // Border thickness
                    color =
                    when {
                        selectedAnswer && correct -> Color.Green
                        selectedAnswer && !correct -> Color.Red
                        else -> if (enabled) Color.White else Color.DarkGray
                    },

                    shape = RoundedCornerShape(8.dp) // Border with rounded corners
                )
                .padding(vertical = 12.dp, horizontal = 16.dp) // Adds padding inside the button
        )
    }


    @Composable
    fun GameTextField(name: String, isValid: Boolean = true, onNameChange: (String) -> Unit) {
        OutlinedTextField(
            value = name,
            isError = !isValid,
            onValueChange = { onNameChange(it) },
            label = {
                Text(
                    text = "Enter your name",
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center
                )
            },
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedSupportingTextColor = Color.White,
                unfocusedLabelColor = Color.White,
                focusedLabelColor = Color.White,
                focusedContainerColor = Color(0xff4e0d91).copy(alpha = 0.3f),
                unfocusedContainerColor = Color(0xff3a7c83).copy(alpha = 0.3f),
                errorContainerColor = Color.Red,
            ),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(6.dp))
        )
    }

    @Composable
    fun GameLoadingAnimation(modifier: Modifier = Modifier) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading_lottie))

        LottieAnimation(
            composition = composition,
            speed = 0.8f,
            iterations = LottieConstants.IterateForever,
            modifier = modifier
        )
    }
}