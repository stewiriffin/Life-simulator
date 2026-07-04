// app/src/main/java/com/maisha/game/data/NamePool.kt
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
            "Brian", "Kevin", "Dennis", "Collins", "Victor", "Ian", "Samuel", "Daniel", "Joseph", "Peter",
            "James", "David", "Michael", "George", "Patrick", "Otieno", "Kamau", "Kipchoge", "Baraka", "Juma",
            "Mwangi", "Kiptoo", "Omondi", "Wekesa", "Mutiso", "Abdi", "Hassan", "Eric", "Alex", "Tony"
        ),
        femaleFirstNames = listOf(
            "Wanjiku", "Akinyi", "Njeri", "Amina", "Faith", "Grace", "Mercy", "Joy", "Mary", "Anne",
            "Lucy", "Jane", "Ruth", "Esther", "Sarah", "Wambui", "Atieno", "Chebet", "Nyambura", "Achieng",
            "Halima", "Fatuma", "Wangari", "Muthoni", "Nafula", "Zawadi", "Neema", "Purity", "Sharon", "Irene"
        ),
        surnames = listOf(
            "Ochieng", "Wanjala", "Kamau", "Muthoni", "Otieno", "Njoroge", "Mutua", "Kariuki", "Odhiambo", "Wambui",
            "Kiprop", "Cheruiyot", "Onyango", "Wekesa", "Mwangi", "Kimani", "Ouma", "Achieng", "Barasa", "Maina",
            "Kiptoo", "Nyambura", "Abdi", "Hassan", "Okoth"
        )
    )

    private val pools: Map<String, CountryNamePool> = mapOf(
        "KE" to kenyaPool,
        "NG" to CountryNamePool(
            maleFirstNames = listOf(
                "Chidi", "Emeka", "Tunde", "Ibrahim", "Olu", "Bayo", "Kunle", "Segun", "Femi", "Yusuf",
                "Chinedu", "Obinna", "Tobi", "Sola", "Ade", "Kelechi", "Uche", "Nnamdi", "Babatunde", "Musa",
                "Ifeanyi", "Chukwudi", "Damilola", "Kayode", "Sani", "Ahmed", "Olamide", "Timi", "Jide", "Dele"
            ),
            femaleFirstNames = listOf(
                "Adaeze", "Ngozi", "Aisha", "Funke", "Zainab", "Amara", "Chioma", "Bisi", "Halima", "Efe",
                "Ifunanya", "Nneka", "Yetunde", "Folake", "Blessing", "Chiamaka", "Fatima", "Ifeoma", "Kemi", "Tolu",
                "Onyinye", "Rukayat", "Sade", "Temitope", "Uchechi", "Yewande", "Adanna", "Bukola", "Chidinma", "Damilola"
            ),
            surnames = listOf(
                "Okonkwo", "Adeyemi", "Bello", "Eze", "Okafor", "Nwosu", "Musa", "Adebayo", "Chukwu", "Yakubu",
                "Ogunleye", "Ibrahim", "Okoro", "Balogun", "Nwachukwu", "Afolabi", "Obi", "Lawal", "Emeka", "Danjuma",
                "Okeke", "Adesina", "Umar", "Onyeka", "Suleiman"
            )
        ),
        "US" to CountryNamePool(
            maleFirstNames = listOf(
                "James", "Michael", "David", "Chris", "Daniel", "Matthew", "Andrew", "Ryan", "Tyler", "Brandon",
                "Joshua", "Justin", "Kevin", "Brian", "Jason", "Eric", "Jacob", "Nathan", "Aaron", "Adam",
                "Ethan", "Noah", "Liam", "Mason", "Logan", "Aiden", "Caleb", "Dylan", "Hunter", "Connor"
            ),
            femaleFirstNames = listOf(
                "Emily", "Sarah", "Jessica", "Ashley", "Megan", "Lauren", "Hannah", "Olivia", "Sophia", "Emma",
                "Madison", "Abigail", "Isabella", "Ava", "Mia", "Chloe", "Grace", "Elizabeth", "Samantha", "Natalie",
                "Rachel", "Kayla", "Brittany", "Alexis", "Victoria", "Lily", "Zoe", "Hailey", "Kaylee", "Avery"
            ),
            surnames = listOf(
                "Smith", "Johnson", "Williams", "Brown", "Jones", "Miller", "Davis", "Wilson", "Anderson", "Taylor",
                "Thomas", "Moore", "Jackson", "Martin", "Lee", "Thompson", "White", "Harris", "Clark", "Lewis",
                "Robinson", "Walker", "Young", "Allen", "King"
            )
        ),
        "CA" to CountryNamePool(
            maleFirstNames = listOf(
                "Liam", "Noah", "Oliver", "Lucas", "Benjamin", "Ethan", "William", "James", "Logan", "Mason",
                "Jacob", "Alexander", "Michael", "Daniel", "Matthew", "Samuel", "David", "Joseph", "Carter", "Owen",
                "Jack", "Leo", "Henry", "Sebastian", "Jackson", "Aiden", "Gabriel", "Thomas", "Charles", "Nathan"
            ),
            femaleFirstNames = listOf(
                "Olivia", "Emma", "Charlotte", "Amelia", "Sophia", "Ava", "Mia", "Chloe", "Lily", "Abigail",
                "Emily", "Harper", "Ella", "Grace", "Scarlett", "Zoey", "Hannah", "Aria", "Nora", "Riley",
                "Victoria", "Claire", "Sophie", "Isabelle", "Camille", "Léa", "Florence", "Alice", "Jade", "Rose"
            ),
            surnames = listOf(
                "Smith", "Brown", "Tremblay", "Martin", "Roy", "Wilson", "MacDonald", "Campbell", "Anderson", "Taylor",
                "Gagnon", "Lee", "Williams", "Johnson", "Thompson", "White", "Côté", "Bouchard", "Morin", "Lavoie",
                "Patel", "Singh", "Chen", "Nguyen", "Kim"
            )
        ),
        "GB" to CountryNamePool(
            maleFirstNames = listOf(
                "Oliver", "Harry", "George", "Jack", "Charlie", "Thomas", "William", "James", "Henry", "Alfie",
                "Noah", "Leo", "Oscar", "Arthur", "Freddie", "Archie", "Theo", "Finley", "Jacob", "Isaac",
                "Edward", "Alexander", "Samuel", "Joseph", "Daniel", "Max", "Lucas", "Ethan", "Mohammed", "Reuben"
            ),
            femaleFirstNames = listOf(
                "Olivia", "Amelia", "Isla", "Ava", "Emily", "Sophia", "Grace", "Lily", "Freya", "Poppy",
                "Mia", "Ella", "Sophie", "Ivy", "Rosie", "Evie", "Florence", "Isabella", "Charlotte", "Daisy",
                "Phoebe", "Sienna", "Elsie", "Matilda", "Alice", "Ruby", "Willow", "Emilia", "Millie", "Harper"
            ),
            surnames = listOf(
                "Smith", "Jones", "Williams", "Taylor", "Brown", "Davies", "Evans", "Wilson", "Thomas", "Roberts",
                "Johnson", "Lewis", "Walker", "Robinson", "Wood", "Thompson", "White", "Watson", "Jackson", "Wright",
                "Green", "Harris", "Cooper", "King", "Lee"
            )
        ),
        "FR" to CountryNamePool(
            maleFirstNames = listOf(
                "Louis", "Gabriel", "Raphaël", "Arthur", "Jules", "Adam", "Lucas", "Hugo", "Nathan", "Ethan",
                "Léo", "Paul", "Noah", "Gabin", "Sacha", "Maël", "Tom", "Mohamed", "Aaron", "Timéo",
                "Enzo", "Théo", "Maxime", "Antoine", "Nicolas", "Alexandre", "Mathis", "Clément", "Baptiste", "Romain"
            ),
            femaleFirstNames = listOf(
                "Emma", "Jade", "Louise", "Alice", "Chloé", "Léa", "Manon", "Camille", "Inès", "Sarah",
                "Lina", "Rose", "Anna", "Mila", "Julia", "Léna", "Zoé", "Lola", "Agathe", "Juliette",
                "Clara", "Eva", "Charlotte", "Margaux", "Ambre", "Nina", "Jeanne", "Lucie", "Elise", "Pauline"
            ),
            surnames = listOf(
                "Martin", "Bernard", "Dubois", "Thomas", "Robert", "Richard", "Petit", "Durand", "Leroy", "Moreau",
                "Simon", "Laurent", "Lefebvre", "Michel", "Garcia", "David", "Bertrand", "Roux", "Vincent", "Fournier",
                "Morel", "Girard", "André", "Lefèvre", "Mercier"
            )
        ),
        "DE" to CountryNamePool(
            maleFirstNames = listOf(
                "Lukas", "Leon", "Finn", "Paul", "Jonas", "Felix", "Max", "Tim", "Niklas", "Jan",
                "Ben", "Luis", "Noah", "Elias", "Emil", "Henry", "Theo", "Oskar", "Matteo", "Anton",
                "Julian", "Moritz", "David", "Alexander", "Sebastian", "Tobias", "Philipp", "Simon", "Fabian", "Marcel"
            ),
            femaleFirstNames = listOf(
                "Mia", "Emma", "Hannah", "Sophia", "Lena", "Marie", "Lea", "Anna", "Laura", "Julia",
                "Emilia", "Lina", "Mila", "Clara", "Luisa", "Ella", "Ida", "Greta", "Nora", "Sophie",
                "Johanna", "Amelie", "Charlotte", "Marlene", "Helena", "Paula", "Nele", "Frieda", "Pia", "Jana"
            ),
            surnames = listOf(
                "Müller", "Schmidt", "Schneider", "Fischer", "Weber", "Meyer", "Wagner", "Becker", "Hoffmann", "Schäfer",
                "Koch", "Bauer", "Richter", "Klein", "Wolf", "Schröder", "Neumann", "Schwarz", "Zimmermann", "Braun",
                "Krüger", "Hofmann", "Hartmann", "Lange", "Schmitt"
            )
        ),
        "IN" to CountryNamePool(
            maleFirstNames = listOf(
                "Arjun", "Rahul", "Vikram", "Amit", "Rohan", "Sanjay", "Karan", "Dev", "Anil", "Raj",
                "Aarav", "Vivaan", "Aditya", "Krishna", "Siddharth", "Nikhil", "Varun", "Harsh", "Manish", "Pranav",
                "Ravi", "Suresh", "Deepak", "Ankit", "Gaurav", "Mohit", "Yash", "Kunal", "Abhishek", "Ishaan"
            ),
            femaleFirstNames = listOf(
                "Priya", "Ananya", "Kavya", "Meera", "Sita", "Lakshmi", "Divya", "Neha", "Pooja", "Riya",
                "Aanya", "Diya", "Isha", "Saanvi", "Anika", "Myra", "Kiara", "Aadhya", "Pari", "Navya",
                "Shreya", "Tanvi", "Nisha", "Sneha", "Kriti", "Aditi", "Swati", "Pallavi", "Ritu", "Sunita"
            ),
            surnames = listOf(
                "Sharma", "Patel", "Singh", "Kumar", "Reddy", "Gupta", "Iyer", "Nair", "Das", "Verma",
                "Mehta", "Joshi", "Chopra", "Malhotra", "Banerjee", "Chatterjee", "Rao", "Pillai", "Desai", "Shah",
                "Agarwal", "Kapoor", "Bhat", "Menon", "Khan"
            )
        ),
        "JP" to CountryNamePool(
            maleFirstNames = listOf(
                "Haruto", "Yuto", "Sota", "Ren", "Kaito", "Hayato", "Riku", "Shota", "Takumi", "Daiki",
                "Hiroto", "Sora", "Yuma", "Kota", "Minato", "Asahi", "Itsuki", "Yamato", "Haruki", "Sosuke",
                "Kenji", "Takeshi", "Hiroshi", "Kazuki", "Ryota", "Naoki", "Sho", "Akira", "Taro", "Yuki"
            ),
            femaleFirstNames = listOf(
                "Yui", "Hina", "Sakura", "Aoi", "Mei", "Rin", "Mio", "Yuna", "Sara", "Nana",
                "Himari", "Koharu", "Ichika", "Akari", "Yua", "Riko", "Saki", "Miyu", "Hana", "Ema",
                "Ayaka", "Misaki", "Nanami", "Yuka", "Rina", "Kana", "Mao", "Airii", "Nozomi", "Haruka"
            ),
            surnames = listOf(
                "Sato", "Suzuki", "Takahashi", "Tanaka", "Watanabe", "Ito", "Yamamoto", "Nakamura", "Kobayashi", "Kato",
                "Yoshida", "Yamada", "Sasaki", "Yamaguchi", "Matsumoto", "Inoue", "Kimura", "Hayashi", "Shimizu", "Yamazaki",
                "Mori", "Abe", "Ikeda", "Hashimoto", "Yamashita"
            )
        ),
        "BR" to CountryNamePool(
            maleFirstNames = listOf(
                "Lucas", "Gabriel", "Miguel", "Arthur", "Pedro", "Rafael", "Felipe", "Bruno", "Diego", "Thiago",
                "Matheus", "Gustavo", "Leonardo", "Enzo", "Bernardo", "Heitor", "Davi", "Samuel", "João", "Guilherme",
                "Rodrigo", "André", "Carlos", "Eduardo", "Marcelo", "Vinícius", "Caio", "Igor", "Henrique", "Daniel"
            ),
            femaleFirstNames = listOf(
                "Maria", "Ana", "Julia", "Beatriz", "Larissa", "Camila", "Fernanda", "Isabela", "Luiza", "Mariana",
                "Alice", "Helena", "Valentina", "Laura", "Sophia", "Manuela", "Giovanna", "Cecília", "Eloá", "Liz",
                "Gabriela", "Amanda", "Bruna", "Carolina", "Letícia", "Natália", "Patrícia", "Rafaela", "Tatiana", "Yasmin"
            ),
            surnames = listOf(
                "Silva", "Santos", "Oliveira", "Souza", "Lima", "Costa", "Ferreira", "Rodrigues", "Almeida", "Pereira",
                "Carvalho", "Gomes", "Martins", "Araújo", "Melo", "Barbosa", "Ribeiro", "Alves", "Cardoso", "Rocha",
                "Dias", "Nascimento", "Moreira", "Cavalcanti", "Teixeira"
            )
        ),
        "MX" to CountryNamePool(
            maleFirstNames = listOf(
                "Santiago", "Mateo", "Diego", "Luis", "Carlos", "Javier", "Miguel", "Andrés", "Ricardo", "Fernando",
                "Sebastián", "Emiliano", "Leonardo", "Daniel", "Alejandro", "José", "Juan", "Pedro", "Ángel", "David",
                "Eduardo", "Francisco", "Gabriel", "Héctor", "Iván", "Jorge", "Manuel", "Óscar", "Pablo", "Raúl"
            ),
            femaleFirstNames = listOf(
                "Sofía", "Valentina", "Camila", "Mariana", "Lucía", "Daniela", "Paula", "Andrea", "Fernanda", "Ximena",
                "Regina", "Renata", "Victoria", "Isabella", "Emma", "Mia", "Natalia", "Valeria", "Aitana", "Romina",
                "Alejandra", "Carolina", "Elena", "Gabriela", "Jimena", "Karla", "Laura", "María", "Patricia", "Rosa"
            ),
            surnames = listOf(
                "Hernández", "García", "Martínez", "López", "González", "Rodríguez", "Pérez", "Sánchez", "Ramírez", "Torres",
                "Flores", "Rivera", "Gómez", "Díaz", "Cruz", "Morales", "Reyes", "Gutiérrez", "Ortiz", "Chávez",
                "Ramos", "Vargas", "Castillo", "Jiménez", "Moreno"
            )
        ),
        "ZA" to CountryNamePool(
            maleFirstNames = listOf(
                "Thabo", "Sipho", "Liam", "Ethan", "Siyabonga", "Kagiso", "Mandla", "Johan", "Pieter", "David",
                "Bandile", "Lwazi", "Tshepo", "Bongani", "Andile", "James", "Michael", "Daniel", "Ryan", "Kyle",
                "Willem", "Francois", "Ruan", "Jaco", "Nkosana", "Mpho", "Karabo", "Lesego", "Thulani", "Vusi"
            ),
            femaleFirstNames = listOf(
                "Nomsa", "Thandi", "Lerato", "Zanele", "Emma", "Olivia", "Amahle", "Lindiwe", "Sarah", "Grace",
                "Naledi", "Palesa", "Boitumelo", "Refilwe", "Ayanda", "Chloe", "Mia", "Hannah", "Jessica", "Amy",
                "Annelie", "Marike", "Elize", "Carmen", "Precious", "Blessing", "Faith", "Hope", "Ntombi", "Sibongile"
            ),
            surnames = listOf(
                "Nkosi", "Dlamini", "Mokoena", "Botha", "Van der Merwe", "Pretorius", "Khumalo", "Mahlangu", "Ndlovu", "Smith",
                "Naidoo", "Pillay", "Govender", "Jacobs", "Williams", "Molefe", "Sithole", "Zulu", "Van Wyk", "Du Plessis",
                "Nel", "Coetzee", "Mabaso", "Radebe", "Mthembu"
            )
        ),
        "EG" to CountryNamePool(
            maleFirstNames = listOf(
                "Omar", "Youssef", "Ahmed", "Karim", "Hassan", "Mahmoud", "Ali", "Tarek", "Amir", "Nader",
                "Mohamed", "Khaled", "Mostafa", "Hossam", "Amr", "Sherif", "Walid", "Ramy", "Adel", "Samir",
                "Ibrahim", "Fady", "Mina", "George", "Mark", "Andrew", "Bassem", "Hany", "Ziad", "Yassin"
            ),
            femaleFirstNames = listOf(
                "Fatima", "Nour", "Yasmin", "Mariam", "Salma", "Dina", "Hana", "Layla", "Rania", "Amira",
                "Sara", "Nada", "Farah", "Malak", "Jana", "Lina", "Maya", "Nourhan", "Reem", "Habiba",
                "Mona", "Heba", "Aya", "Doaa", "Esraa", "Ghada", "Hanan", "Iman", "Laila", "Nadia"
            ),
            surnames = listOf(
                "Hassan", "Ibrahim", "Farouk", "Mahmoud", "Ali", "Said", "Khalil", "Nasser", "Fouad", "Rashad",
                "Mostafa", "Ahmed", "Youssef", "Saleh", "Mansour", "Soliman", "Gamal", "Hegazy", "Kamal", "Zaki",
                "Abdelrahman", "Elmasry", "Fathy", "Osman", "Tawfik"
            )
        ),
        "PH" to CountryNamePool(
            maleFirstNames = listOf(
                "Jose", "Mark", "John", "Angelo", "Carlo", "Miguel", "Rafael", "Paolo", "Kenneth", "Jerome",
                "Christian", "Daniel", "Joshua", "Nathan", "Gabriel", "Matthew", "Ryan", "Justin", "Kyle", "Lance",
                "Andres", "Emmanuel", "Francis", "Ivan", "Jasper", "Luis", "Marco", "Noel", "Oscar", "Vincent"
            ),
            femaleFirstNames = listOf(
                "Maria", "Angel", "Grace", "Joy", "Kim", "Hannah", "Patricia", "Nicole", "Andrea", "Catherine",
                "Angela", "Bianca", "Christine", "Diana", "Elena", "Faith", "Gabrielle", "Isabelle", "Jasmine", "Kristine",
                "Lorraine", "Michelle", "Natalie", "Olivia", "Pauline", "Queenie", "Rose", "Samantha", "Therese", "Vanessa"
            ),
            surnames = listOf(
                "Santos", "Reyes", "Cruz", "Bautista", "Garcia", "Mendoza", "Torres", "Flores", "Ramos", "Aquino",
                "Dela Cruz", "Gonzales", "Lopez", "Perez", "Rivera", "Castillo", "Fernandez", "Morales", "Navarro", "Villanueva",
                "Domingo", "Santiago", "Lim", "Tan", "Sy"
            )
        ),
        "ID" to CountryNamePool(
            maleFirstNames = listOf(
                "Budi", "Agus", "Rizky", "Adi", "Hendra", "Dimas", "Fajar", "Rudi", "Andi", "Bayu",
                "Ahmad", "Dedi", "Eko", "Fauzan", "Gilang", "Hadi", "Irfan", "Joko", "Kurniawan", "Lukman",
                "Muhammad", "Nanda", "Putra", "Rama", "Satria", "Taufik", "Wahyu", "Yoga", "Zaki", "Arief"
            ),
            femaleFirstNames = listOf(
                "Siti", "Dewi", "Putri", "Ayu", "Rina", "Maya", "Lestari", "Indah", "Fitri", "Wulan",
                "Aisyah", "Bunga", "Citra", "Dian", "Eka", "Farah", "Gita", "Hana", "Intan", "Kartika",
                "Lina", "Mega", "Nisa", "Oktavia", "Putri", "Rani", "Sari", "Tika", "Utami", "Yuni"
            ),
            surnames = listOf(
                "Wijaya", "Santoso", "Pratama", "Kusuma", "Saputra", "Hidayat", "Nugroho", "Setiawan", "Rahman", "Siregar",
                "Gunawan", "Halim", "Iskandar", "Kurniawan", "Lestari", "Mahendra", "Nasution", "Prasetyo", "Ramadhan", "Suryadi",
                "Utama", "Wibowo", "Yulianto", "Zulkarnain", "Hartono"
            )
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
