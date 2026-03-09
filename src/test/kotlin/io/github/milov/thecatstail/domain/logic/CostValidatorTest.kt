package io.github.milov.thecatstail.domain.logic

import io.github.milov.thecatstail.domain.model.DiceCost
import io.github.milov.thecatstail.domain.model.Element
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CostValidatorTest {

    @Test
    fun `should satisfy exact same-element cost`() {
        val pool = mapOf(Element.PYRO to 3, Element.OMNI to 0)
        val cost = listOf(DiceCost(Element.PYRO, 3))
        assertTrue(CostValidator.canPay(pool, cost))
    }

    @Test
    fun `should satisfy same-element cost using Omni`() {
        val pool = mapOf(Element.PYRO to 1, Element.OMNI to 2)
        val cost = listOf(DiceCost(Element.PYRO, 3))
        assertTrue(CostValidator.canPay(pool, cost))
    }

    @Test
    fun `should fail same-element cost if not enough`() {
        val pool = mapOf(Element.PYRO to 1, Element.OMNI to 1)
        val cost = listOf(DiceCost(Element.PYRO, 3))
        assertFalse(CostValidator.canPay(pool, cost))
    }

    @Test
    fun `should satisfy unaligned cost using any element`() {
        val pool = mapOf(Element.PYRO to 1, Element.HYDRO to 1, Element.ANEMO to 1)
        val cost = listOf(DiceCost(Element.UNALIGNED, 3))
        assertTrue(CostValidator.canPay(pool, cost))
    }

    @Test
    fun `should satisfy complex cost`() {
        // 3 Pyro + 2 Unaligned
        val pool = mapOf(
            Element.PYRO to 2, 
            Element.OMNI to 2, 
            Element.HYDRO to 1, 
            Element.CRYO to 1
        )
        val cost = listOf(
            DiceCost(Element.PYRO, 3),
            DiceCost(Element.UNALIGNED, 2)
        )
        // 3 Pyro: 2 Pyro + 1 Omni (Remaining: 1 Omni, 1 Hydro, 1 Cryo)
        // 2 Unaligned: 1 Hydro + 1 Cryo (OR 1 Omni + 1 Hydro, etc.)
        assertTrue(CostValidator.canPay(pool, cost))
    }
}
