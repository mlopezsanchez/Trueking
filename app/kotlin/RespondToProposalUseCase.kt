import com.example.trueking.data.model.ProposalStatus
import com.example.trueking.data.repository.TradeRepository
import javax.inject.Inject

class RespondToProposalUseCase @Inject constructor(
    private val repository: TradeRepository
) {
    suspend operator fun invoke(proposalId: String, accepted: Boolean) {
        val newStatus = if (accepted) ProposalStatus.ACCEPTED.name else ProposalStatus.REJECTED.name
        repository.updateProposalStatus(proposalId, newStatus)
    }
}