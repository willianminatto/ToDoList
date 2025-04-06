package br.edu.satc.todolistcompose

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.edu.satc.todolistcompose.ui.theme.ToDoListComposeTheme

enum class AppTheme { LIGHT, DARK, SYSTEM }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)

            var selectedTheme by remember {
                mutableStateOf(loadThemePreference(prefs))
            }

            ToDoListComposeTheme(
                darkTheme = when (selectedTheme) {
                    AppTheme.DARK -> true
                    AppTheme.LIGHT -> false
                    AppTheme.SYSTEM -> isSystemInDarkTheme()
                }
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ThemeToggle(
                            selectedTheme = selectedTheme,
                            onThemeChange = {
                                selectedTheme = it
                                saveThemePreference(prefs, it)
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Aqui chama a sua tela principal com a lista de tarefas
                        br.edu.satc.todolistcompose.ui.screens.HomeScreen()
                    }
                }
            }
        }
    }

    private fun saveThemePreference(prefs: SharedPreferences, theme: AppTheme) {
        prefs.edit().putString("theme", theme.name).apply()
    }

    private fun loadThemePreference(prefs: SharedPreferences): AppTheme {
        val name = prefs.getString("theme", AppTheme.SYSTEM.name)
        return AppTheme.valueOf(name ?: AppTheme.SYSTEM.name)
    }
}

@Composable
fun ThemeToggle(
    selectedTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit
) {
    Column {
        Text("Escolha o tema:")
        Row {
            Button(onClick = { onThemeChange(AppTheme.LIGHT) }, modifier = Modifier.padding(4.dp)) {
                Text("Claro")
            }
            Button(onClick = { onThemeChange(AppTheme.DARK) }, modifier = Modifier.padding(4.dp)) {
                Text("Escuro")
            }
            Button(onClick = { onThemeChange(AppTheme.SYSTEM) }, modifier = Modifier.padding(4.dp)) {
                Text("Automático")
            }
        }
    }
}
