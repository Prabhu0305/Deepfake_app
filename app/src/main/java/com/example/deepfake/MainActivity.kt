package com.example.deepfake

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.deepfake.ui.theme.DeepfakeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DeepfakeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DeepfakeApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun DeepfakeApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Initializing...") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var resultText by remember { mutableStateOf("") }

    val detector = remember { DeepfakeDetector(context) }

    LaunchedEffect(Unit) {
        status = "Loading models..."
        status = detector.loadModels()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        uri?.let {
            try {
                bitmap = if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source)
                }
                resultText = ""
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading image", e)
                status = "Error loading image"
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            detector.close()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Deepfake Detector",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(text = "Status: $status")
        
        Spacer(modifier = Modifier.height(16.dp))

        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Selected Image",
                modifier = Modifier
                    .size(250.dp)
                    .padding(8.dp)
            )
        } ?: Text("No image selected", modifier = Modifier.height(250.dp))

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { launcher.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select Image")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                bitmap?.let {
                    resultText = detector.analyze(it)
                }
            },
            enabled = status == "Models ready" && bitmap != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Detect Deepfake")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (resultText.isNotEmpty()) {
            Text(
                text = resultText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
