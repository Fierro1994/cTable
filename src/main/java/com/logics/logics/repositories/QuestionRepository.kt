package com.logics.logics.repositories

import com.logics.logics.entities.Question
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface QuestionRepository : ReactiveCrudRepository<Question, Long> {
}