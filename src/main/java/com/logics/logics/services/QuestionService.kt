package com.logics.logics.services

import com.logics.logics.entities.Question
import com.logics.logics.repositories.QuestionRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.*

@Service
class QuestionService(private val questionRepository: QuestionRepository) {

    fun getRandomQuestion(): Mono<Question> {
        return questionRepository.findAll()
            .collectList()
            .map { questions -> questions.randomOrNull() }
            .flatMap { Mono.justOrEmpty(it) }
    }

    fun checkAnswer(question: Question, answer: String): Boolean {
        return question.correctAnswer.equals(answer, ignoreCase = true)
    }
}