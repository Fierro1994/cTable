package com.logics.logics.services

import com.logics.logics.entities.GameState
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

import com.logics.logics.repositories.GameStateRepository


@Service
class GameService(private val gameStateRepository: GameStateRepository) {

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