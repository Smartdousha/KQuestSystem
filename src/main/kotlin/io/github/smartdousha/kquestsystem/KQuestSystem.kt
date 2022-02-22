package io.github.smartdousha.kquestsystem

import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile

object KQuestSystem : Plugin() {
    @Config("config.yml")
    lateinit var conf: ConfigFile

    @Config("message.yml", autoReload = true)
    lateinit var mesconf: ConfigFile

    override fun onEnable() {
        info("KQuestSystem", "已成功加载！")
        warning("*************************************************************")
        warning("欢迎您使用 KQuestSystem 插件!")
        warning("本插件由豆沙 QQ:2446206510 为个人定制,未经允许不得发布,倒卖,魔改.")
        warning("*************************************************************")
    }

    override fun onDisable() {
        info("KQuestSystem", "已成功卸载！")
    }
}