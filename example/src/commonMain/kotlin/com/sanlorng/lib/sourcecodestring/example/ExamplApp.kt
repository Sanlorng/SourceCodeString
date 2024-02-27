package com.sanlorng.lib.sourcecodestring.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sanlorng.lib.generated.Source
import com.sanlorng.lib.generated.codeOfExampleApp
import com.sanlorng.lib.generated.stringTemplateSourceCode
import com.sanlorng.lib.sourcecodestring.annotation.Sample

@Composable
@Sample
fun ExampleApp() {
    val source = remember { Source() }
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
        ) {
            var showSourceCode by remember {
                mutableStateOf(false)
            }
            Button(
                onClick = {
                    showSourceCode = true
                }
            ) {
                Text("Show Source Code")
            }
            if (showSourceCode) {
                AlertDialog(
                    onDismissRequest = {
                        showSourceCode = false
                    },
                    confirmButton = {
                        TextButton(onClick = { showSourceCode = false }) {
                            Text("Close")
                        }
                    },
                    text = {
                        Box(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                            Text(source.codeOfExampleApp)
                        }
                    }
                )
            }

            Text("sourceCodeOfStringTemplateTest:\n${source.stringTemplateSourceCode}")
        }
    }
}

@Sample("stringTemplate", nameTemplate = "%sSourceCode", upperFirstChar = "false", inline = "false")
fun stringTemplateTest() {
    val test1 = "test"
    val test2 = "test with: $test1"
    val test3 = "test with: ${test2.first()}"
    val test4 = """test with $test3"""
}