import com.google.firebase.Timestamp

// Representa una solicitud de un usuario (proposer) para intercambiar su
// objeto/habilidad (proposerTradeId) por el de otro usuario (receiverTradeId).
data class TradeProposal(
    val id: String = "", // ID único de la propuesta

    // IDs de los anuncios de trueque involucrados
    val proposerTradeId: String = "", // ID del anuncio del que propone
    val receiverTradeId: String = "", // ID del anuncio del que recibe la propuesta

    // IDs de los usuarios
    val proposerId: String = "", // ID del usuario que envía la propuesta
    val receiverId: String = "", // ID del usuario que la recibe

    val status: String = ProposalStatus.PENDING.name, // Estado: PENDING, ACCEPTED, REJECTED
    val createdAt: Timestamp = Timestamp.now()
)

// Usamos un enum para tener estados consistentes y evitar errores de tipeo.
enum class ProposalStatus {
    PENDING,  // Pendiente de respuesta
    ACCEPTED, // Aceptada
    REJECTED  // Rechazada
}