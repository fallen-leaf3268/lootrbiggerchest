# Lootr Big Chest

将 Lootr 模组所有容器的容量从默认 27 格（3×9）扩展至 81 格（9×9），每个容器独立随机尺寸。

## 兼容的容器

| 容器 | 原版尺寸 | 扩展尺寸（默认） |
|------|---------|----------------|
| Lootr 箱子 | 3×9 | 9×9 |
| Lootr 木桶 | 3×9 | 9×9 |
| Lootr 潜影盒 | 3×9 | 9×9 |
| Lootr 运输矿车 | 3×9 | 9×9 |

## 依赖

- **Minecraft** 1.20.1
- **Forge** 47.3+
- **Lootr** 0.7+

## 配置

配置文件位于 `config/lootrbigchest-common.toml`：

```toml
[general]
enabled = true

[chest]
enabled = true
# 最小行数
min_rows = 3
# 最大行数
max_rows = 9
# 最小列数
min_cols = 9
# 最大列数
max_cols = 9

[barrel]
enabled = true
min_rows = 3
max_rows = 9
min_cols = 9
max_cols = 9

[shulker]
enabled = true
min_rows = 3
max_rows = 9
min_cols = 9
max_cols = 9

[minecart]
enabled = true
min_rows = 3
max_rows = 9
min_cols = 9
max_cols = 9
```

每种容器类型可单独启用/禁用，尺寸在最小和最大值之间随机选取，每个容器在首次打开时确定尺寸并持久化保存。

## 注意事项

- 已存在的旧容器将保持 3×9 尺寸，新加载的区块中生成的容器使用新尺寸
- 可通过修改配置中的 `min_rows`/`max_rows` 等参数调整容量范围
- 容器 GUI 会自动适配不同尺寸
