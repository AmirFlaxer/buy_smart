package com.amir.buysmart.presentation.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun VoiceInputButton(
    onResult: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }

    val recognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            recognizer.startListeningHebrew()
            isListening = true
        }
    }

    DisposableEffect(Unit) {
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onError(error: Int) { isListening = false }
            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                isListening = false
                if (!text.isNullOrBlank()) onResult(text)
            }
        })
        onDispose { recognizer.destroy() }
    }

    // אנימציית פעפוע כשמקליט
    val infiniteTransition = rememberInfiniteTransition(label = "mic")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            tween(550, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = {
                if (isListening) {
                    recognizer.stopListening()
                    isListening = false
                } else {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        recognizer.startListeningHebrew()
                        isListening = true
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = if (isListening) "עצור הקלטה" else "הקלטה קולית",
                tint = if (isListening) Color.Red.copy(alpha = alpha)
                       else LocalContentColor.current
            )
        }
        AnimatedVisibility(visible = isListening) {
            Text(
                text = "לחץ לסיום",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Red,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}

private fun SpeechRecognizer.startListeningHebrew() {
    startListening(
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "iw-IL")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
    )
}
