import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing
import org.springframework.data.domain.ReactiveAuditorAware
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Configuration
@EnableR2dbcAuditing
class R2dbcAuditingConfiguration {
    @Bean
    fun reactiveAuditorAware(): ReactiveAuditorAware<LocalDateTime> {
        return ReactiveAuditorAware { Mono.just(LocalDateTime.now()) }
    }
}