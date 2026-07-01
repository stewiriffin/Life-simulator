// app/src/main/java/com/maisha/game/data/JobPool.kt
package com.maisha.game.data

import com.maisha.game.data.model.Job
import com.maisha.game.data.model.SchoolStage

object JobPool {

    val jobs: List<Job> = listOf(
        Job(
            id = "matatu_conductor",
            title = "Matatu Conductor",
            minEducation = SchoolStage.SECONDARY,
            baseSalary = 180_000
        ),
        Job(
            id = "shop_attendant",
            title = "Shop Attendant",
            minEducation = SchoolStage.SECONDARY,
            baseSalary = 200_000
        ),
        Job(
            id = "security_guard",
            title = "Security Guard",
            minEducation = SchoolStage.SECONDARY,
            baseSalary = 220_000
        ),
        Job(
            id = "waiter",
            title = "Waiter",
            minEducation = SchoolStage.SECONDARY,
            baseSalary = 250_000
        ),
        Job(
            id = "teacher",
            title = "Teacher",
            minEducation = SchoolStage.GRADUATED,
            baseSalary = 480_000
        ),
        Job(
            id = "software_developer",
            title = "Software Developer",
            minEducation = SchoolStage.GRADUATED,
            baseSalary = 900_000
        ),
        Job(
            id = "nurse",
            title = "Nurse",
            minEducation = SchoolStage.GRADUATED,
            baseSalary = 550_000
        ),
        Job(
            id = "accountant",
            title = "Accountant",
            minEducation = SchoolStage.GRADUATED,
            baseSalary = 650_000
        ),
        Job(
            id = "bank_clerk",
            title = "Bank Clerk",
            minEducation = SchoolStage.GRADUATED,
            baseSalary = 600_000
        ),
        Job(
            id = "journalist",
            title = "Journalist",
            minEducation = SchoolStage.GRADUATED,
            baseSalary = 500_000
        ),
        Job(
            id = "engineer",
            title = "Engineer",
            minEducation = SchoolStage.GRADUATED,
            baseSalary = 1_000_000
        ),
        Job(
            id = "civil_servant",
            title = "Civil Servant",
            minEducation = SchoolStage.GRADUATED,
            baseSalary = 450_000
        )
    )

    fun findById(jobId: String): Job? = jobs.find { it.id == jobId }
}
