# 素材与来源

## 1.1.2 的专用 SA 动作

`combostate/corpse_piler.vmd` 派生自 SlashBlade Resharped 1.1.23 内的
`assets/slashblade/combostate/motion.vmd`，原动作归原项目作者所有。
本项目只调整刀身骨骼 JointA1 的前后位移，重现脚本为
`tools/prepare_sa_motion.py`。仅尸横遍野使用此资源，不覆盖原模组动画。

## 1.1.1 的扇形 SA 重做

以用户补充的视频多视角作为形状参考，运行时几何由本项目代码构建。
新贴图通过内置 imagegen 生成，替换 1.1.0 的旧血带：
`src/main/resources/assets/eldenblade/textures/entity/blood_sweep.png`。
保留真实 RGBA 透明通道，尺寸为 2172×724；未复用旧血带的湿亮纹理。
1.1.1 的音效文件与 1.1.0 相同，未从补充视频重新提取声音。

最终提示词：

> Production VFX texture atlas, one straight unwrapped rectangular slash sweep field on a real transparent background. Aspect ratio 3:1, preferably 1536x512. This is a UV texture for a radial crescent slash, NOT an illustration of a crescent. Texture X maps along the swept arc; texture Y maps from outer rim at TOP to inner radius at BOTTOM. A thin crisp scarlet RED upper boundary at y about 15%, many fine parallel horizontal blood-red motion streaks, semi-transparent flat red veil below the boundary, steadily fading into fully transparent lower half. Vary lengths of hair-thin streaks and gaps, taper gracefully at both left and right ends. Only scarlet/crimson red hues, almost no black, no white. Dry, airy magical slash afterimage seen in a dark fantasy action game, a blade sweeping red light through air. Dense thin red combed streaks at the upper edge, porous transparent wisps trailing downwards, tiny detached red flecks along upper edge. FLAT stylized special-effect texture with no surface lighting or reflections. NO glossy liquid, NO thick ribbons, NO flesh, NO veins, NO tendrils, NO cloth, NO skin, NO chunks, NO 3D shading, NO wet highlights, NO lens flare, NO white glints, NO background, NO ground, NO sword, NO scene, NO text, NO checkerboard. Preserve genuine semi-transparency. Overall silhouette a long horizontal band with a crisp thin top and softly eroded transparent lower margin, mapped onto a 3D fan later.

## 1.1.0 的 SA 资源

血光纹理由内置 imagegen 生成，使用真实透明通道，保存为
`src/main/resources/assets/eldenblade/textures/entity/blood_slash.png`。
它映射到代码生成的 32 段弧形网格上，每段一个四边形；不增加武器本体的面数。

音效来自用户提供的 2026-10-20 录制片段的第一音轨，裁取 1.25–1.50 秒和 1.50–2.60 秒。
处理包括 80 Hz 高通、14 kHz 低通、轻度频域降噪、统一 1.6 倍增益、单声道转换和边界淡化。
输出为 44.1 kHz 单声道 Ogg Vorbis。录制环境声可能有少量残留，未声称是原作无损独立音源。
音效属于原作录制内容，不主张其原创权利；完整参考视频不打包。

生成模式：内置 imagegen；最终提示词如下：

> Use case: stylized-concept. Production game VFX texture, a single horizontal unwrapped blood slash ribbon, very wide 4:1 composition, real transparent background. For UV mapping onto a curved sword slash mesh in Minecraft. Left-to-right flowing thin blood filaments, continuous crimson and scarlet central streak with dark burgundy veins, fine sharp bright red highlights, irregular torn wispy edges and small trailing droplets close to the main ribbon. Main ribbon occupies middle 65 percent of image height, tapers to sharp points at both horizontal ends. Mostly semi-transparent gaps toward edges. NOT a sword or weapon, NO scene, NO text, NO symbols, NO frame, NO checkerboard, NO white background, NO white glow, no fire or orange. This is a flat straight texture strip, not a crescent; curvature applied later by mesh. Polished dark fantasy magical slashing blood VFX. Clear connected core with ragged blood strands.

## 武器本体

尸山血海模型为本项目制作的原创游戏适配几何，外观方向参考用户提供的《艾尔登法环》截图和认可的概念图。没有将参考模组“柯罗诺斯永境之主”的模型或贴图复制到成品；该模组仅用于本地结构和性能比较。

血纹、漆面和尸骨祭坛等表面素材由项目制作过程中生成，再经专用 UV、Blender 烘焙和原生模型导出处理。当前游戏使用单张 2048×2048 图集，物品栏图标从实际模型离线渲染。

Shift 描述是用户提供的原作武器文本，按要求保留原文；它不是本项目原创故事。本仓库不声称拥有原作名称、文本或相关作品的权利。

未公开本地参考截图、第三方模组 JAR、反编译研究文件或包含私人路径的 Blender 历史工程。成品运行所需 OBJ／PNG 位于 src/main/resources/assets/eldenblade/model/。
