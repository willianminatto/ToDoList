package br.edu.satc.todolistcompose.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Entity
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val descricao: String
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM Task")
    fun getAll(): Flow<List<Task>>

    @Insert
    suspend fun insert(task: Task)

    @Delete
    suspend fun delete(task: Task)
}

@Database(entities = [Task::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "task_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

class TaskRepository(private val dao: TaskDao) {
    fun getAll(): Flow<List<Task>> = dao.getAll()
    suspend fun add(task: Task) = dao.insert(task)
    suspend fun delete(task: Task) = dao.delete(task)
}

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository
    val tasks: Flow<List<Task>>

    init {
        val dao = AppDatabase.getDatabase(application).taskDao()
        repository = TaskRepository(dao)
        tasks = repository.getAll()
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            repository.add(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.delete(task)
        }
    }
}

enum class ThemeMode { LIGHT, DARK, AUTO }

class MainActivity : ComponentActivity() {
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val savedMode = prefs.getString("theme", "AUTO") ?: "AUTO"
        val themeMode = ThemeMode.valueOf(savedMode)

        setContent {
            var currentTheme by remember { mutableStateOf(themeMode) }

            CompositionLocalProvider(LocalThemeMode provides currentTheme) {
                val isDark = when (currentTheme) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                    ThemeMode.AUTO -> isSystemInDarkTheme()
                }

                MaterialTheme(
                    colorScheme = if (isDark) darkColorScheme() else lightColorScheme()
                ) {
                    MainScreen(
                        onThemeChange = {
                            currentTheme = it
                            prefs.edit().putString("theme", it.name).apply()
                        }
                    )
                }
            }
        }
    }
}

val LocalThemeMode = compositionLocalOf { ThemeMode.AUTO }

@Composable
fun MainScreen(viewModel: TaskViewModel = viewModel(factory = ViewModelFactory(LocalContext.current.applicationContext as Application)), onThemeChange: (ThemeMode) -> Unit) {
    val taskList by viewModel.tasks.collectAsState(initial = emptyList())
    var newTaskText by remember { mutableStateOf(TextFieldValue("")) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
        .verticalScroll(rememberScrollState())) {

        Text("To Do List", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))

        BasicTextField(
            value = newTaskText,
            onValueChange = { newTaskText = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(1.dp, Color.Gray)
                .padding(8.dp)
        )

        Button(
            onClick = {
                if (newTaskText.text.isNotBlank()) {
                    viewModel.addTask(Task(descricao = newTaskText.text))
                    newTaskText = TextFieldValue("")
                }
            },
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("Adicionar")
        }

        taskList.forEach { task ->
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(task.descricao)
                Button(onClick = { viewModel.deleteTask(task) }) {
                    Text("Remover")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { onThemeChange(ThemeMode.LIGHT) }) { Text("Claro") }
            Button(onClick = { onThemeChange(ThemeMode.DARK) }) { Text("Escuro") }
            Button(onClick = { onThemeChange(ThemeMode.AUTO) }) { Text("Auto") }
        }
    }
}

class ViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return TaskViewModel(app) as T
    }
}
