import com.google.firebase.Timestamp

data class Trade(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val category: String = "", // "Objeto" o "Habilidad"
    val lookingFor: String = "",
    val createdAt: Timestamp = Timestamp.now()
)