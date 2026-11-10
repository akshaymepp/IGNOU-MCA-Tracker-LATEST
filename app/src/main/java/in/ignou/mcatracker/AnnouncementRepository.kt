package `in`.ignou.mcatracker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class AnnouncementRepository {
    private val sources = listOf(
        "https://www.ignou.ac.in/announcements/0?nav=6" to "IGNOU Headquarters",
        "https://webservices.ignou.ac.in/assignments/schools/socis/mca_new/mca_new.html" to "SOCIS MCA_NEW Assignments",
        "https://rckozhikode.ignou.ac.in/" to "IGNOU Regional Centre Kozhikode"
    )

    suspend fun fetch(): List<Announcement> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Announcement>()
        sources.forEach { (sourceUrl, sourceName) ->
            try {
                val doc = Jsoup.connect(sourceUrl)
                    .userAgent("Mozilla/5.0 (compatible; IGNOU-MCA-Tracker/1.0)")
                    .timeout(18000).get()
                doc.select("a[href]").forEach anchorLoop@{ a ->
                    val title = a.text().trim().replace(Regex("\\s+"), " ")
                    if (title.length < 12) return@anchorLoop
                    val context = a.closest("tr")?.text()
                        ?: a.parent()?.parent()?.text()
                        ?: a.parent()?.text().orEmpty()
                    val combined = "$title $context".lowercase()
                    if (!isRelevant(combined, sourceName)) return@anchorLoop
                    val href = a.absUrl("href").ifBlank { sourceUrl }
                    val rawDate = Regex("\\b\\d{1,2}[-/]\\d{1,2}[-/]\\d{4}\\b")
                        .find(context)?.value ?: ""
                    val date = formatAnnouncementDate(rawDate)
                    results += Announcement(title, date, href, sourceName, classify(combined), context.take(260))
                }
            } catch (_: Exception) {
                // A source may be temporarily unavailable; keep results from other sources.
            }
        }
        results.distinctBy { it.url + it.title }.take(150)
    }


    private fun formatAnnouncementDate(rawDate: String): String {
        if (rawDate.isBlank()) return ""
        val patterns = listOf("dd/MM/yyyy", "d/M/yyyy", "dd-MM-yyyy", "d-M-yyyy")
        for (pattern in patterns) {
            try {
                val parser = java.text.SimpleDateFormat(pattern, java.util.Locale.ENGLISH)
                parser.isLenient = false
                val parsed = parser.parse(rawDate) ?: continue
                return java.text.SimpleDateFormat(
                    "dd - MMM - yyyy", java.util.Locale.ENGLISH
                ).format(parsed)
            } catch (_: Exception) {
                // Try another supported date pattern.
            }
        }
        return rawDate
    }

    private fun isRelevant(text: String, source: String): Boolean {
        val projectTerms = listOf(
            "mca_new", "mca new", "mcsp", "synopsis", "project proposal",
            "project submission", "project report", "project viva", "project evaluation",
            "project guidelines", "project guide", "project supervisor", "project approval",
            "project fee", "project work", "internship", "dissertation"
        )
        val hallTicketTerms = listOf(
            "hall ticket", "hall-ticket", "admit card", "admission ticket",
            "term-end examination", "term end examination", "tee"
        )
        val resultTerms = listOf(
            "result", "results", "grade card", "gradecard", "revaluation",
            "re-evaluation", "re evaluation", "rechecking", "re-checking",
            "revised result", "updated result", "term-end result", "term end result",
            "tee result", "marksheet", "mark sheet", "marks statement"
        )
        val otherMcaTerms = listOf(
            "exam registration", "examination form", "exam form", "term-end examination form",
            "tee form", "examination fee", "exam fee", "assignment", "examination",
            "date sheet", "re-registration", "registration", "practical",
            "programme guide", "counselling"
        )
        return projectTerms.any(text::contains) ||
            hallTicketTerms.any(text::contains) ||
            resultTerms.any(text::contains) ||
            otherMcaTerms.any(text::contains) ||
            source.contains("MCA_NEW")
    }

    private fun classify(text: String): String = when {
        listOf(
            "synopsis", "project", "mcsp", "viva", "internship",
            "dissertation", "supervisor", "project fee"
        ).any(text::contains) -> "Project"
        listOf("hall ticket", "hall-ticket", "admit card", "admission ticket").any(text::contains) -> "Hall Ticket"
        listOf(
            "result", "results", "grade card", "gradecard", "revaluation",
            "re-evaluation", "re evaluation", "rechecking", "re-checking",
            "revised result", "updated result", "tee result", "marksheet", "mark sheet"
        ).any(text::contains) -> "Results"
        "assignment" in text -> "Assignments"
        listOf(
            "exam registration", "examination form", "exam form",
            "term-end examination form", "tee form", "examination fee", "exam fee"
        ).any(text::contains) -> "Exam Registration"
        listOf("term-end", "term end", "exam", "examination", "date sheet").any(text::contains) -> "Exams"
        listOf("registration", "re-registration").any(text::contains) -> "Registration"
        else -> "General"
    }

}
