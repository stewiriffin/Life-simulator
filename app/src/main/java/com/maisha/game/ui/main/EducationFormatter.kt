// app/src/main/java/com/maisha/game/ui/main/EducationFormatter.kt
package com.maisha.game.ui.main

import android.content.res.Resources
import com.maisha.game.R
import com.maisha.game.data.model.EducationState
import com.maisha.game.data.model.SchoolStage

object EducationFormatter {

    fun formatStatus(education: EducationState, res: Resources): String {
        if (education.expelled) return res.getString(R.string.edu_expelled)
        return when (education.stage) {
            SchoolStage.NONE -> res.getString(R.string.edu_not_enrolled)
            SchoolStage.PRIMARY -> {
                val school = education.schoolName ?: res.getString(R.string.edu_primary_school_fallback)
                res.getString(R.string.edu_primary_class, education.currentGrade, school)
            }
            SchoolStage.SECONDARY -> {
                val school = education.schoolName ?: res.getString(R.string.edu_secondary_school_fallback)
                res.getString(R.string.edu_secondary_form, education.currentGrade, school)
            }
            SchoolStage.UNIVERSITY -> {
                val course = education.courseOfStudy ?: res.getString(R.string.edu_general_studies)
                val school = education.schoolName ?: res.getString(R.string.edu_university_fallback)
                res.getString(R.string.edu_university_year, education.currentGrade, course, school)
            }
            SchoolStage.GRADUATED -> {
                val course = education.courseOfStudy ?: res.getString(R.string.edu_your_studies)
                res.getString(R.string.edu_graduated, course)
            }
        }
    }

    fun formatHighestEducation(education: EducationState, res: Resources): String {
        if (education.expelled) return res.getString(R.string.edu_expelled_from_school)
        return when (education.stage) {
            SchoolStage.GRADUATED -> {
                val course = education.courseOfStudy ?: res.getString(R.string.edu_university_fallback)
                res.getString(R.string.edu_graduated, course)
            }
            SchoolStage.UNIVERSITY -> {
                val course = education.courseOfStudy ?: res.getString(R.string.edu_general_studies)
                res.getString(R.string.edu_university_in_progress, course)
            }
            SchoolStage.SECONDARY -> res.getString(R.string.edu_secondary_school)
            SchoolStage.PRIMARY -> res.getString(R.string.edu_primary_school)
            SchoolStage.NONE -> res.getString(R.string.edu_no_formal_education)
        }
    }
}
