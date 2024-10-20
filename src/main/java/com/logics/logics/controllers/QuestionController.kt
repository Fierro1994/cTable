package com.logics.logics.controllers

import com.logics.logics.entities.CategoryList
import com.logics.logics.entities.Question
import com.logics.logics.repositories.QuestionRepository
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import reactor.core.publisher.Mono

@Controller
@RequestMapping("/questions")
class QuestionController(private val questionRepository: QuestionRepository) {

    @GetMapping("/create")
    fun showCreateQuestionForm(model: Model): String {
        model.addAttribute("question", Question(
            id = null,
            questionText = "",
            optionA = "",
            optionB = "",
            optionC = "",
            optionD = "",
            correctAnswer = "",
            imageUrl = null,
        ))
        model.addAttribute("categories", CategoryList.values())
        return "create_question"
    }

    @PostMapping("/create")
    fun createQuestion(@ModelAttribute question: Question): Mono<String> {
        return questionRepository.save(question)
            .map { _ -> "redirect:/questions/create?success" }
    }
}
