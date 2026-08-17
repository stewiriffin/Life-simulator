package com.maisha.game.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.maisha.game.R
import com.maisha.game.domain.LeisureActivity

@Composable
fun leisureTitle(activity: LeisureActivity): String = when (activity) {
    LeisureActivity.STORYTIME -> stringResource(R.string.leisure_storytime_title)
    LeisureActivity.PARK_CAREGIVER -> stringResource(R.string.leisure_park_caregiver_title)
    LeisureActivity.NAP_ROUTINE -> stringResource(R.string.leisure_nap_routine_title)
    LeisureActivity.PLAYDATE -> stringResource(R.string.leisure_playdate_title)
    LeisureActivity.PLAYGROUND -> stringResource(R.string.leisure_playground_title)
    LeisureActivity.STUDY_BUDDY -> stringResource(R.string.leisure_study_buddy_title)
    LeisureActivity.CHORES -> stringResource(R.string.leisure_chores_title)
    LeisureActivity.NIGHT_OUT -> stringResource(R.string.leisure_night_out_title)
    LeisureActivity.NATURE_DAY -> stringResource(R.string.leisure_nature_day_title)
    LeisureActivity.CITY_SHOW -> stringResource(R.string.leisure_city_show_title)
    LeisureActivity.SPA_DAY -> stringResource(R.string.leisure_spa_day_title)
    LeisureActivity.WEEKEND_RETREAT -> stringResource(R.string.leisure_weekend_retreat_title)
    LeisureActivity.REUNION_DINNER -> stringResource(R.string.leisure_reunion_dinner_title)
    LeisureActivity.GRANDKIDS_DAY -> stringResource(R.string.leisure_grandkids_day_title)
    LeisureActivity.COMMUNITY_CLUB -> stringResource(R.string.leisure_community_club_title)
    LeisureActivity.MEMOIR_WRITING -> stringResource(R.string.leisure_memoir_title)
    LeisureActivity.LIBRARY_VISIT -> stringResource(R.string.leisure_library_title)
    LeisureActivity.MEDITATION -> stringResource(R.string.leisure_meditation_title)
    LeisureActivity.ANNUAL_CHECKUP -> stringResource(R.string.leisure_checkup_title)
}

@Composable
fun leisureDescription(activity: LeisureActivity): String = when (activity) {
    LeisureActivity.STORYTIME -> stringResource(R.string.leisure_storytime_desc)
    LeisureActivity.PARK_CAREGIVER -> stringResource(R.string.leisure_park_caregiver_desc)
    LeisureActivity.NAP_ROUTINE -> stringResource(R.string.leisure_nap_routine_desc)
    LeisureActivity.PLAYDATE -> stringResource(R.string.leisure_playdate_desc)
    LeisureActivity.PLAYGROUND -> stringResource(R.string.leisure_playground_desc)
    LeisureActivity.STUDY_BUDDY -> stringResource(R.string.leisure_study_buddy_desc)
    LeisureActivity.CHORES -> stringResource(R.string.leisure_chores_desc)
    LeisureActivity.NIGHT_OUT -> stringResource(R.string.leisure_night_out_desc)
    LeisureActivity.NATURE_DAY -> stringResource(R.string.leisure_nature_day_desc)
    LeisureActivity.CITY_SHOW -> stringResource(R.string.leisure_city_show_desc)
    LeisureActivity.SPA_DAY -> stringResource(R.string.leisure_spa_day_desc)
    LeisureActivity.WEEKEND_RETREAT -> stringResource(R.string.leisure_weekend_retreat_desc)
    LeisureActivity.REUNION_DINNER -> stringResource(R.string.leisure_reunion_dinner_desc)
    LeisureActivity.GRANDKIDS_DAY -> stringResource(R.string.leisure_grandkids_day_desc)
    LeisureActivity.COMMUNITY_CLUB -> stringResource(R.string.leisure_community_club_desc)
    LeisureActivity.MEMOIR_WRITING -> stringResource(R.string.leisure_memoir_desc)
    LeisureActivity.LIBRARY_VISIT -> stringResource(R.string.leisure_library_desc)
    LeisureActivity.MEDITATION -> stringResource(R.string.leisure_meditation_desc)
    LeisureActivity.ANNUAL_CHECKUP -> stringResource(R.string.leisure_checkup_desc)
}

@Composable
fun leisureEffectLabel(activity: LeisureActivity): String = when (activity) {
    LeisureActivity.STORYTIME -> stringResource(R.string.leisure_effect_storytime)
    LeisureActivity.PARK_CAREGIVER -> stringResource(R.string.leisure_effect_park)
    LeisureActivity.NAP_ROUTINE -> stringResource(R.string.leisure_effect_nap)
    LeisureActivity.PLAYDATE -> stringResource(R.string.leisure_effect_playdate)
    LeisureActivity.PLAYGROUND -> stringResource(R.string.leisure_effect_playground)
    LeisureActivity.STUDY_BUDDY -> stringResource(R.string.leisure_effect_study)
    LeisureActivity.CHORES -> stringResource(R.string.leisure_effect_chores)
    LeisureActivity.NIGHT_OUT -> stringResource(R.string.leisure_effect_night_out)
    LeisureActivity.NATURE_DAY -> stringResource(R.string.leisure_effect_nature_day)
    LeisureActivity.CITY_SHOW -> stringResource(R.string.leisure_effect_city_show)
    LeisureActivity.SPA_DAY -> stringResource(R.string.leisure_effect_spa_day)
    LeisureActivity.WEEKEND_RETREAT -> stringResource(R.string.leisure_effect_retreat)
    LeisureActivity.REUNION_DINNER -> stringResource(R.string.leisure_effect_reunion)
    LeisureActivity.GRANDKIDS_DAY -> stringResource(R.string.leisure_effect_grandkids)
    LeisureActivity.COMMUNITY_CLUB -> stringResource(R.string.leisure_effect_community)
    LeisureActivity.MEMOIR_WRITING -> stringResource(R.string.leisure_effect_memoir)
    LeisureActivity.LIBRARY_VISIT -> stringResource(R.string.leisure_effect_library)
    LeisureActivity.MEDITATION -> stringResource(R.string.leisure_effect_meditation)
    LeisureActivity.ANNUAL_CHECKUP -> stringResource(R.string.leisure_effect_checkup)
}

fun leisureMessageRes(activity: LeisureActivity): Int = when (activity) {
    LeisureActivity.STORYTIME -> R.string.msg_leisure_storytime
    LeisureActivity.PARK_CAREGIVER -> R.string.msg_leisure_park
    LeisureActivity.NAP_ROUTINE -> R.string.msg_leisure_nap
    LeisureActivity.PLAYDATE -> R.string.msg_leisure_playdate
    LeisureActivity.PLAYGROUND -> R.string.msg_leisure_playground
    LeisureActivity.STUDY_BUDDY -> R.string.msg_leisure_study_buddy
    LeisureActivity.CHORES -> R.string.msg_leisure_chores
    LeisureActivity.NIGHT_OUT -> R.string.msg_leisure_night_out
    LeisureActivity.NATURE_DAY -> R.string.msg_leisure_nature_day
    LeisureActivity.CITY_SHOW -> R.string.msg_leisure_city_show
    LeisureActivity.SPA_DAY -> R.string.msg_leisure_spa_day
    LeisureActivity.WEEKEND_RETREAT -> R.string.msg_leisure_weekend_retreat
    LeisureActivity.REUNION_DINNER -> R.string.msg_leisure_reunion_dinner
    LeisureActivity.GRANDKIDS_DAY -> R.string.msg_leisure_grandkids
    LeisureActivity.COMMUNITY_CLUB -> R.string.msg_leisure_community_club
    LeisureActivity.MEMOIR_WRITING -> R.string.msg_leisure_memoir
    LeisureActivity.LIBRARY_VISIT -> R.string.msg_leisure_library
    LeisureActivity.MEDITATION -> R.string.msg_leisure_meditation
    LeisureActivity.ANNUAL_CHECKUP -> R.string.msg_leisure_checkup
}
