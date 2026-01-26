import androidx.compose.foundation.layout.add
import androidx.core.util.remove
import com.example.trueking.data.model.TradeProposal
import com.example.trueking.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class TradeRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : TradeRepository {
    private val proposalsCollection = firestore.collection("tradeProposals")

    // ... (implementaciones existentes)

    // --- IMPLEMENTACIÓN DE LAS NUEVAS FUNCIONES ---

    override suspend fun sendTradeProposal(proposal: TradeProposal): Resource<Unit> {
        return try {
            // Usamos el ID autogenerado por Firestore
            proposalsCollection.add(proposal).await()
            Resource.Success(Unit)
        } catch (e: java.lang.Exception) {
            Resource.Error(e.message ?: "Error al enviar la propuesta")
        }
    }

    override fun getReceivedProposals(userId: String): Flow<Resource<List<TradeProposal>>> =
        callbackFlow {
            // Buscamos propuestas donde el usuario actual es el receptor (receiverId)
            val snapshotListener = proposalsCollection
                .whereEqualTo("receiverId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Resource.Error(error.message ?: "Error al obtener propuestas"))
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val proposals = snapshot.toObjects(TradeProposal::class.java)
                        trySend(Resource.Success(proposals))
                    }
                }
            // Se cierra el listener cuando el Flow se cancela
            awaitClose { snapshotListener.remove() }
        }

    override suspend fun updateProposalStatus(
        proposalId: String,
        newStatus: String
    ): Resource<Unit> {
        return try {
            proposalsCollection.document(proposalId)
                .update("status", newStatus)
                .await()
            Resource.Success(Unit)
        } catch (e: java.lang.Exception) {
            Resource.Error(e.message ?: "Error al actualizar la propuesta")
        }
    }
}