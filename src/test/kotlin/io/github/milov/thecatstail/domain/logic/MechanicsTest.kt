package io.github.milov.thecatstail.domain.logic

import io.github.milov.thecatstail.domain.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MechanicsTest {

    private fun createTestMatch(): Match {
        val player1 = PlayerState("p1", listOf(
            Character("c1", "Char 1", Element.PYRO, maxEnergy = 3)
        ))
        val player2 = PlayerState("p2", listOf(
            Character("c2", "Char 2", Element.HYDRO, maxEnergy = 3)
        ))
        return Match("test-match", listOf(player1, player2))
    }

    @Test
    fun `summon trigger and expiration`() {
        val match = createTestMatch()
        val p1 = match.players[0]
        val p2 = match.players[1]

        val oz = Summon("oz", "Oz", Element.ELECTRO, 1, 2)
        p1.summonsZone.add(oz)

        // Round 1 End
        MatchManager.endActionPhase(match)
        assertEquals(9, p2.characters[0].currentHp)
        assertEquals(1, p1.summonsZone[0].usages)

        // Round 2 End
        MatchManager.endActionPhase(match)
        assertEquals(8, p2.characters[0].currentHp)
        assertTrue(p1.summonsZone.isEmpty(), "Summon should expire after 2 usages")
    }

    @Test
    fun `elemental tuning logic`() {
        val match = createTestMatch()
        val p1 = match.players[0] // Element is PYRO
        
        val card = ActionCard("test-card", "Test Card", ActionCardType.EVENT)
        p1.hand.add(card)
        p1.dicePool[Element.HYDRO] = 1
        p1.dicePool[Element.PYRO] = 0

        MatchManager.applyMove(match, "p1", Move.ElementalTuning(card.id, Element.HYDRO))

        assertEquals(0, p1.dicePool[Element.HYDRO])
        assertEquals(1, p1.dicePool[Element.PYRO], "Should have tuned Hydro to Pyro (Active Char Element)")
        assertTrue(p1.hand.isEmpty(), "Card should be consumed")
    }

    @Test
    fun `playing support card`() {
        val match = createTestMatch()
        val p1 = match.players[0]
        
        val paimonSupport = SupportCard("paimon", "Paimon", 2)
        val paimonCard = ActionCard("paimon", "Paimon", ActionCardType.SUPPORT, supportEffect = paimonSupport, cost = listOf(DiceCost(Element.UNALIGNED, 3)))
        
        p1.hand.add(paimonCard)
        p1.dicePool[Element.OMNI] = 10

        MatchManager.applyMove(match, "p1", Move.PlayCard(paimonCard.id))

        assertEquals(7, p1.dicePool[Element.OMNI])
        assertEquals(1, p1.supportZone.size)
        assertEquals("Paimon", p1.supportZone[0].name)
        assertTrue(p1.hand.isEmpty())
    }

    @Test
    fun `skill with summon effect`() {
        val match = createTestMatch()
        val p1 = match.players[0]
        
        val summon = Summon("oz", "Oz", Element.ELECTRO, 1, 2)
        val skill = Skill("skill", "Skill", SkillType.ELEMENTAL_SKILL, listOf(DiceCost(Element.UNALIGNED, 3)), 1, Element.ELECTRO, summonEffect = summon)
        
        // Setup character with this skill
        val char = Character("fischl", "Fischl", Element.ELECTRO, maxEnergy = 3, skills = listOf(skill))
        val updatedP1 = p1.copy(characters = listOf(char))
        val updatedMatch = Match(match.id, listOf(updatedP1, match.players[1]), 
                                 match.activePlayerIndex, match.roundNumber, match.phase, match.firstPlayerIndex)
        updatedMatch.players[0].dicePool[Element.OMNI] = 10

        MatchManager.applyMove(updatedMatch, "p1", Move.UseSkill(skill.id))

        assertEquals(1, updatedMatch.players[0].summonsZone.size)
        assertEquals("Oz", updatedMatch.players[0].summonsZone[0].name)
    }

    @Test
    fun `elemental infusion and status expiration`() {
        // 1. Setup
        val armorStatus = CharacterStatus("armor", "Armor", durationRounds = 2, elementOverride = Element.GEO, damageBonus = 2)
        val burstSkill = Skill("burst", "Burst", SkillType.ELEMENTAL_BURST, emptyList(), 4, Element.GEO, applyStatus = armorStatus)
        val normalAttack = Skill("normal", "Normal", SkillType.NORMAL_ATTACK, emptyList(), 2, Element.UNALIGNED)
        val noelle = Character("noelle", "Noelle", Element.GEO, maxEnergy = 2, currentEnergy = 2, skills = listOf(burstSkill, normalAttack))
        
        val p1 = PlayerState("p1", listOf(noelle))
        p1.dicePool[Element.OMNI] = 10
        val defender = Character("c2", "C2", Element.HYDRO, maxEnergy = 2, appliedElements = mutableListOf(Element.HYDRO))
        val p2 = PlayerState("p2", listOf(defender))
        
        val match = Match("test-match", listOf(p1, p2), 0, 1, Phase.ACTION, 0)
        
        // 2. Use Burst (GEO 4 vs HYDRO -> Crystallize +1 = 5 DMG)
        MatchManager.applyMove(match, "p1", Move.UseSkill("burst"))
        assertEquals(5, match.players[1].characters[0].currentHp, "Should deal 5 damage on burst (4 + 1 reaction)")
        assertTrue(match.players[1].characters[0].appliedElements.isEmpty(), "Crystallize should clear Hydro")
        
        // Reset turn to p1 for testing purposes (MatchManager switches turn after attack)
        match.activePlayerIndex = 0

        // 3. Use Normal Attack (without Hydro, should deal 2+2=4 DMG)
        MatchManager.applyMove(match, "p1", Move.UseSkill("normal"))
        assertEquals(1, match.players[1].characters[0].currentHp, "Should deal 4 damage on infused normal attack (2 base + 2 bonus)")
        assertEquals(Element.GEO, match.players[1].characters[0].appliedElements.first(), "Normal attack should apply GEO now")

        // 4. End Round 1 -> Duration 2 to 1
        MatchManager.endActionPhase(match)
        assertEquals(1, match.players[0].characters[0].activeStatuses[0].durationRounds)
        
        // 5. End Round 2 -> Expires
        MatchManager.endActionPhase(match)
        assertTrue(match.players[0].characters[0].activeStatuses.isEmpty(), "Status should expire")
    }
}
