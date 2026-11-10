package `in`.ignou.mcatracker

data class Announcement(
    val title: String,
    val date: String,
    val url: String,
    val source: String,
    val category: String,
    val summary: String = ""
) {
    val id: String get() = (url + title).hashCode().toString()
}
