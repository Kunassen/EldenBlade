# 尸山血海配置与指令（0.4.0-alpha）

进入存档后，Forge 自动生成当前存档的 `serverconfig/eldenblade-server.toml`。单机通常为该整合包游戏目录下 `saves/<存档名>/serverconfig/eldenblade-server.toml`；服务器为实际世界目录下的 `serverconfig/eldenblade-server.toml`。不是全局 config 文件夹。不同存档互不影响；多人以服务器配置为准。

手动编辑推荐先退出存档/关闭服务器，修改后重新进入。游玩中调节请用指令，指令立即改变服务端设置并保存到同一文件。文件监视器重载会排到服务器线程处理，避免与同一 tick 内的指令/伤害结算并发修改运行参数。为新建存档提供整合包默认值，可将示例文件放到该实例的 `defaultconfigs/eldenblade-server.toml`；已有存档仍使用自己的 serverconfig。

## 默认值

```toml
[balance]
baseAttack = 24.0
saMaxHealthPercent = 10.0
seChancePercent = 10.0
seMaxHealthPercent = 10.0
seCooldownTicks = 2
```

完整中文注释模板位于 `config-examples/eldenblade-server.toml`。

## 指令（需要 2 级管理员权限，单机需开启作弊）

| 指令示例 | 含义 |
|---|---|
| `/eldenblade config` | 查看当前设置及路径说明 |
| `/eldenblade config attack 50` | 当前存档尸山血海默认基础面板攻击力设为 50 |
| `/eldenblade config sa_percent 5` | SA 每段扣 5% 最大生命，两段共 10% |
| `/eldenblade config se_chance 20` | SE 每次有效命中有 20% 概率触发 |
| `/eldenblade config se_percent 3` | SE 每次触发扣 3% 最大生命 |
| `/eldenblade config se_cooldown 10` | SE 冷却设为 10 tick，正常 20 TPS 下为 0.5 秒 |
| `/eldenblade attack 80` | 仅将主手尸山血海基础攻击力设为 80，写入此刀物品数据 |
| `/eldenblade attack reset` | 清除此刀独立攻击力，重新跟随当前存档默认值 |
| `/eldenblade give` | 获取使用当前默认攻击力的新刀 |

攻击力允许 0～1000000；百分比允许 0～100，支持小数；冷却允许 0～12000 整数 tick。概率 0 关闭 SE，概率 100 表示每次满足冷却条件的命中都触发。冷却 0 不限制同 tick 连续触发。百分比伤害 0 只关闭对应的额外扣血，不关闭 SA 普通两段攻击。

## 生效范围

- 新合成、命令获取的刀使用配置攻击力。已有刀在玩家主手或背包内会跟随默认值：主手每 tick 更新，背包每 20 tick 检查；箱子中的刀取入背包后更新。创造成品首次进入背包后同样应用配置。其他刀不受影响。
- 用 `/eldenblade attack` 单独设过数值的刀有持久物品覆盖，不受默认攻击力变化影响；`attack reset` 恢复跟随。覆盖随该物品转移；需要清除时使用 reset。
- 修改的是重锋的基础攻击参数。最终面板/实际伤害仍可能受锻造、附魔、玩家属性和整合包属性上限影响，不保证数值超过其他模组限制后仍等值作用。
- SA 两段各 150% 玩家攻击力、范围 7×7×7、间隔 3 tick 保持。最大生命百分比在释放时快照，本次两段采用同一比例；下一次 SA 使用新配置。
- SE 每次判定读取当前概率、伤害比例及冷却。已经开始的冷却保留到原定结束时刻，修改冷却适用于下一次成功触发。玩家全局冷却、SA 可触发 SE、SE 不递归的规则保持。
- 百分比扣血沿用现有破限伤实现；改变配置没有扩大已验证的 Boss 兼容范围。
