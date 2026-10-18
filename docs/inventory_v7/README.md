# 0.4.4 物品栏宽度微调

以 0.4.3 为基准：刀刃宽度 ×5/4，刀鞘宽度 ×5/6。对应制作系数分别为 1.75 和 1.5416667。沿用刀柄左上、刀尖右下、刀鞘向右下凸及 B 背景；按更新轮廓重新居中。

完整／破损／无鞘图标为 66／60／44 三角面，仍符合性能预算。世界几何和世界材质区域像素逐项不变，资源验证通过。`item_blade.png` 等图片是实际模型的离线图标渲染，蓝色耐久框由游戏绘制，尚未进行客户端验收。

资源 `art/runtime_candidate_v7/`，工程 `art/blender/rivers_of_blood_runtime_v7.blend`。运行 `tools/export_blade_v4.py` 和 `tools/verify_inventory_v5.py`，Blender 参数 `-- --inventory-v7`。

测试包 `build/libs/EldenBlade-1.20.1-0.4.4-alpha.jar`；构建日志 `gradle-build.log`，包核验 `package_verification.json`。
