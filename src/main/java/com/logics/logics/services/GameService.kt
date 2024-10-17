package com.logics.logics.services

import com.logics.logics.entities.GameRoom
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

data class GameState(
    val roomId: Long,
    val teamA: List<String>,
    val teamB: List<String>,
    var status: String = "IN_PROGRESS"
)

@Service
class GameService(private val gameRoomService: GameRoomService) {

    private val games = ConcurrentHashMap<Long, GameState>()
    private val logger = LoggerFactory.getLogger(GameService::class.java)

    fun startGameWithTeams(roomId: Long): Mono<GameState> {
        return gameRoomService.findById(roomId).flatMap { room ->
            val players = room.playerIds ?: emptyList()
            val (teamA, teamB) = divideIntoTeams(players)

            val gameState = GameState(roomId, teamA, teamB)
            games[roomId] = gameState

            logger.info("Игра в комнате $roomId начата. Команды: $teamA vs $teamB")
            Mono.just(gameState)
        }
    }

    private fun divideIntoTeams(players: List<String>): Pair<List<String>, List<String>> {
        val shuffledPlayers = players.shuffled()
        val mid = shuffledPlayers.size / 2
        return Pair(shuffledPlayers.take(mid), shuffledPlayers.drop(mid))
    }

    fun getGameState(roomId: Long): Mono<GameState> {
        return Mono.justOrEmpty(games[roomId])
    }
}