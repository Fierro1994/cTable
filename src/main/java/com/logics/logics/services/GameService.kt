package com.logics.logics.services

import com.logics.logics.entities.GameRoom
import com.logics.logics.entities.GameState
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

import com.logics.logics.repositories.GameStateRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory


@Service
class GameService(private val gameStateRepository: GameStateRepository,
                  private val logger: Logger = LoggerFactory.getLogger(GameService::class.java)
) {

    fun startGameWithTeams(roomId: Long, teamA: List<String>, teamB: List<String>, category: String): Mono<GameState> {
        val gameState = GameState(
            roomId = roomId,
            teamA = teamA,
            teamB = teamB,
            category = category,
            teamAScore = 0,
            teamBScore = 0,
            status = "IN_PROGRESS"
        )
        return gameStateRepository.save(gameState)
    }

    fun createGame(gameRoom: Mono<GameRoom>): Mono<GameState> {
        return gameRoom.flatMap { room ->
            logger.info("Creating game for room: ${room.id}")
            val gameState = GameState(
                roomId = room.id ?: 0L,
                teamA = emptyList(),
                teamB = emptyList(),
                category = room.category,
                teamAScore = 0,
                teamBScore = 0,
                status = "WAITING"
            )
            gameStateRepository.save(gameState)
                .doOnSuccess { savedState ->
                    logger.info("Game state saved successfully: ${savedState.id}")
                }
                .doOnError { error ->
                    logger.error("Error saving game state: ${error.message}", error)
                }
        }
    }

    fun getGameState(roomId: Long): Mono<GameState> {
        return gameStateRepository.findByRoomId(roomId)
    }

    fun updateScore(roomId: Long, teamAScore: Int, teamBScore: Int): Mono<GameState> {
        return gameStateRepository.findByRoomId(roomId)
            .flatMap { gameState ->
                gameState.teamAScore = teamAScore
                gameState.teamBScore = teamBScore
                gameStateRepository.save(gameState)
            }
    }
}