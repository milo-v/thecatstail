package io.github.milov.thecatstail.domain.logic

import io.github.milov.thecatstail.domain.model.Element

data class ReactionResult(
    val name: String,
    val bonusDamage: Int = 0,
    val isPiercing: Boolean = false, 
    val nextElement: Element? = null, 
    val isFrozen: Boolean = false,
    val swirledElement: Element? = null,
    val isSwirl: Boolean = false
)

object ReactionManager {
    fun calculateReaction(applied: List<Element>, incoming: Element): ReactionResult? {
        if (applied.isEmpty()) return null
        
        // TCG typically only has 1 element applied at a time? 
        // Let's assume the first one.
        val current = applied.first()
        
        return when {
            // Pyro reactions
            (current == Element.PYRO && incoming == Element.HYDRO) || (current == Element.HYDRO && incoming == Element.PYRO) -> 
                ReactionResult("Vaporize", bonusDamage = 2)
            (current == Element.PYRO && incoming == Element.CRYO) || (current == Element.CRYO && incoming == Element.PYRO) -> 
                ReactionResult("Melt", bonusDamage = 2)
            (current == Element.PYRO && incoming == Element.ELECTRO) || (current == Element.ELECTRO && incoming == Element.PYRO) -> 
                ReactionResult("Overloaded", bonusDamage = 2) // Also forces switch in real game
            
            // Hydro reactions
            (current == Element.HYDRO && incoming == Element.ELECTRO) || (current == Element.ELECTRO && incoming == Element.HYDRO) -> 
                ReactionResult("Electro-Charged", bonusDamage = 1, isPiercing = true)
            (current == Element.HYDRO && incoming == Element.CRYO) || (current == Element.CRYO && incoming == Element.HYDRO) -> 
                ReactionResult("Frozen", bonusDamage = 1, isFrozen = true)
            
            // Dendro reactions
            (current == Element.DENDRO && incoming == Element.HYDRO) || (current == Element.HYDRO && incoming == Element.DENDRO) -> 
                ReactionResult("Bloom", bonusDamage = 2)
            (current == Element.DENDRO && incoming == Element.PYRO) || (current == Element.PYRO && incoming == Element.DENDRO) -> 
                ReactionResult("Burning", bonusDamage = 1) // Also creates a summon usually
            (current == Element.DENDRO && incoming == Element.ELECTRO) || (current == Element.ELECTRO && incoming == Element.DENDRO) -> 
                ReactionResult("Quicken", bonusDamage = 2)
            
            // Anemo (Swirl)
            (incoming == Element.ANEMO && current in listOf(Element.PYRO, Element.HYDRO, Element.ELECTRO, Element.CRYO)) -> 
                ReactionResult("Swirl", bonusDamage = 0, swirledElement = current, isSwirl = true)
                
            // Geo (Crystallize)
            (incoming == Element.GEO && current in listOf(Element.PYRO, Element.HYDRO, Element.ELECTRO, Element.CRYO)) -> 
                ReactionResult("Crystallize", bonusDamage = 1) // Usually gives a shield

            else -> null
        }
    }
}
