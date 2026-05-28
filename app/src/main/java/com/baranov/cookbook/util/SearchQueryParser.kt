package com.baranov.cookbook.util

/**
 * Результат парсинга поисковой строки.
 * @param recipeName подстрока для поиска по названию рецепта (null если не задано).
 * @param authorName подстрока для поиска по имени автора (null если не задано).
 */
data class SearchQueryParts(
    val recipeName: String? = null,
    val authorName: String? = null
)

/**
 * Парсер расширенного поискового синтаксиса с флагами.
 *
 * Поддерживаемые флаги (расширяемый список — см. [FLAGS]):
 *   r:  — поиск по названию рецепта
 *   a:  — поиск по имени автора
 *
 * Правила:
 *   - Флаг распознаётся в начале токена (в начале строки или после пробела).
 *   - Ведущие пробелы после двоеточия игнорируются.
 *   - Значение флага тянется до следующего известного флага или конца строки, затем trim.
 *   - Текст до первого флага (без префикса) считается значением r: (название) по умолчанию.
 *
 * Примеры:
 *   "Блины"            -> recipeName="Блины"
 *   "r:Блины"          -> recipeName="Блины"
 *   "a:Walter White"          -> authorName="Walter White"
 *   "r:Блины a:Walter White"   -> recipeName="Блины", authorName="Walter White"
 *   "Блины a:Walter White"     -> recipeName="Блины", authorName="Walter White"
 *   "a:Walter White a:Vasya"  -> authorName="Vasya" (Выбирается последний)
 */
object SearchQueryParser {

    // Карта известных префиксов → внутренний ключ. Расширяемо: добавить флаг = добавить строку сюда.
    private val FLAGS = mapOf(
        "r:" to "recipe",
        "a:" to "author"
    )

    fun parse(input: String): SearchQueryParts {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return SearchQueryParts()

        // Аккумулируем значения по ключам. Последнее значение выигрывает (перезапись).
        val values = mutableMapOf<String, String>()

        // Идём по строке, разбивая на сегменты по флагам.
        // Текст до первого флага считается "recipe" по умолчанию.
        var currentKey = "recipe"
        val currentValue = StringBuilder()

        // Токенизация по пробелам, но сохраняем возможность многословных значений.
        val tokens = trimmed.split(Regex("\\s+"))

        fun flush() {
            val v = currentValue.toString().trim()
            if (v.isNotEmpty()) {
                values[currentKey] = v  // последний выигрывает
            }
            currentValue.clear()
        }

        for (token in tokens) {
            val matchedFlag = FLAGS.keys.firstOrNull { token.startsWith(it) }
            if (matchedFlag != null) {
                // Начинается новый флаг — сбрасываем накопленное предыдущее значение.
                flush()
                currentKey = FLAGS[matchedFlag]!!
                // Остаток токена после префикса (с учётом "пробелы после : игнорируются").
                val rest = token.removePrefix(matchedFlag).trimStart()
                if (rest.isNotEmpty()) currentValue.append(rest)
            } else {
                // Продолжение текущего значения.
                if (currentValue.isNotEmpty()) currentValue.append(" ")
                currentValue.append(token)
            }
        }
        flush()

        return SearchQueryParts(
            recipeName = values["recipe"],
            authorName = values["author"]
        )
    }
}