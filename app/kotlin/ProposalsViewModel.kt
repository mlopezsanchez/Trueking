import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trueking.data.model.TradeProposal
import com.example.trueking.domain.use_case.trades.GetReceivedProposalsUseCase
import com.example.trueking.domain.use_case.trades.RespondToProposalUseCase
import com.example.trueking.util.Resource
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProposalsViewModel @Inject constructor(
    private val getReceivedProposalsUseCase: GetReceivedProposalsUseCase,
    private val respondToProposalUseCase: RespondToProposalUseCase,
    private val auth: FirebaseAuth // Para obtener el ID del usuario actual
) : ViewModel() {

    private val _proposalsState =
        MutableStateFlow<Resource<List<TradeProposal>>>(Resource.Loading())
    val proposalsState: StateFlow<Resource<List<TradeProposal>>> = _proposalsState

    init {
        // Cargar las propuestas en cuanto el ViewModel se inicializa
        loadProposals()
    }

    private fun loadProposals() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            _proposalsState.value = Resource.Error("Usuario no autenticado.")
            return
        }

        getReceivedProposalsUseCase(currentUserId)
            .onEach { result ->
                _proposalsState.value = result
            }.launchIn(viewModelScope)
    }

    fun acceptProposal(proposalId: String) {
        viewModelScope.launch {
            respondToProposalUseCase(proposalId, accepted = true)
            // Opcional: podrías añadir un estado para mostrar un Toast de éxito/error
        }
    }

    fun rejectProposal(proposalId: String) {
        viewModelScope.launch {
            respondToProposalUseCase(proposalId, accepted = false)
        }
    }
}