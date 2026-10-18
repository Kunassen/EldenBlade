# 柯罗诺斯预算优化 — 2026-10-18

比较对象：用户提供的 `prinegorerouse-1.3.0-all.jar`，命名刀 `chronosn_blade`，资源 `chronosn.obj/png`。其资源仅用于读取统计，没有复制进尸山血海。

## 同口径结果

单位均为三角面，四边面按 2 个三角面计。刀体分组包含刀柄、刀镡与装具；刀鞘包含绳结。

| 分组／资源 | 柯罗诺斯永境之主 | 尸山血海 v3 | 尸山血海 v4 |
|---|---:|---:|---:|
| blade | 1,175 | 3,218 | 1,166 |
| sheath | 413 | 1,722 | 412 |
| 上述基础合计 | 1,588 | 4,940 | 1,578 |
| blade_damaged | 1,062 | 尚未导出 | 898 |
| item_blade | 117 | 尚未导出 | 46 |
| item_damaged | 110 | 尚未导出 | 41 |
| item_bladens | 92 | 尚未导出 | 77 |
| blade_luminous + sheath_luminous | 85 | 0 | 0 |
| 颜色贴图 | 2048×2048 RGBA8 | 2048×2048 | 2048×2048 RGBA8 |
| 单图基础显存估算 | 16 MiB | 16 MiB | 16 MiB |
| 完整 OBJ 文件 | 458,001 字节 | 仅手持预览 | 220,689 字节 |
| PNG 文件 | 5,942,055 字节 | — | 4,443,448 字节 |

v4 基础面数比 v3 少 68.06%，比参考基础分组少 0.63%。原发布版 0.4.0 的 blade + sheath 为 4,864 面，v4 比它少 67.56%。这些比例只指几何数量，不代表同等帧率提升。

## 渲染路径与统计边界

已检查本地重锋 1.1.23 的 SlashBladeTEISR、LayerMainBlade、BladeItemEntityRenderer 和 BladeRenderState 字节码。正常手持／佩戴路径绘制 blade 或 blade_damaged，以及 sheath；另有对应 luminous pass。物品栏选择 item_blade 或 item_damaged，耐久框来自重锋共享资源。参考 JAR 未扫描到引用 RenderOverrideEvent 的 class。

参考 OBJ 还有 handle 1,032 面、effect 381 面、item_back 20 面和 blade_fragment 98 面；未将这些全部加进“正常刀＋鞘”统计，也没有把互斥的完整／破损分组同时累计。附魔闪光可再次提交基础几何，因此表格不是 GPU 总提交量。共同的耐久框等资源不计入单把刀的专属面数。

v4 不添加自定义逐帧渲染器、实时程序材质、额外法线／粗糙度贴图或发光几何层。SA 仍使用既有刀光与战斗逻辑，其实体、粒子、目标数量和服务器扣血开销未包含在这次模型比较中。不能据此宣称整个模组在所有情况下比参考模组更快。

相同 RGBA8 尺寸的贴图，基础层未压缩容量均为 16 MiB；若完整 mipmap 链上传，估算约 21.33 MiB。实际驱动分配未测，PNG 压缩文件大小不等于显存。

## 保留与取舍

- 保留非对称上挑刀尖、六处不同深浅的刃口缺损、363.125 单位总长度、既定加粗比例。
- 保留交叉立体缠带和鞘口布带，减少平顺刀段的截面采样。
- 红白浸染、血纹和雕饰烘焙到颜色贴图；游戏不计算制作阶段的复杂材质节点。
- 刀鞘保留短内壁与开口，移除看不到的长内管；绳结仍有体积，但近看棱角比高模明显。
- 物品栏三个状态从实际模型离线渲染到图集下方，每状态 512×512，按透明轮廓裁剪为 46／41／77 面，保留 B 背景。轮廓裁剪避免透明方角产生附魔闪光，也减少透明区域绘制。图标没有实时立体视差；世界手持／佩戴仍是三维模型。
- OBJ 复用位置、UV、法线索引，降低重复文本和加载数据；实际运行内存与帧时间仍需客户端测量。

## 产物与复现

- 原生候选资源：`../../art/runtime_candidate_v4/rivers_of_blood.obj`、`rivers_of_blood_v4.png`。
- 最终单图集 Blender 工程：`../../art/blender/rivers_of_blood_runtime_v4.blend`。
- 可编辑材质工程：`../../art/blender/rivers_of_blood_material_v4.blend`。
- 模型渲染：`07_baked_overview.png`、`08_baked_edge.png`、`09_baked_hilt.png`。
- 图标渲染：`item_blade.png`、`item_damaged.png`、`item_bladens.png`；蓝色耐久框由游戏提供。
- 机器报告：`performance.json`、`native_export.json`、`verification.json`。
- Blender 顺序执行 `tools/optimize_blade_v4.py`、`tools/bake_blade_v4.py`、`tools/export_blade_v4.py`、`tools/verify_blade_v4.py`；用 `--python-exit-code 1` 捕获脚本失败。v4 复用 v3 的刀形与烘焙代码，旧脚本和源工程需保留。

独立检查已通过：分组预算、合法索引、有限坐标、GameUV 范围与非退化性、贴图大小／通道、单材质／单图、六个缺口、刀背、刀尖和总长度。预览均为实际网格离线渲染，未完成 Minecraft 客户端视觉验收与同场景帧时间测试。

## 测试构建

`build/libs/EldenBlade-1.20.1-0.4.1-alpha.jar` 构建成功（离线、重锋 1.1.23、Forge 47.3.22）。包体 4,522,088 字节。与 0.4.0 比较，11 个游戏 class 字节完全一致，只有 mods.toml 版本、OBJ、PNG 内容变化；无需因本轮美术修改重复战斗机制测试。最终 JAR 资源与已验证源文件完全匹配，无第三方研究资源或 Blender 工程混入。检查记录 `package_verification.json`；SHA256 文件与 JAR 同目录。
