import com.example.trueking.data.repository.AuthRepository
import com.example.trueking.data.repository.AuthRepositoryImpl
import com.example.trueking.data.repository.TradeRepository
import com.example.trueking.data.repository.TradeRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindTradeRepository(impl: TradeRepositoryImpl): TradeRepository
}