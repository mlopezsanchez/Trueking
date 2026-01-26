import com.example.trueking.data.model.TradeProposal
import com.example.trueking.util.Resource
import kotlinx.coroutines.flow.Flow

interface TradeRepository {
    // ... (funciones existentes como getTrades)// --- NUEVAS FUNCIONES PARA PROPUESTAS DE TRUEQUE ---

    // Enviar una nueva propuesta de trueque
    suspend fun sendTradeProposal(proposal: TradeProposal): Resource<Unit>

    // Obtener las propuestas recibidas por un usuario en tiempo real
    fun getReceivedProposals(userId: String): Flow<Resource<List<TradeProposal>>>

    // Actualizar el estado de una propuesta (Aceptar o Rechazar)
    suspend fun updateProposalStatus(proposalId: String, newStatus: String): Resource<Unit>
}