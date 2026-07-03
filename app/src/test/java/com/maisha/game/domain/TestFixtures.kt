package com.maisha.game.domain

import com.maisha.game.data.events.EventRepository
import com.maisha.game.data.model.Asset
import com.maisha.game.data.model.AssetType
import com.maisha.game.data.model.CareerState
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.CriminalRecord
import com.maisha.game.data.model.EducationState
import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.Job
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.Pet
import com.maisha.game.data.model.PetSpecies
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.Business
import com.maisha.game.data.model.HealthCondition
import com.maisha.game.data.model.SkillProgress
import com.maisha.game.data.model.SocialMediaState
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.Stats
import com.maisha.game.notifications.NotificationScheduler

object TestFixtures {

    fun character(
        name: String = "Test",
        age: Int = 25,
        gender: Gender = Gender.MALE,
        stats: Stats = Stats(),
        birthYear: Int = 2000,
        alive: Boolean = true,
        countryCode: String = "KE",
        family: List<Person> = emptyList(),
        education: EducationState = EducationState(),
        career: CareerState = CareerState(),
        assets: List<Asset> = emptyList(),
        pets: List<Pet> = emptyList(),
        socialMedia: SocialMediaState = SocialMediaState(),
        skills: List<SkillProgress> = emptyList(),
        businesses: List<Business> = emptyList(),
        avatarConfig: AvatarConfig = AvatarConfig.DEFAULT,
        activeConditions: List<HealthCondition> = emptyList(),
        criminalRecord: CriminalRecord = CriminalRecord(),
        generationNumber: Int = 1,
        relocationCount: Int = 0,
        relocationHistory: List<String> = emptyList(),
        lastRelocationAge: Int? = null
    ): Character = Character(
        name = name,
        age = age,
        gender = gender,
        stats = stats,
        birthYear = birthYear,
        alive = alive,
        countryCode = countryCode,
        birthCountryCode = countryCode,
        family = family,
        education = education,
        career = career,
        assets = assets,
        pets = pets,
        socialMedia = socialMedia,
        skills = skills,
        businesses = businesses,
        avatarConfig = avatarConfig,
        activeConditions = activeConditions,
        criminalRecord = criminalRecord,
        generationNumber = generationNumber,
        relocationCount = relocationCount,
        relocationHistory = relocationHistory,
        lastRelocationAge = lastRelocationAge
    )

    fun person(
        id: String = "p1",
        name: String = "Pat",
        relation: RelationType = RelationType.SIBLING,
        age: Int = 20,
        relationshipLevel: Int = 60,
        interactedThisYear: Boolean = false,
        gender: Gender = Gender.FEMALE,
        countryCode: String = "KE",
        isMarried: Boolean = false
    ): Person = Person(
        id = id,
        name = name,
        relation = relation,
        gender = gender,
        age = age,
        relationshipLevel = relationshipLevel,
        interactedThisYear = interactedThisYear,
        countryCode = countryCode,
        isMarried = isMarried
    )

    fun job(
        id: String = "teacher",
        title: String = "Teacher",
        baseSalary: Int = 500_000,
        level: Int = 1,
        performanceScore: Int = 50,
        minEducation: SchoolStage = SchoolStage.GRADUATED
    ): Job = Job(
        id = id,
        title = title,
        minEducation = minEducation,
        baseSalary = baseSalary,
        level = level,
        performanceScore = performanceScore
    )

    fun asset(
        id: String = "a1",
        currentValue: Int = 100_000,
        monthlyUpkeep: Int = 2_000,
        condition: Int = 100,
        type: AssetType = AssetType.MOTORBIKE,
        isHeirloom: Boolean = false,
        generationAcquired: Int = 1,
        name: String = "Test Asset"
    ): Asset = Asset(
        id = id,
        type = type,
        name = name,
        purchasePrice = currentValue,
        currentValue = currentValue,
        condition = condition,
        monthlyUpkeep = monthlyUpkeep,
        isHeirloom = isHeirloom,
        generationAcquired = generationAcquired
    )

    fun pet(
        id: String = "pet1",
        name: String = "Buddy",
        species: PetSpecies = PetSpecies.DOG,
        age: Int = 0,
        health: Int = 100,
        relationshipLevel: Int = 60
    ): Pet = Pet(
        id = id,
        name = name,
        species = species,
        age = age,
        health = health,
        relationshipLevel = relationshipLevel
    )

    fun child(
        id: String,
        age: Int,
        relationship: Int,
        relation: RelationType = RelationType.CHILD,
        gender: Gender = Gender.MALE,
        countryCode: String = "KE",
        avatarConfig: com.maisha.game.data.model.AvatarConfig = com.maisha.game.data.model.AvatarConfig.DEFAULT
    ): Person = Person(
        id = id,
        name = id,
        relation = relation,
        gender = gender,
        age = age,
        relationshipLevel = relationship,
        countryCode = countryCode,
        avatarConfig = avatarConfig
    )

    fun gameEngine(): GameEngine {
        val financeEngine = FinanceEngine()
        val healthEngine = HealthEngine()
        val eventRepository = EventRepository.forTesting(financeEngine)
        return GameEngine(
            eventRepository = eventRepository,
            educationEngine = EducationEngine(),
            careerEngine = CareerEngine(healthEngine),
            financeEngine = financeEngine,
            relationshipEngine = RelationshipEngine(FinanceEngine()),
            mortalityEngine = MortalityEngine(),
            crimeEngine = CrimeEngine(),
            healthEngine = healthEngine,
            achievementEngine = AchievementEngine(financeEngine),
            notificationScheduler = NotificationScheduler.forTesting(),
            relocationEngine = RelocationEngine(),
            socialMediaEngine = SocialMediaEngine(financeEngine),
            skillEngine = SkillEngine(),
            businessEngine = BusinessEngine(financeEngine)
        )
    }
}
