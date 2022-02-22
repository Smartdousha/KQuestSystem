package io.github.smartdousha.kquestsystem.command

import io.github.smartdousha.kquestsystem.KQuestSystem
import io.github.smartdousha.kquestsystem.KQuestSystem.conf
import io.github.smartdousha.kquestsystem.KQuestSystem.mesconf
import io.github.smartdousha.kquestsystem.bean.Quest
import io.github.smartdousha.kquestsystem.database.Database
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.command.*
import taboolib.common.platform.function.info
import taboolib.expansion.createHelper
import taboolib.library.xseries.XMaterial
import taboolib.library.xseries.XSound
import taboolib.module.nms.getI18nName
import taboolib.module.nms.getName
import taboolib.module.ui.ClickEvent
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Linked
import taboolib.platform.compat.depositBalance
import taboolib.platform.compat.getBalance
import taboolib.platform.compat.withdrawBalance
import taboolib.platform.util.*
import java.util.*


@CommandHeader(
    name = "KQuestSystem", aliases = ["kquest", "kq"], description = "任务系统", permissionDefault = PermissionDefault.TRUE
)
object Command {
    @CommandBody
    val main = mainCommand {
        createHelper()
    }

    @CommandBody(optional = true)
    val submit = subCommand {
        dynamic(commit = "材料物品数量") {
            execute<Player> { sender, context, _ ->
                val money = 0.0
                val questName = UUID.randomUUID().toString()
                try {
                    val materialAmount = context.argument(0).toInt()
                    if (sender.itemInHand.isAir) {
                        sender.sendMessage(ChatColor.RED.toString() + "你没有手持任何物品")
                        return@execute
                    }

                    try {
                        if (money >= 0) {
                            if (sender.getBalance() < money) {
                                sender.sendMessage(
                                    ChatColor.translateAlternateColorCodes(
                                        '&', mesconf.getString("submit.fail.money")!!
                                    )
                                )
                                return@execute
                            }
                        }
                        if (money < 0) {
                            sender.sendMessage(
                                ChatColor.translateAlternateColorCodes(
                                    '&', mesconf.getString("submit.fail.money-error")!!
                                )
                            )
                        }
                    } catch (e: Exception) {
                        sender.sendMessage(
                            ChatColor.translateAlternateColorCodes(
                                '&', mesconf.getString("submit.fail.money-exception")!!
                            )
                        )
                        return@execute
                    }

                    val itemInHand = sender.itemInHand
                    val itemNeed = itemInHand.clone()
                    itemNeed.amount = materialAmount
                    val materialString = Base64.getEncoder().encodeToString(itemNeed.serializeToByteArray())

                    if (itemInHand.amount < materialAmount) {
                        sender.sendMessage(
                            ChatColor.translateAlternateColorCodes(
                                '&', mesconf.getString("submit.fail.amount")!!
                            )
                        )
                        return@execute
                    }

                    sender.openMenu<Linked<ItemStack>>(title = "§7请放入您的奖励") {
                        rows(1)
                        onClick { event: ClickEvent ->
                            event.isCancelled = false
                        }

                        set(7, buildItem(XMaterial.GREEN_WOOL) {
                            name = "§a确认发布"
                        }) {
                            fun Inventory.removeItemInSlot(vararg slot: Int): Inventory {
                                slot.forEach {
                                    val item = this.getItem(it)
                                    if (item != null) {
                                        this.removeItem(item)
                                    }
                                }
                                return this
                            }
                            sender.sendMessage(
                                ChatColor.translateAlternateColorCodes(
                                    '&',
                                    mesconf.getString("submit.success.money")!!.replace("{money}", money.toString())
                                )
                            )
                            val inventoryString = Base64.getEncoder()
                                .encodeToString(inventory.removeItemInSlot(7, 8).serializeToByteArray())
                            info("${sender.name} 发布了任务 $questName")
                            itemInHand.amount -= materialAmount
                            sender.closeInventory()

                            sender.depositBalance(money)
                            Database.sql.insertDataToQuest(
                                Quest(
                                    publisher = sender.uniqueId.toString(),
                                    name = questName,
                                    awardMoney = money,
                                    materials = materialString,
                                    award = inventoryString
                                )
                            )
                        }


                        set(8, buildItem(XMaterial.RED_WOOL) {
                            name = "§c取消发布"
                        }) {
                            sender.closeInventory()
                        }
                    }
                } catch (e: NumberFormatException) {
                    sender.sendMessage(
                        ChatColor.translateAlternateColorCodes(
                            '&', mesconf.getString("submit.fail.material-amount-exception")!!
                        )
                    )
                }
            }

            dynamic(commit = "奖励金币", optional = true) {
                execute<Player> { sender, context, _ ->
                    try {
                        val money = context.argument(0).toDouble() ?: 0.0
                        val questName = UUID.randomUUID().toString()
                        val materialAmount = context.argument(-1).toInt()

                        if (sender.itemInHand.isAir) {
                            sender.sendMessage(ChatColor.RED.toString() + "你没有手持任何物品")
                            return@execute
                        }

                        val materialString =
                            Base64.getEncoder().encodeToString(sender.itemInHand.serializeToByteArray())

                        try {
                            if (money >= 0) {
                                if (sender.getBalance() < money) {
                                    sender.sendMessage(
                                        ChatColor.translateAlternateColorCodes(
                                            '&', mesconf.getString("submit.fail.money")!!
                                        )
                                    )
                                    return@execute
                                }
                            }
                            if (money < 0) {
                                sender.sendMessage(
                                    ChatColor.translateAlternateColorCodes(
                                        '&', mesconf.getString("submit.fail.money-error")!!
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            sender.sendMessage(
                                ChatColor.translateAlternateColorCodes(
                                    '&', mesconf.getString("submit.fail.money-exception")!!
                                )
                            )
                            return@execute
                        }

                        val itemInHand = sender.itemInHand

                        if (!sender.inventory.checkItem(itemInHand, materialAmount)) {
                            sender.sendMessage(
                                ChatColor.translateAlternateColorCodes(
                                    '&', mesconf.getString("submit.fail.material-not-enough")!!
                                )
                            )
                            return@execute
                        }



                        sender.openMenu<Linked<ItemStack>>("§7请放入您的奖励") {
                            rows(1)
                            onClick { event: ClickEvent ->
                                event.isCancelled = false
                            }
                            set(7, buildItem(XMaterial.GREEN_WOOL) {
                                name = "确认发布"
                            }) {
                                fun Inventory.removeItemInSlot(vararg slot: Int): Inventory {
                                    slot.forEach {
                                        val item = this.getItem(it)
                                        if (item != null) {
                                            this.removeItem(item)
                                        }
                                    }
                                    return this
                                }

                                if (!sender.checkItem(amount = materialAmount, item = itemInHand)) {
                                    sender.sendMessage(mesconf.getString("submit.fail.material-not-found"))
                                    sender.closeInventory()
                                    return@set
                                } else {
                                    sender.sendMessage(
                                        ChatColor.translateAlternateColorCodes(
                                            '&',
                                            mesconf.getString("submit.success.money")!!
                                                .replace("{money}", money.toString())
                                        )
                                    )
                                    val inventoryString = Base64.getEncoder()
                                        .encodeToString(inventory.removeItemInSlot(7, 8).serializeToByteArray())
                                    info("${sender.name} 发布了任务 $questName")
                                    itemInHand.amount -= materialAmount
                                    sender.closeInventory()

                                    sender.depositBalance(money)
                                    Database.sql.insertDataToQuest(
                                        Quest(
                                            publisher = sender.uniqueId.toString(),
                                            name = questName,
                                            awardMoney = money,
                                            materials = materialString,
                                            award = inventoryString
                                        )
                                    )
                                }
                            }
                            set(8, buildItem(XMaterial.RED_WOOL) {
                                name = "取消发布"
                            }) {
                                sender.sendMessage(
                                    ChatColor.translateAlternateColorCodes(
                                        '&', mesconf.getString("submit.fail.cancel")!!
                                    )
                                )
                                sender.closeInventory()
                            }
                        }
                    } catch (e: NumberFormatException) {
                        sender.sendMessage(
                            ChatColor.translateAlternateColorCodes(
                                '&', mesconf.getString("submit.fail.material-amount-exception")!!
                            )
                        )
                    }
                }
            }
        }
    }

    @CommandBody
    val menu = subCommand {
        execute<Player> { sender, context, argument ->
            sender.openMenu<Linked<ItemStack>>(title = conf.getString("menu.title")!!) {
                rows(6)
                slots(inventoryCenterSlots)
                val list = ArrayList<ItemStack>()
                val quests = Database.sql.getAllSubmitQuests()
                sender.playSound(sender.location, XSound.BLOCK_NOTE_BLOCK_PLING.parseSound()!!, 1f, 1f)
                if (quests.isNotEmpty()) {
                    quests.forEach { it ->
                        val questNotFinishedItemStack = buildItem(XMaterial.BOOK)
                        val itemInfoLore = ArrayList<String>()
                        if (it != null) {
                            if (it.award != null && it.materials != null) {
                                val materialItem = Base64.getDecoder().decode(it.materials).deserializeToItemStack()
                                if (materialItem.hasName()) {
                                    itemInfoLore.add(
                                        ChatColor.translateAlternateColorCodes(
                                            '&',
                                            conf.getString("menu.material")!!
                                                .replace("{material}", materialItem.itemMeta!!.displayName)
                                                .replace("{amount}", materialItem.amount.toString())
                                        )
                                    )
                                } else {
                                    itemInfoLore.add(
                                        ChatColor.translateAlternateColorCodes(
                                            '&',
                                            conf.getString("menu.material")!!
                                                .replace("{material}", materialItem.getI18nName())
                                                .replace("{amount}", materialItem.amount.toString())
                                        )
                                    )
                                }
                                Base64.getDecoder().decode(it.award).deserializeToInventory().contents.forEach { item ->
                                    if (item != null) {
                                        if (item.hasName()) {
                                            itemInfoLore.add(
                                                ChatColor.translateAlternateColorCodes(
                                                    '&',
                                                    conf.getString("menu.award")!!
                                                        .replace("{material}", item.itemMeta!!.displayName)
                                                        .replace("{amount}", item.amount.toString())
                                                )
                                            )
                                        } else {
                                            itemInfoLore.add(
                                                ChatColor.translateAlternateColorCodes(
                                                    '&',
                                                    conf.getString("menu.award")!!
                                                        .replace("{material}", item.getI18nName())
                                                        .replace("{amount}", item.amount.toString())
                                                )
                                            )
                                        }
                                    }
                                }

                                if (it.awardMoney != null) {
                                    itemInfoLore.add(
                                        ChatColor.translateAlternateColorCodes(
                                            '&',
                                            conf.getString("menu.awardMoney")!!
                                                .replace("{money}", it.awardMoney.toString())
                                        )
                                    )
                                }

                                val itemMeta = questNotFinishedItemStack.itemMeta
                                itemMeta!!.setDisplayName(it.name)
                                itemMeta.lore = itemInfoLore
                                questNotFinishedItemStack.itemMeta = itemMeta
                                list.add(questNotFinishedItemStack)
                            }
                        }
                    }
                }

                set(48, buildItem(XMaterial.CHEST) {
                    name = "${conf.getString("menu.receive")}"
                }) {
                    clicker.openMenu<Linked<ItemStack>>(title = conf.getString("menu.receive")!!) {
                        rows(6)
                        val data = Database.sql.fetchData(clicker)
                        val itemList = ArrayList<ItemStack>()
                        slots(inventoryCenterSlots)
                        if (data.isNotEmpty()) {
                            data.forEach {
                                if (it.isNotEmpty()) {
                                    val item = Base64.getDecoder().decode(it).deserializeToItemStack()
                                    itemList.add(item)
                                }
                            }
                        }

                        set(49, buildItem(XMaterial.GREEN_WOOL) {
                            name = "§a一键领取"
                        }) {
                            clicker.giveItem(itemList)
                            clicker.sendMessage(conf.getString("menu.success"))
                            Database.sql.deleteData(
                                clicker
                            )
                            clicker.closeInventory()
                        }


                        onClick { event, element ->
                            clicker.giveItem(itemList)
                            clicker.sendMessage(conf.getString("menu.success"))
                            Database.sql.deleteData(
                                clicker
                            )
                            clicker.closeInventory()
                        }

                        elements {
                            itemList
                        }

                        onGenerate { player, element, index, slot ->
                            element
                        }


                        setNextPage(53) { _, hasNextPage ->
                            if (hasNextPage) {
                                buildItem(XMaterial.SPECTRAL_ARROW) { name = "§7下一页" }
                            } else {
                                buildItem(XMaterial.ARROW) { name = "§8下一页" }
                            }
                        }
                        setPreviousPage(45) { _, hasPreviousPage ->
                            if (hasPreviousPage) {
                                buildItem(XMaterial.SPECTRAL_ARROW) { name = "§7上一页" }
                            } else {
                                buildItem(XMaterial.ARROW) { name = "§8上一页" }
                            }
                        }
                    }
                }

                //进入删除界面
                set(50, buildItem(XMaterial.PLAYER_HEAD) {
                    name = conf.getString("menu.private")
                }) {
                    sender.openMenu<Linked<ItemStack>>(title = conf.getString("menu.private")!!) {
                        sender.playSound(
                            sender.location, XSound.BLOCK_NOTE_BLOCK_PLING.parseSound()!!, 1f, 1f
                        )
                        rows(6)
                        slots(inventoryCenterSlots)
                        val playerSubmitQuests = Database.sql.getPlayerQuests(clicker)
                        val itemList = ArrayList<ItemStack>()

                        if (playerSubmitQuests.isNotEmpty()) {
                            playerSubmitQuests.forEach { quest ->
                                val needDeleteItem = buildItem(XMaterial.BOOK) {
                                    name = quest.name
                                    Base64.getDecoder().decode(quest.award)
                                        .deserializeToInventory().contents.forEach { item ->
                                            if (item != null) {
                                                lore.add(
                                                    ChatColor.translateAlternateColorCodes(
                                                        '&',
                                                        conf.getString("menu.award")!!
                                                            .replace("{material}", item.getName())
                                                            .replace("{amount}", item.amount.toString())
                                                    )
                                                )

                                                lore.add(
                                                    ChatColor.translateAlternateColorCodes(
                                                        '&',
                                                        conf.getString("menu.award")!!
                                                            .replace("{material}", item.getName())
                                                            .replace("{amount}", item.amount.toString())
                                                    )
                                                )
                                            }
                                        }
                                    lore.add(
                                        ChatColor.translateAlternateColorCodes(
                                            '&',
                                            conf.getString("menu.awardMoney")!!
                                                .replace("{money}", quest.awardMoney.toString())
                                        )
                                    )
                                }
                                itemList.add(needDeleteItem)
                            }
                        }

                        //进入删除界面
                        onClick { event, element ->
                            event.clicker.openMenu<Linked<ItemStack>>(title = "删除界面") {
                                event.clicker.playSound(
                                    event.clicker.location, XSound.BLOCK_NOTE_BLOCK_PLING.parseSound()!!, 1f, 1f
                                )

                                onClick { clickevent ->
                                    clickevent.clicker.playSound(
                                        sender.location, XSound.BLOCK_NOTE_BLOCK_PLING.parseSound()!!, 1f, 1f
                                    )
                                }

                                rows(1)
                                set(8, buildItem(XMaterial.RED_BED) {
                                    name = "§c退出"
                                }) {
                                    event.clicker.closeInventory()
                                }
                                set(7, buildItem(XMaterial.SLIME_BALL) {
                                    name = "§a确认删除"
                                }) {
                                    event.clicker.closeInventory()
                                    event.clicker.sendTitle("", "§a成功删除任务", 10, 20, 10)
                                    Database.sql.deleteSubmitQuest(event.currentItem!!.getName())
                                }
                            }
                        }

                        elements {
                            itemList
                        }

                        onGenerate { player, element, index, slot ->
                            element
                        }

                        setNextPage(53) { _, hasNextPage ->
                            if (hasNextPage) {
                                buildItem(XMaterial.SPECTRAL_ARROW) { name = "§7下一页" }
                            } else {
                                buildItem(XMaterial.ARROW) { name = "§8下一页" }
                            }
                        }
                        setPreviousPage(45) { _, hasPreviousPage ->
                            if (hasPreviousPage) {
                                buildItem(XMaterial.SPECTRAL_ARROW) { name = "§7上一页" }
                            } else {
                                buildItem(XMaterial.ARROW) { name = "§8上一页" }
                            }
                        }
                    }
                }

                onClick { event, element ->
                    val p = event.clicker
                    sender.playSound(sender.location, XSound.BLOCK_NOTE_BLOCK_PLING.parseSound()!!, 1f, 1f)
                    val questName = element.getName()

                    try {
                        val quest = Database.sql.getQuest(questName)
                        val materialItem = Base64.getDecoder().decode(quest!!.materials).deserializeToItemStack()

                        if (!p.checkItem(materialItem, amount = materialItem.amount, true)) {
                            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c你没有足够的材料！"))
                            return@onClick
                        }

                        val inv = Base64.getDecoder().decode(quest.award).deserializeToInventory()
                        inv.contents.forEach {
                            if (it != null) {
                                sender.inventory.addItem(it)
                            }
                        }

                        p.sendMessage(
                            ChatColor.translateAlternateColorCodes(
                                '&', mesconf.getString("quest.finished")!!
                            )
                        )

                        Database.sql.insertDataToPlayer(Bukkit.getOfflinePlayer(quest.publisher), quest)

                        if (quest.awardMoney != null && quest.awardMoney != 0.0) {
                            p.withdrawBalance(quest.awardMoney)
                            p.sendMessage(
                                ChatColor.translateAlternateColorCodes(
                                    '&',
                                    mesconf.getString("quest.money")!!.replace("{money}", quest.awardMoney.toString())
                                )
                            )
                        }
                        sender.closeInventory()
                        Database.sql.deleteSubmitQuest(questName)
                    } catch (e: NullPointerException) {
                        p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c该任务不存在！"))
                    }

                }

                elements {
                    list
                }

                onGenerate { player, element, index, slot ->
                    element
                }


                setNextPage(53) { _, hasNextPage ->
                    if (hasNextPage) {
                        buildItem(XMaterial.SPECTRAL_ARROW) { name = "§7下一页" }
                    } else {
                        buildItem(XMaterial.ARROW) { name = "§8下一页" }
                    }
                }
                setPreviousPage(45) { _, hasPreviousPage ->
                    if (hasPreviousPage) {
                        buildItem(XMaterial.SPECTRAL_ARROW) { name = "§7上一页" }
                    } else {
                        buildItem(XMaterial.ARROW) { name = "§8上一页" }
                    }
                }
            }

        }
    }

    @CommandBody
    val reload = subCommand {
        execute<CommandSender> { sender, context, argument ->
            if (sender.isOp) {
                sender.sendMessage(KQuestSystem.mesconf.getString("reload"))
                conf.reload()
            } else {
                sender.sendMessage(KQuestSystem.mesconf.getString("no-permission"))
            }
        }
    }
}

