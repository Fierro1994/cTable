package com.logics.logics.services

import com.logics.logics.entities.GameState
import com.logics.logics.repositories.GameRoomRepository
import com.logics.logics.repositories.GameStateRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class GameService(
    private val gameStateRepository: GameStateRepository,
    private val gameRoomRepository: GameRoomRepository
) {

    private val logger = LoggerFactory.getLogger(GameService::class.java)

    // Метод, который проверяет наличие состояния игры и создает его, если оно отсутствует
    fun startGame(roomId: Long): Mono<GameState> {
        return gameStateRepository.findByRoomId(roomId)
            .switchIfEmpty(
                gameRoomRepository.findById(roomId)
                    .flatMap { room ->
                        val players = room.playerIds?.toList() ?: emptyList()
                        val teamA = players.subList(0, players.size / 2)
                        val teamB = players.subList(players.size / 2, players.size)

                        val gameState = GameState(
                            roomId = room.id ?: 0L,
                            teamA = teamA,
                            teamB = teamB,
                            category = room.category,
                            teamAScore = 0,
                            teamBScore = 0,
                            status = "IN_PROGRESS"
                        )

                        gameStateRepository.save(gameState)
                    }
            )
    }

    fun saveGameState(gameState: GameState): Mono<GameState> {
        return gameStateRepository.save(gameState)
    }

    fun getGameState(roomId: Long): Mono<GameState> {
        return gameStateRepository.findByRoomId(roomId)
    }

    // Метод для получения списка игроков команды (teamA или teamB) в зависимости от имени игрока
    fun getTeamPlayers(roomId: Long, username: String): Mono<List<String>> {
        return getGameState(roomId).map { gameState ->
            val cleanedTeamA = cleanPlayerNames(gameState.teamA)
            val cleanedTeamB = cleanPlayerNames(gameState.teamB)

            if (cleanedTeamA.contains(username)) {
                cleanedTeamA
            } else if (cleanedTeamB.contains(username)) {
                cleanedTeamB
            } else {
                emptyList()
            }
        }
    }

    // Метод для получения всех игроков в комнате (teamA + teamB)
    fun getAllPlayers(roomId: Long): Mono<List<String>> {
        return getGameState(roomId).map { gameState ->
            val cleanedTeamA = cleanPlayerNames(gameState.teamA)
            val cleanedTeamB = cleanPlayerNames(gameState.teamB)
            cleanedTeamA + cleanedTeamB
        }
    }

    // Метод для очистки имен игроков от фигурных скобок
    private fun cleanPlayerNames(players: List<String>): List<String> {
        return players.map { cleanUsername(it) }
    }

    // Метод для удаления фигурных скобок из имени
    private fun cleanUsername(username: String): String {
        return username.replace("{", "").replace("}", "")
    }
}