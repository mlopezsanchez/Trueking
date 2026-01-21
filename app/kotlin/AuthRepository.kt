import com.example.trueking.util.Resource
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser

interface AuthRepository {
    val currentUser: FirebaseUser?
    suspend fun login(email: String, pass: String): Resource<AuthResult>
    suspend fun register(name: String, email: String, pass: String): Resource<AuthResult>
    fun logout()
}