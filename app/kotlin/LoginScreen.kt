import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.password
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.trueking.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel()
) {
    // --- Observar el estado del login desde el ViewModel ---
    val loginState by viewModel.loginState.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TextField(
                value = viewModel.email,
                onValueChange = { viewModel.onEmailChange(it) },
                label = { Text("Email") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = viewModel.password,
                onValueChange = { viewModel.onPasswordChange(it) },
                label = { Text("Contraseña") }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { viewModel.login() }) {
                Text("Iniciar Sesión")
            }
        }

        // --- Manejar los diferentes estados del login ---
        LaunchedEffect(loginState) {
            when (val state = loginState) {
                is Resource.Loading -> {
                    // El CircularProgressIndicator se muestra abajo
                }

                is Resource.Success -> {
                    Toast.makeText(context, "¡Login exitoso!", Toast.LENGTH_SHORT).show()
                    // Aquí navegarías a la pantalla principal
                    // navController.navigate("main_screen")
                    viewModel.resetLoginState()
                }

                is Resource.Error -> {
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                    viewModel.resetLoginState()
                }

                null -> {
                    // Estado inicial, no hacer nada
                }
            }
        }

        // Mostrar indicador de carga si el estado es Loading
        if (loginState is Resource.Loading) {
            CircularProgressIndicator()
        }
    }
}