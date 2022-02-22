package io.github.smartdousha.kquestsystem.bean

import org.bukkit.entity.Player

data class Quest(
    val publisher: String,
    val name: String,
    val materials: String?,
    val award: String?,
    val awardMoney: Double?,
) {
    constructor(player: Player, name: String) : this(
        publisher = player.uniqueId.toString(),
        name,
        materials = null.toString(),
        award = null.toString(),
        awardMoney = 0.0
    )

    constructor(player: Player, name: String, money: Double?) : this(
        publisher = player.uniqueId.toString(),
        name,
        materials = null.toString(),
        award = null.toString(),
        awardMoney = money
    )

    constructor(player: Player, name: String, money: Double?, materials: String?) : this(
        publisher = player.uniqueId.toString(),
        name = name,
        materials = materials,
        awardMoney = money,
        award = null
    )
}

