package com.logics.logics.entities

enum class CategoryList {
    HISTORY,
    SCIENCE,
    LITERATURE,
    GEOGRAPHY,
    SPORTS,
    ENTERTAINMENT,
    ART,
    TECHNOLOGY;

    fun getLocalizedName(): String {
        return when (this) {
            HISTORY -> "История"
            SCIENCE -> "Наука"
            LITERATURE -> "Литература"
            GEOGRAPHY -> "География"
            SPORTS -> "Спорт"
            ENTERTAINMENT -> "Развлечения"
            ART -> "Искусство"
            TECHNOLOGY -> "Технологии"
        }
    }
}