package io.github.smartdousha.kquestsystem.database

import io.github.smartdousha.kquestsystem.KQuestSystem.conf
import io.github.smartdousha.kquestsystem.bean.Quest
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.module.database.*

object Database {
    lateinit var sql: SQL

    @Awake(LifeCycle.ENABLE)
    fun connectSQL() {
        if (conf.contains("database.host")) {
            sql = try {
                SQL()
            } catch (ex: Exception) {
                ex.printStackTrace()
                return
            }
            info("KQuestSystem", "§a跨服数据库连接成功!")
        }
    }

    class SQL {
        private val host = conf.getHost("database")
        private val tQuest = Table("t_quest", host) {
            /**
             * 主键 发布者uid
             */
            add("publisher_uuid") {
                type(ColumnTypeSQL.VARCHAR, 36) {
                    options(ColumnOptionSQL.NOTNULL)
                }
            }
            /**
             * 任务UUID
             */
            add("quest_uuid") {
                type(ColumnTypeSQL.VARCHAR, 36) {
                    options(ColumnOptionSQL.NOTNULL, ColumnOptionSQL.UNIQUE_KEY, ColumnOptionSQL.PRIMARY_KEY)
                }
            }

            /**
             * 任务材料
             */
            add("materials") {
                type(ColumnTypeSQL.TEXT)
            }

            /**
             * 任务奖励
             */
            add("award") {
                type(ColumnTypeSQL.TEXT)
            }

            /**
             * 任务奖励金币
             */
            add("awardmoney") {
                type(ColumnTypeSQL.DOUBLE)
            }

        }

        private val tPlayer = Table(name = "t_quest_player", host) {
            add("player_uuid") {
                type(ColumnTypeSQL.VARCHAR, 36) {
                    options(ColumnOptionSQL.NOTNULL)
                }
            }
            add("inventory") {
                type(ColumnTypeSQL.TEXT)
            }
        }

        private val dataSource = host.createDataSource()

        init {
            tQuest.workspace(dataSource) {
                createTable()
            }.run()

            tPlayer.workspace(dataSource) {
                createTable()
            }.run()
        }

        fun insertDataToPlayer(player: OfflinePlayer, quest: Quest) {
            tPlayer.workspace(dataSource) {
                insert("player_uuid", "inventory") {
                    value(
                        quest.publisher, quest.materials!!
                    )
                }
            }.run()
        }

        fun fetchData(player: OfflinePlayer): List<String> {
            return tPlayer.workspace(dataSource) {
                select {
                    rows("inventory")
                    where("player_uuid" eq player.uniqueId.toString())
                }
            }.map {
                getString("inventory")
            }
        }

        fun deleteData(player: OfflinePlayer) {
            tPlayer.workspace(dataSource) {
                delete {
                    where(("player_uuid" eq player.uniqueId.toString()))
                }
            }.run()
        }

        fun updateDataToPlayer(player: OfflinePlayer, quest: Quest) {
            tPlayer.workspace(dataSource) {
                update {
                    set("inventory", quest.materials)
                    where("player_uuid" eq player.uniqueId.toString())
                }
            }.run()
        }


        fun getQuestName(uuid: String) {
            tQuest.workspace(dataSource) {
                select {
                    where("quest_uuid" eq uuid.toString())
                }
            }.firstOrNull {
                val name = getString("publisher_uuid")
            }
        }

        fun getQuestMaterials(uuid: String) {
            tQuest.workspace(dataSource) {
                select {
                    where("quest_uuid" eq uuid)
                }
            }.firstOrNull {
                val materials = getString("materials")
            }
        }

        fun getQuestAward(uuid: String) {
            tQuest.workspace(dataSource) {
                select {
                    where("quest_uuid" eq uuid)
                }
            }.firstOrNull {
                val award = getString("award")
            }
        }

        fun getQuestAwardMoney(uuid: String) {
            tQuest.workspace(dataSource) {
                select {
                    where("quest_uuid" eq uuid)
                }
            }.firstOrNull {
                getInt("awardmoney")
            }
        }

        fun playerHasQuest(uuid: String, questName: String): Boolean {
            return tQuest.workspace(dataSource) {
                select {
                    where("quest_uuid" eq uuid and ("publisher_uuid" eq questName))
                }
            }.find()
        }

        fun updateMaterials(uuid: String, materials: String, name: String) {
            tQuest.workspace(dataSource) {
                update {
                    set("materials", materials)
                    where("quest_uuid" eq uuid and ("publisher_uuid" eq name))
                }
            }.run()
        }

        fun getQuestMaterial(uuid: String, name: String): String? {
            return tQuest.workspace(dataSource) {
                select {
                    where("quest_uuid" eq uuid and ("publisher_uuid" eq name))
                }
            }.firstOrNull {
                getString("materials")
            }
        }

        fun updateAward(uuid: String, award: String, name: String) {
            tQuest.workspace(dataSource) {
                update {
                    set("award", award)
                    where("quest_uuid" eq uuid and ("publisher_uuid" eq name))
                }
            }.run()
        }


        fun insertDataToQuest(quest: Quest) {
            tQuest.workspace(dataSource) {
                insert("publisher_uuid", "quest_uuid", "awardmoney", "award", "materials") {
                    value(
                        quest.publisher, quest.name, quest.awardMoney!!, quest.award!!, quest.materials!!
                    )
                }
            }.run()
        }


        fun updateQuestName(name: String, player: Player) {
            tQuest.workspace(dataSource) {
                update {
                    set("publisher_uuid", "${getQuestName(player.uniqueId.toString())},$name")
                    where("quest_uuid" eq player.uniqueId.toString())
                }
            }.run()
        }

        fun userHasPublished(uuid: String): Boolean {
            return tQuest.workspace(dataSource) {
                select {
                    where {
                        "quest_uuid" eq uuid
                    }
                }
            }.find()
        }

        fun deleteSubmitQuest(questName: String) {
            tQuest.workspace(dataSource) {
                delete {
                    where("quest_uuid" eq questName)
                }
            }.run()
        }


        fun getQuestName(player: Player): String? {
            return tQuest.workspace(dataSource) {
                select {
                    where("publisher_uuid" eq player.uniqueId.toString())
                }
            }.firstOrNull {
                getString("quest_uuid")
            }
        }

        fun getPlayerQuests(player: Player): List<Quest> {
            return tQuest.workspace(dataSource) {
                select {
                    where("publisher_uuid" eq player.uniqueId.toString())
                }
            }.map {
                val name = getString("quest_uuid")
                val awardMoney = getDouble("awardmoney")
                val materials = getString("materials")
                val award = getString("award")
                val uid = getString("publisher_uuid")
                Quest(
                    name = name, awardMoney = awardMoney, materials = materials, award = award, publisher = uid
                )
            }
        }

        fun getQuest(questName: String): Quest? {
            return tQuest.workspace(dataSource) {
                select {
                    where("quest_uuid" eq questName)
                }
            }.firstOrNull {
                val name = getString("quest_uuid")
                val awardMoney = getDouble("awardmoney")
                val materials = getString("materials")
                val award = getString("award")
                val uid = getString("publisher_uuid")
                Quest(
                    name = name, awardMoney = awardMoney, materials = materials, award = award, publisher = uid
                )
            }
        }

        fun splitStringWithChar(str: String, char: String): List<String> {
            val list = mutableListOf<String>()
            var temp = ""
            for (i in str.indices) {
                if (str[i] == char[0]) {
                    list.add(temp)
                    temp = ""
                } else {
                    temp += str[i]
                }
            }
            list.add(temp)
            return list
        }

        fun getPlayerSubmitQuests(player: Player): List<String> {
            return tQuest.workspace(dataSource) {
                select {
                    rows("quest_uuid")
                    where("publisher_uuid" eq player.uniqueId.toString())
                }
            }.map {
                getString("quest_uuid")
            }
        }

        fun getAllSubmitQuests(): List<Quest?> {
            return tQuest.workspace(dataSource) {
                select {
                    rows("quest_uuid", "publisher_uuid", "awardmoney", "award", "materials")
                }
            }.map {
                val quest = Quest(
                    name = getString("quest_uuid"),
                    award = getString("award") ?: null,
                    materials = getString("materials") ?: null,
                    publisher = getString("publisher_uuid"),
                    awardMoney = getDouble("awardmoney")
                )
                quest
            }
        }

        fun userHasSubmitQuest(player: Player, questName: String): Boolean {
            return tQuest.workspace(dataSource) {
                select {
                    where("uid" eq player.uniqueId.toString() and ("publisher_uuid" eq questName))
                }
            }.find()
        }

        private fun hasNullField(obj: Any): Boolean {
            val fields = obj::class.java.declaredFields
            for (field in fields) {
                field.isAccessible = true
                if (field.get(obj) == null) {
                    return true
                }
            }
            return false
        }

    }
}