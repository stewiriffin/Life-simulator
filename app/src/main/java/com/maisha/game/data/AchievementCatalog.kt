// app/src/main/java/com/maisha/game/data/AchievementCatalog.kt (modified — string resource IDs)
package com.maisha.game.data

import com.maisha.game.R
import com.maisha.game.data.model.Achievement
import com.maisha.game.data.model.AchievementCategory

object AchievementCatalog {

    val all: List<Achievement> = listOf(
        Achievement(
            id = "first_job",
            titleRes = R.string.achievement_first_job_title,
            descriptionRes = R.string.achievement_first_job_description,
            category = AchievementCategory.CAREER,
            iconName = "briefcase"
        ),
        Achievement(
            id = "corner_office",
            titleRes = R.string.achievement_corner_office_title,
            descriptionRes = R.string.achievement_corner_office_description,
            category = AchievementCategory.CAREER,
            iconName = "office"
        ),
        Achievement(
            id = "career_changer",
            titleRes = R.string.achievement_career_changer_title,
            descriptionRes = R.string.achievement_career_changer_description,
            category = AchievementCategory.CAREER,
            iconName = "shuffle"
        ),
        Achievement(
            id = "graduate",
            titleRes = R.string.achievement_graduate_title,
            descriptionRes = R.string.achievement_graduate_description,
            category = AchievementCategory.EDUCATION,
            iconName = "graduation_cap"
        ),
        Achievement(
            id = "straight_as",
            titleRes = R.string.achievement_straight_as_title,
            descriptionRes = R.string.achievement_straight_as_description,
            category = AchievementCategory.EDUCATION,
            iconName = "star"
        ),
        Achievement(
            id = "dropout",
            titleRes = R.string.achievement_dropout_title,
            descriptionRes = R.string.achievement_dropout_description,
            category = AchievementCategory.EDUCATION,
            iconName = "door"
        ),
        Achievement(
            id = "tied_the_knot",
            titleRes = R.string.achievement_tied_the_knot_title,
            descriptionRes = R.string.achievement_tied_the_knot_description,
            category = AchievementCategory.FAMILY,
            iconName = "rings"
        ),
        Achievement(
            id = "first_child",
            titleRes = R.string.achievement_first_child_title,
            descriptionRes = R.string.achievement_first_child_description,
            category = AchievementCategory.FAMILY,
            iconName = "baby"
        ),
        Achievement(
            id = "growing_family",
            titleRes = R.string.achievement_growing_family_title,
            descriptionRes = R.string.achievement_growing_family_description,
            category = AchievementCategory.FAMILY,
            iconName = "family"
        ),
        Achievement(
            id = "family_person",
            titleRes = R.string.achievement_family_person_title,
            descriptionRes = R.string.achievement_family_person_description,
            category = AchievementCategory.FAMILY,
            iconName = "heart"
        ),
        Achievement(
            id = "six_figures",
            titleRes = R.string.achievement_six_figures_title,
            descriptionRes = R.string.achievement_six_figures_description,
            category = AchievementCategory.WEALTH,
            iconName = "coins"
        ),
        Achievement(
            id = "first_million",
            titleRes = R.string.achievement_first_million_title,
            descriptionRes = R.string.achievement_first_million_description,
            category = AchievementCategory.WEALTH,
            iconName = "million"
        ),
        Achievement(
            id = "property_owner",
            titleRes = R.string.achievement_property_owner_title,
            descriptionRes = R.string.achievement_property_owner_description,
            category = AchievementCategory.WEALTH,
            iconName = "house"
        ),
        Achievement(
            id = "multiple_streams",
            titleRes = R.string.achievement_multiple_streams_title,
            descriptionRes = R.string.achievement_multiple_streams_description,
            category = AchievementCategory.WEALTH,
            iconName = "portfolio"
        ),
        Achievement(
            id = "half_century",
            titleRes = R.string.achievement_half_century_title,
            descriptionRes = R.string.achievement_half_century_description,
            category = AchievementCategory.LONGEVITY,
            iconName = "calendar"
        ),
        Achievement(
            id = "golden_years",
            titleRes = R.string.achievement_golden_years_title,
            descriptionRes = R.string.achievement_golden_years_description,
            category = AchievementCategory.LONGEVITY,
            iconName = "sunset"
        ),
        Achievement(
            id = "centenarian",
            titleRes = R.string.achievement_centenarian_title,
            descriptionRes = R.string.achievement_centenarian_description,
            category = AchievementCategory.LONGEVITY,
            iconName = "crown"
        ),
        Achievement(
            id = "brush_with_law",
            titleRes = R.string.achievement_brush_with_law_title,
            descriptionRes = R.string.achievement_brush_with_law_description,
            category = AchievementCategory.MISCHIEF,
            iconName = "handcuffs"
        ),
        Achievement(
            id = "repeat_offender",
            titleRes = R.string.achievement_repeat_offender_title,
            descriptionRes = R.string.achievement_repeat_offender_description,
            category = AchievementCategory.MISCHIEF,
            iconName = "repeat"
        ),
        Achievement(
            id = "clean_record",
            titleRes = R.string.achievement_clean_record_title,
            descriptionRes = R.string.achievement_clean_record_description,
            category = AchievementCategory.MISCHIEF,
            iconName = "shield"
        ),
        Achievement(
            id = "world_citizen",
            titleRes = R.string.achievement_world_citizen_title,
            descriptionRes = R.string.achievement_world_citizen_description,
            category = AchievementCategory.FAMILY,
            iconName = "globe"
        ),
        Achievement(
            id = "inseparable",
            titleRes = R.string.achievement_inseparable_title,
            descriptionRes = R.string.achievement_inseparable_description,
            category = AchievementCategory.FAMILY,
            iconName = "inseparable"
        ),
        Achievement(
            id = "second_generation",
            titleRes = R.string.achievement_second_generation_title,
            descriptionRes = R.string.achievement_second_generation_description,
            category = AchievementCategory.FAMILY,
            iconName = "tree"
        ),
        Achievement(
            id = "dynasty_builder",
            titleRes = R.string.achievement_dynasty_builder_title,
            descriptionRes = R.string.achievement_dynasty_builder_description,
            category = AchievementCategory.FAMILY,
            iconName = "dynasty"
        ),
        Achievement(
            id = "true_friend",
            titleRes = R.string.achievement_true_friend_title,
            descriptionRes = R.string.achievement_true_friend_description,
            category = AchievementCategory.FAMILY,
            iconName = "handshake"
        ),
        Achievement(
            id = "social_circle",
            titleRes = R.string.achievement_social_circle_title,
            descriptionRes = R.string.achievement_social_circle_description,
            category = AchievementCategory.FAMILY,
            iconName = "friends"
        )
    )

    fun byId(id: String): Achievement? = all.find { it.id == id }
}
