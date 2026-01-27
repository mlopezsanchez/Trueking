import com.example.trueking.data.model.User
import com.example.trueking.util.Resource
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    override suspend fun login(email: String, pass: String): Resource<AuthResult> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, pass).await()
            Resource.Success(result)
        } catch (e: java.lang.Exception) {
            Resource.Error(e.message ?: "Error desconocido al iniciar sesión")
        }
    }

    override suspend fun register(name: String, email: String, pass: String): Resource<AuthResult> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
            result.user?.let { firebaseUser ->
                // Guardar información adicional del usuario en Firestore
                val user = User(id = firebaseUser.uid, name = name, email = email)
                firestore.collection("users").document(firebaseUser.uid).set(user).await()
            }
            Resource.Success(result)
        } catch (e: java.lang.Exception) {
            Resource.Error(e.message ?: "Error desconocido durante el registro")
        }
    }

    override fun logout() {
        firebaseAuth.signOut()
    }
}