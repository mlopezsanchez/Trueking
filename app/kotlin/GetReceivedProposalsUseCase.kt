import com.example.trueking.data.repository.TradeRepository
import javax.inject.Inject

class GetReceivedProposalsUseCase @Inject constructor(
    private val repository: TradeRepository
) {
    // El operador invoke permite llamar a la clase como si fuera una función
    operator fun invoke(userId: String) = repository.getReceivedProposals(userId)
}