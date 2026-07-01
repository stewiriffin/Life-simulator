// app/src/main/java/com/maisha/game/ui/main/CareerFormatter.kt
package com.maisha.game.ui.main

import android.content.res.Resources
import com.maisha.game.R
import com.maisha.game.data.model.CareerState
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.Job
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.util.formatMoney

object CareerFormatter {

    fun formatStatus(career: CareerState, res: Resources): String {
        val job = career.currentJob ?: return res.getString(R.string.career_unemployed)
        return res.getString(R.string.career_job_level, job.title, job.level)
    }

    fun formatSalary(job: Job, res: Resources, countryCode: String = "KE"): String {
        val levelMultiplier = 1.0 + (job.level - 1) * 0.15
        val annual = (job.baseSalary * levelMultiplier).toInt()
        return res.getString(R.string.format_salary_per_year, formatMoney(annual, countryCode))
    }

    fun formatEducationRequirement(stage: SchoolStage, res: Resources): String = when (stage) {
        SchoolStage.PRIMARY -> res.getString(R.string.edu_req_primary)
        SchoolStage.SECONDARY -> res.getString(R.string.edu_req_secondary)
        SchoolStage.UNIVERSITY -> res.getString(R.string.edu_req_university)
        SchoolStage.GRADUATED -> res.getString(R.string.edu_req_degree)
        SchoolStage.NONE -> res.getString(R.string.edu_req_none)
    }

    fun jobIneligibilityReason(character: Character, job: Job, res: Resources): String? {
        if (character.career.currentJob != null) return res.getString(R.string.job_ineligible_employed)
        if (character.age < 18) return res.getString(R.string.job_ineligible_age)
        val stageOrder = listOf(
            SchoolStage.NONE,
            SchoolStage.PRIMARY,
            SchoolStage.SECONDARY,
            SchoolStage.UNIVERSITY,
            SchoolStage.GRADUATED
        )
        val currentIndex = stageOrder.indexOf(character.education.stage)
        val requiredIndex = stageOrder.indexOf(job.minEducation)
        if (currentIndex < requiredIndex) {
            return res.getString(
                R.string.job_ineligible_education,
                formatEducationRequirement(job.minEducation, res)
            )
        }
        return null
    }

    fun isJobEligible(character: Character): Boolean {
        if (character.age < 18) return false
        return when (character.education.stage) {
            SchoolStage.SECONDARY,
            SchoolStage.UNIVERSITY,
            SchoolStage.GRADUATED -> true
            else -> false
        }
    }
}
