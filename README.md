# Sculk Base Lib

`sculkbaselib` 是 EnderEchoing 与 SculkBorne 共用的幽匿内核前置库。

它只包含两边都需要的内容与基础设施：

- 影匿效果、累计时长附件、永久黑暗、随机负面脉冲
- 回响晶簇方块/物品与生长
- 校频幽匿尖啸体、内含物品机制 API、触发效果客户端绘制 API
- 幽匿方块标签、幽匿绽放
- 影匿雾/后处理/音效、共享 mixin、共享配置

## 玩法入口

手持 4 个紫水晶碎片右键一个 `CAN_SUMMON` 为真、且尚未校频的幽匿尖啸体，
会把它替换成 `sculkbaselib:calibrated_sculk_shrieker`。该交互现在在库里，
因此只装 EnderEchoing + sculkbaselib 时也能完成整条幽匿核心循环。

手持回响碎片右键幽匿催发体，会在其上方种下一阶段回响晶簇；这条种植交互也由本库统一处理。

## 依赖关系

```text
EnderEchoing ─┐
              ├─> sculkbaselib
SculkBorne ───┘

freshsculk ──> SculkBorne
```

EnderEchoing 和 SculkBorne 都必须在运行时安装本库；SculkBorne 对
EnderEchoing 仍保持可选兼容，反过来也一样。

## 内容 ID

共享内容的注册命名空间是 `sculkbaselib`：

- `sculkbaselib:sculk_veil`
- `sculkbaselib:echo_druse`
- `sculkbaselib:echo_druse_block`
- `sculkbaselib:calibrated_sculk_shrieker`
- `sculkbaselib:rhyme_shard`

从旧版 EnderEchoing / SculkBorne 的兼容副本迁移过来时，这些 ID 发生变化；
旧存档中的对应方块、物品和影匿累计数据需要重新生成测试世界，或单独做数据迁移。

## 配置

`config/sculkbaselib-common.toml`：

- `sculk_veil_darkness_duration`
- `sculk_veil_glowing_duration`
- `sculk_veil_player_darkness_ticks`（默认 3600，设计稿的 3 分钟）
- `sculk_veil_mob_darkness_ticks`（默认 2400）
- `echo_druse_max_growth_value`
- `sculk_shrieker_can_summon_chance`

## 对外 API

- `api.SculkTags`
- `api.SculkVeil`
- `api.SculkVeilImmune`
- `api.SculkVeilDebuffs`
- `api.SculkVibrations`
- `api.SculkDruseEvent`
- `api.SculkCatalystHooks`
- `api.block.CalibratedShriekerMechanism(s)`
- `api.event.SculkVeilEvent` / `SculkBloomEvent`
- `client.api.CalibratedShriekerClientEffectRegistry`
- `client.api.GeoBoneOverlayLayer`（GeckoLib 骨骼贴图覆盖机制；骨骼名、UV 与贴图必须按目标模型制作）

## 客户端效果复用

GeckoLib 模型如果有一根需要独立贴图的骨骼（这是通用机制，不是通用贴图）：

```java
addRenderLayer(new GeoBoneOverlayLayer<>(this, List.of("your_bone"), yourModelTexture));
```

Creesper 的 `creeper_whisper.png` 是 48×1536 的 32 帧纵向动态图集，按 Creesper 的骨骼 UV 制作，
不能直接换到别的实体上，因此 Creesper 层使用默认的 `animatedTexture = true`，
由 `AnimatableTexture` 推进图集帧；非动态贴图才需要显式传 `false`。
每个实体需要不同贴图时，第三个参数可以传 `Function<T, ResourceLocation>`。

## 构建

```powershell
.\gradlew.bat build
```

下游工程默认读取同级仓库的 `../sculkbaselib/build/libs/sculkbaselib-1.0.0.jar`，
也可以用 `-PsculkbaselibJar=<path>` 指向别处的副本。CI 需要先构建本仓库。
