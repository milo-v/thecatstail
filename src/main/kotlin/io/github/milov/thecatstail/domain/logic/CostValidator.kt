package io.github.milov.thecatstail.domain.logic

import io.github.milov.thecatstail.domain.model.DiceCost
import io.github.milov.thecatstail.domain.model.Element

object CostValidator {
    /**
     * Checks if the dice pool can satisfy the given cost.
     * @param dicePool Map of Element to Count
     * @param cost List of DiceCost (Element, Amount)
     * @return True if cost is satisfiable
     */
    fun canPay(dicePool: Map<Element, Int>, cost: List<DiceCost>): Boolean {
        val tempPool = dicePool.toMutableMap()
        
        // 1. Pay same-element costs first (Specific Element or Omni)
        for (c in cost) {
            if (c.element != Element.UNALIGNED) {
                var remaining = c.amount
                
                // Use specific element
                val specificAvailable = tempPool.getOrDefault(c.element, 0)
                val specificUsed = minOf(specificAvailable, remaining)
                tempPool[c.element] = specificAvailable - specificUsed
                remaining -= specificUsed
                
                // Use Omni for remaining
                if (remaining > 0) {
                    val omniAvailable = tempPool.getOrDefault(Element.OMNI, 0)
                    val omniUsed = minOf(omniAvailable, remaining)
                    tempPool[Element.OMNI] = omniAvailable - omniUsed
                    remaining -= omniUsed
                }
                
                if (remaining > 0) return false
            }
        }
        
        // 2. Pay unaligned costs (Any element, including remaining Omni)
        for (c in cost) {
            if (c.element == Element.UNALIGNED) {
                var remaining = c.amount
                
                // Sort pool to use Omni last? Actually, for unaligned, it doesn't matter unless we have other specific costs.
                // But we already paid specific costs.
                
                // Use non-Omni elements first for unaligned
                val otherElements = tempPool.keys.filter { it != Element.OMNI }.sortedBy { tempPool[it] }
                for (el in otherElements) {
                    val available = tempPool.getOrDefault(el, 0)
                    val used = minOf(available, remaining)
                    tempPool[el] = available - used
                    remaining -= used
                    if (remaining == 0) break
                }
                
                // Use Omni if still remaining
                if (remaining > 0) {
                    val omniAvailable = tempPool.getOrDefault(Element.OMNI, 0)
                    val omniUsed = minOf(omniAvailable, remaining)
                    tempPool[Element.OMNI] = omniAvailable - omniUsed
                    remaining -= omniUsed
                }
                
                if (remaining > 0) return false
            }
        }
        
        return true
    }
}
