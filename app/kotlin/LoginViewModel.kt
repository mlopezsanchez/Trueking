import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trueking.domain.use_case.auth.LoginUseCase
import com.example.trueking.util.Resource
import com.google.firebase.auth.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase // Inyectamos el caso de uso
) : ViewModel() {

    // --- Estados de los campos de texto ---
    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set

    fun onEmailChange(newEmail: String) {
        email = newEmail
    }

    fun onPasswordChange(newPassword: String) {
        password = newPassword
    }

    // --- Estado del proceso de login ---
    private val _loginState = MutableStateFlow<Resource<AuthResult>?>(null)
    val loginState = _loginState.asStateFlow()

    // --- Función para iniciar el login ---
    fun login() {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = Resource.Error("Por favor, completa todos los campos.")
            return
        }

        viewModelScope.launch {
            // 1. Indicar que el proceso está cargando
            _loginState.value = Resource.Loading()
            // 2. Llamar al caso de uso
            val result = loginUseCase(email, password)
            // 3. Actualizar el estado con el resultado
            _loginState.value = result
        }
    }

    // Función para limpiar el estado después de mostrar un mensaje
    fun resetLoginState() {
        _loginState.value = null
    }
}