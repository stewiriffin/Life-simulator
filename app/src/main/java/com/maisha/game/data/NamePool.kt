// app/src/main/java/com/maisha/game/data/NamePool.kt (modified — per-country pools)
package com.maisha.game.data

import com.maisha.game.data.model.Gender
import kotlin.random.Random

data class CountryNamePool(
    val maleFirstNames: List<String>,
    val femaleFirstNames: List<String>,
    val surnames: List<String>
) {
    fun randomSurname(): String = surnames.random()
    fun randomMaleFirstName(): String = maleFirstNames.random()
    fun randomFemaleFirstName(): String = femaleFirstNames.random()
    fun randomFirstName(gender: Gender): String = when (gender) {
        Gender.MALE -> randomMaleFirstName()
        Gender.FEMALE -> randomFemaleFirstName()
    }
    fun randomFullName(gender: Gender): String = "${randomFirstName(gender)} ${randomSurname()}"
    fun randomSiblingName(): String {
        val firstName = when (Random.nextInt(3)) {
            0 -> randomMaleFirstName()
            1 -> randomFemaleFirstName()
            else -> (maleFirstNames + femaleFirstNames).random()
        }
        return "$firstName ${randomSurname()}"
    }
}

object NamePool {

    private val kenyaPool = CountryNamePool(
        maleFirstNames = listOf(
            "Brian", "Kevin", "Dennis", "Collins", "Victor",
            "Ian", "Samuel", "Daniel", "Joseph", "Peter",
            "James", "David", "Michael", "George", "Patrick"
        ),
        femaleFirstNames = listOf(
            "Wanjiku", "Akinyi", "Njeri", "Amina", "Faith",
            "Grace", "Mercy", "Joy", "Mary", "Anne",
            "Lucy", "Jane", "Ruth", "Esther", "Sarah"
        ),
        surnames = listOf(
            "Ochieng", "Wanjala", "Kamau", "Muthoni", "Otieno",
            "Njoroge", "Mutua", "Kariuki", "Odhiambo", "Wambui"
        )
    )

    private val pools: Map<String, CountryNamePool> = mapOf(
        "KE" to kenyaPool,
        "NG" to CountryNamePool(
            maleFirstNames = listOf("Chidi", "Emeka", "Tunde", "Ibrahim", "Olu", "Bayo", "Kunle", "Segun", "Femi", "Yusuf"),
            femaleFirstNames = listOf("Adaeze", "Ngozi", "Aisha", "Funke", "Zainab", "Amara", "Chioma", "Bisi", "Halima", "Efe"),
            surnames = listOf("Okonkwo", "Adeyemi", "Bello", "Eze", "Okafor", "Nwosu", "Musa", "Adebayo", "Chukwu", "Yakubu")
        ),
        "US" to CountryNamePool(
            maleFirstNames = listOf("James", "Michael", "David", "Chris", "Daniel", "Matthew", "Andrew", "Ryan", "Tyler", "Brandon"),
            femaleFirstNames = listOf("Emily", "Sarah", "Jessica", "Ashley", "Megan", "Lauren", "Hannah", "Olivia", "Sophia", "Emma"),
            surnames = listOf("Smith", "Johnson", "Williams", "Brown", "Jones", "Miller", "Davis", "Wilson", "Anderson", "Taylor")
        ),
        "GB" to CountryNamePool(
            maleFirstNames = listOf("Oliver", "Harry", "George", "Jack", "Charlie", "Thomas", "William", "James", "Henry", "Alfie"),
            femaleFirstNames = listOf("Olivia", "Amelia", "Isla", "Ava", "Emily", "Sophia", "Grace", "Lily", "Freya", "Poppy"),
            surnames = listOf("Smith", "Jones", "Williams", "Taylor", "Brown", "Davies", "Evans", "Wilson", "Thomas", "Roberts")
        ),
        "IN" to CountryNamePool(
            maleFirstNames = listOf("Arjun", "Rahul", "Vikram", "Amit", "Rohan", "Sanjay", "Karan", "Dev", "Anil", "Raj"),
            femaleFirstNames = listOf("Priya", "Ananya", "Kavya", "Meera", "Sita", "Lakshmi", "Divya", "Neha", "Pooja", "Riya"),
            surnames = listOf("Sharma", "Patel", "Singh", "Kumar", "Reddy", "Gupta", "Iyer", "Nair", "Das", "Verma")
        ),
        "BR" to CountryNamePool(
            maleFirstNames = listOf("Lucas", "Gabriel", "Miguel", "Arthur", "Pedro", "Rafael", "Felipe", "Bruno", "Diego", "Thiago"),
            femaleFirstNames = listOf("Maria", "Ana", "Julia", "Beatriz", "Larissa", "Camila", "Fernanda", "Isabela", "Luiza", "Mariana"),
            surnames = listOf("Silva", "Santos", "Oliveira", "Souza", "Lima", "Costa", "Ferreira", "Rodrigues", "Almeida", "Pereira")
        ),
        "PH" to CountryNamePool(
            maleFirstNames = listOf("Jose", "Mark", "John", "Angelo", "Carlo", "Miguel", "Rafael", "Paolo", "Kenneth", "Jerome"),
            femaleFirstNames = listOf("Maria", "Angel", "Grace", "Joy", "Kim", "Hannah", "Patricia", "Nicole", "Andrea", "Catherine"),
            surnames = listOf("Santos", "Reyes", "Cruz", "Bautista", "Garcia", "Mendoza", "Torres", "Flores", "Ramos", "Aquino")
        ),
        "DE" to CountryNamePool(
            maleFirstNames = listOf("Lukas", "Leon", "Finn", "Paul", "Jonas", "Felix", "Max", "Tim", "Niklas", "Jan"),
            femaleFirstNames = listOf("Mia", "Emma", "Hannah", "Sophia", "Lena", "Marie", "Lea", "Anna", "Laura", "Julia"),
            surnames = listOf("Müller", "Schmidt", "Schneider", "Fischer", "Weber", "Meyer", "Wagner", "Becker", "Hoffmann", "Schäfer")
        ),
        "JP" to CountryNamePool(
            maleFirstNames = listOf("Haruto", "Yuto", "Sota", "Ren", "Kaito", "Hayato", "Riku", "Shota", "Takumi", "Daiki"),
            femaleFirstNames = listOf("Yui", "Hina", "Sakura", "Aoi", "Mei", "Rin", "Mio", "Yuna", "Sara", "Nana"),
            surnames = listOf("Sato", "Suzuki", "Takahashi", "Tanaka", "Watanabe", "Ito", "Yamamoto", "Nakamura", "Kobayashi", "Kato")
        ),
        "ZA" to CountryNamePool(
            maleFirstNames = listOf("Thabo", "Sipho", "Liam", "Ethan", "Siyabonga", "Kagiso", "Mandla", "Johan", "Pieter", "David"),
            femaleFirstNames = listOf("Nomsa", "Thandi", "Lerato", "Zanele", "Emma", "Olivia", "Amahle", "Lindiwe", "Sarah", "Grace"),
            surnames = listOf("Nkosi", "Dlamini", "Mokoena", "Botha", "Van der Merwe", "Pretorius", "Khumalo", "Mahlangu", "Ndlovu", "Smith")
        ),
        "EG" to CountryNamePool(
            maleFirstNames = listOf("Omar", "Youssef", "Ahmed", "Karim", "Hassan", "Mahmoud", "Ali", "Tarek", "Amir", "Nader"),
            femaleFirstNames = listOf("Fatima", "Nour", "Yasmin", "Mariam", "Salma", "Dina", "Hana", "Layla", "Rania", "Amira"),
            surnames = listOf("Hassan", "Ibrahim", "Farouk", "Mahmoud", "Ali", "Said", "Khalil", "Nasser", "Fouad", "Rashad")
        ),
        "MX" to CountryNamePool(
            maleFirstNames = listOf("Santiago", "Mateo", "Diego", "Luis", "Carlos", "Javier", "Miguel", "Andrés", "Ricardo", "Fernando"),
            femaleFirstNames = listOf("Sofía", "Valentina", "Camila", "Mariana", "Lucía", "Daniela", "Paula", "Andrea", "Fernanda", "Ximena"),
            surnames = listOf("Hernández", "García", "Martínez", "López", "González", "Rodríguez", "Pérez", "Sánchez", "Ramírez", "Torres")
        ),
        "ID" to CountryNamePool(
            maleFirstNames = listOf("Budi", "Agus", "Rizky", "Adi", "Hendra", "Dimas", "Fajar", "Rudi", "Andi", "Bayu"),
            femaleFirstNames = listOf("Siti", "Dewi", "Putri", "Ayu", "Rina", "Maya", "Lestari", "Indah", "Fitri", "Wulan"),
            surnames = listOf("Wijaya", "Santoso", "Pratama", "Kusuma", "Saputra", "Hidayat", "Nugroho", "Setiawan", "Rahman", "Siregar")
        ),
        "FR" to CountryNamePool(
            maleFirstNames = listOf("Louis", "Gabriel", "Raphaël", "Arthur", "Jules", "Adam", "Lucas", "Hugo", "Nathan", "Ethan"),
            femaleFirstNames = listOf("Emma", "Jade", "Louise", "Alice", "Chloé", "Léa", "Manon", "Camille", "Inès", "Sarah"),
            surnames = listOf("Martin", "Bernard", "Dubois", "Thomas", "Robert", "Richard", "Petit", "Durand", "Leroy", "Moreau")
        ),
        "CA" to CountryNamePool(
            maleFirstNames = listOf("Liam", "Noah", "Oliver", "Lucas", "Benjamin", "Ethan", "William", "James", "Logan", "Mason"),
            femaleFirstNames = listOf("Olivia", "Emma", "Charlotte", "Amelia", "Sophia", "Ava", "Mia", "Chloe", "Lily", "Abigail"),
            surnames = listOf("Smith", "Brown", "Tremblay", "Martin", "Roy", "Wilson", "MacDonald", "Campbell", "Anderson", "Taylor")
        )
    )

    fun getNamePool(countryCode: String): CountryNamePool =
        pools[countryCode] ?: kenyaPool

    fun randomSurname(countryCode: String = "KE"): String =
        getNamePool(countryCode).randomSurname()

    fun randomMaleFirstName(countryCode: String = "KE"): String =
        getNamePool(countryCode).randomMaleFirstName()

    fun randomFemaleFirstName(countryCode: String = "KE"): String =
        getNamePool(countryCode).randomFemaleFirstName()

    fun randomFullName(gender: Gender, countryCode: String = "KE"): String =
        getNamePool(countryCode).randomFullName(gender)

    fun randomSiblingName(countryCode: String = "KE"): String =
        getNamePool(countryCode).randomSiblingName()
}
