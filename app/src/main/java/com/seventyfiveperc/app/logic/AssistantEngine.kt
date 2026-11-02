package com.seventyfiveperc.app.logic

import com.seventyfiveperc.app.data.models.*
import java.util.*
import java.text.SimpleDateFormat
import com.seventyfiveperc.app.R

data class AssistantResponse(
    val text: String,
    val imageRes: Int? = null
)

class AssistantEngine {

    private val holidayMap = mapOf(
        "01-01" to "New Year's Day",
        "01-26" to "Republic Day",
        "03-25" to "Holi",
        "08-15" to "Independence Day",
        "10-02" to "Gandhi Jayanti",
        "12-25" to "Christmas"
    )

    private val auraAdvice = listOf(
        "Low attendance is a canon event for some, but you're trying to be the main character. Stay consistent. 💅",
        "Your aura is peaking when you walk into a 9 AM lecture on time. Keep that energy. ⚡",
        "Don't let a toxic situationship with your bed ruin your 75% streak. Wake up! ☕",
        "Tactical bunking is an art. Don't be a messy artist. 🎨",
        "The professor might forget your face, but the server never forgets your attendance. 🖥️",
        "Stay delulu until your goals become realulu, but keep that attendance above 75% fr. 💀"
    )

    fun getDecisionWithContext(
        query: String,
        subjects: List<Subject>,
        attendance: Map<Long, List<AttendanceRecord>>,
        timetable: List<TimetableEntry>,
        academicEvents: List<AcademicEvent> = emptyList()
    ): AssistantResponse {
        val q = query.lowercase().trim()
        val now = Calendar.getInstance()
        val dateKey = SimpleDateFormat("MM-dd", Locale.getDefault()).format(now.time)

        // 0. Greetings & Vibes
        if (q == "hi" || q == "hello" || q == "yo" || q == "hey" || q == "aura") {
            return AssistantResponse(
                "Yo! I'm AURA, your academic plug. ${auraAdvice.random()} How can I help you slay the semester? 💅✨",
                R.drawable.aura_1
            )
        }

        // 1. MENTAL HEALTH & EMOTIONAL SUPPORT (Empathy + Vibes)
        val stressKeywords = listOf("stress", "tired", "burnout", "give up", "quit", "sad", "exhausted", "heavy", "mental", "anxiety", "depressed", "help", "crying", "lonely", "failed", "scared", "hate college")
        if (stressKeywords.any { q.contains(it) }) {
            return AssistantResponse(
                "Listen to me, bestie. 🫂 Attendance is just a number, but your brain is the hardware. If you're hitting a wall, take a 'Mental Health Bunk'. One lecture won't kill your degree, but burnout will kill your vibe. Go get a matcha, touch some grass, or just sleep. I'll handle the stats. You're the main character, don't let the side quests break you. We're in this together. ✨",
                R.drawable.aura_2
            )
        }

        // 2. CAMPUS LIFE & RELATIONSHIPS (The "Wise" Advice)
        val campusKeywords = listOf("canteen", "prof", "lecture", "hostel", "senior", "junior", "ragging", "club", "society", "fest", "event", "freshers", "library", "exam", "assignment", "project", "lab", "viva")
        val romanceKeywords = listOf("crush", "date", "toxic", "situationship", "ghosted", "breakup", "rejected", "love", "ex ")
        
        if (romanceKeywords.any { q.contains(it) }) {
            return AssistantResponse(
                "Your ex is a mid-tier distraction, but your GPA is a top-tier asset. 💅 If they ghosted you, let them stay in the void. Don't let your attendance ghost you too. Go to class, look hot, and mog the entire lecture hall. Revenge is best served with a 75%+ and a glow-up. 📈✨",
                R.drawable.aura_6
            )
        }

        if (q.contains("canteen") || q.contains("food") || q.contains("hungry") || q.contains("eat")) {
            return AssistantResponse("Canteen samosas are a canon event, but so is getting debarred. 🥟 If you're gonna skip for food, make sure it's not a 'Strict Prof' lecture. Tactical snacking > Academic suicide. 🚔", R.drawable.aura_1)
        }

        if (campusKeywords.any { q.contains(it) }) {
            return AssistantResponse(
                "College is 10% classes and 90% surviving the plot twists. 🎬 My advice? Build your 'Attendance Buffer' now so you can go full 'Ghost Mode' when the fests start. Don't be the NPC who misses the fun because they were edging the 75% limit all semester. Lock in! 🏛️",
                R.drawable.aura_8
            )
        }

        // 3. GEN-Z BRAINROT & SOCIAL VIBES
        if (q.contains("rizz") || q.contains("aura") || q.contains("mog") || q.contains("skibidi") || q.contains("sigma") || q.contains("gyatt") || q.contains("fanum") || q.contains("sus") || q.contains("bet") || q.contains("no cap") || q.contains("fr")) {
            return AssistantResponse("Your aura is currently leaking because you're worrying about a 9 AM class. 📉 Real ones have that 'Present' rizz without even trying. Slay the semester or be slayed by the system, no cap. 💅", R.drawable.aura_6)
        }
        
        if (q.contains("delulu")) {
            return AssistantResponse("Being delulu is a lifestyle, but your attendance tracker is the only thing that's real. 💀 Stop gaslighting yourself into thinking 60% is enough to clear the exams. Wake up, babe. ☕", R.drawable.aura_3)
        }

        // 4. HOLIDAYS & EVENTS (Priority)
        val startOfDay = now.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
        val endOfDay = now.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis
        val eventToday = academicEvents.find { it.date in startOfDay..endOfDay }
        
        if (eventToday != null) {
            return if (eventToday.isHoliday) {
                AssistantResponse("Today is a holiday for '${eventToday.name}'. AI advice: Chilling is the only task today. Slay. 🌸", R.drawable.aura_2)
            } else {
                AssistantResponse("You've got '${eventToday.name}' today. 🗓️ Check if it clashes with your lectures! Don't let it ruin your attendance streak.", R.drawable.aura_1)
            }
        }

        val holidayToday = holidayMap[dateKey]
        if (holidayToday != null) {
            return AssistantResponse("Happy $holidayToday! 🌸 Campus is likely dead. Go celebrate and forget about the 75% rule for a day. 💅", R.drawable.aura_2)
        }

        // 5. ATTENDANCE & BUNKING (ACADEMIC DOMAIN)
        if (q.contains("bunk") || q.contains("skip") || q.contains("attendance") || q.contains("cooked") || q.contains("clutch") || q.contains("class") || q.contains("attend")) {
            if (subjects.isEmpty()) return AssistantResponse("Your dashboard is looking a bit empty fr. 💀 Add some subjects or sync your ERP so I can actually tell you if you're cooked or not.", R.drawable.aura_1)
            
            val stats = subjects.map { sub ->
                val records = attendance[sub.id] ?: emptyList()
                val present = records.count { it.status == AttendanceStatus.PRESENT }
                val total = records.size
                val perc = if (total == 0) 100.0 else (present.toDouble() / total) * 100
                sub.name to perc
            }
            val subjectsAtRisk = stats.filter { it.second < 75.0 }
            
            return if (subjectsAtRisk.isNotEmpty()) {
                val sub = subjectsAtRisk.minByOrNull { it.second }!!
                AssistantResponse("You're at ${sub.second.toInt()}% in ${sub.first}. You are LITERALLY COOKED. 🍳 If you bunk today, the Dean is gonna have a field day with your soul. Go to class. Now. 🏃💨", R.drawable.aura_3)
            } else {
                AssistantResponse("Stats are looking clean (75%+). 🧊 Tactical bunk authorized. Go live your life, Ghost Mode is active! 👻 Just don't make it a habit or you'll be back in the trenches, fr.", R.drawable.aura_2)
            }
        }

        // 6. TRULY OUT OF SYLLABUS / RANDOM FALLBACK
        val academicContext = listOf(
            "study", "book", "school", "college", "uni", "degree", "marks", "gpa", 
            "teacher", "sir", "ma'am", "friend", "group", "holiday", "weekend", 
            "class", "lecture", "bunk", "attendance", "mark", "present", "absent",
            "exam", "test", "quiz", "assignment", "project", "lab", "viva",
            "canteen", "hostel", "mess", "placement", "job", "career", "intern",
            "crush", "date", "relationship", "toxic", "broke", "money", "sleep",
            "tired", "stress", "sad", "happy", "vibe", "rizz", "aura", "eat", "food",
            "coding", "programming", "hacker", "tech", "laptop", "bag", "notes",
            "graduation", "freshers", "senior", "junior", "ragging", "fest", "club",
            "society", "sports", "gym", "workout"
        )
        val isTrulyRandom = academicContext.none { q.contains(it) } && q.length > 5

        if (isTrulyRandom) {
            return AssistantResponse(
                "L + Ratio + Didn't ask + This isn't in the student handbook. 💅 Look, I'm here to save your degree and your mental health, not to explain random lore. Stick to the campus vibe! 🏛️",
                R.drawable.aura_5
            )
        }

        // 7. GENERAL WISE FALLBACK
        return AssistantResponse(
            "I'm locked in on your academic journey. 🏛️ Ask me if you should bunk, how to handle a toxic situationship, or check your aura stats. We're in this together, fr. 🤝",
            R.drawable.aura_8
        )
    }

    fun getDecision(query: String, subjects: List<Subject>, attendance: Map<Long, List<AttendanceRecord>>, timetable: List<TimetableEntry>, calendar: String? = null): String {
        return getDecisionWithContext(query, subjects, attendance, timetable).text
    }
}
