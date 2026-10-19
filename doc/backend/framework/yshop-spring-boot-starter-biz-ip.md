# yshop-spring-boot-starter-biz-ip

> 所属：yshop-framework；文档依据本仓库源码整理。

## 1. 模块定位

本模块提供两类纯工具能力：

1. **IP 归属地查询**：读取内置 `ip2region.xdb`，将 IPv4/IPv6 查询为行政区划编号。
2. **行政区划树查询**：读取内置 `area.csv`，构建“全球 → 国家 → 省份 → 城市 → 区县”的树，并提供按编号、路径查询与格式化输出。

模块**不包含 Spring 自动配置类**，也没有 `META-INF/spring` 目录；它不向容器注册 Bean，只是被其它模块当作普通工具库依赖和静态调用。

### 在 yshop-server 中的依赖与启用状态

- `yshop-server/pom.xml` **没有直接声明**本模块。
- 通过 `yshop-module-system-biz`、`yshop-module-member-biz` 传递引入；`yshop-spring-boot-starter-excel` 也依赖它（用于地区 Excel 转换）。
- 没有自动配置，因此不存在“开关”；类首次被使用时通过静态单例加载内置数据文件。
- 运行时确有使用：
  - `system` 模块 `AreaController` 暴露 `/system/area/tree`、`/system/area/get-by-ip`；
  - `system` 模块 `AppAreaController` 提供 App 端地区树；
  - `member` 模块 `AppUserAddressController` 使用地区树；
  - `excel` 模块 `AreaConvert` 使用 `AreaUtils.parseArea(...)` 做 Excel 导入导出转换。

## 2. 目录与包结构

```text
yshop-spring-boot-starter-biz-ip/
├── pom.xml
└── src/main/
    ├── java/co/yixiang/yshop/framework/ip/core/
    │   ├── Area.java
    │   ├── enums/AreaTypeEnum.java
    │   └── utils/
    │       ├── AreaUtils.java
    │       └── IPUtils.java
    └── resources/
        ├── area.csv          # 行政区划数据（UTF-8，带表头）
        └── ip2region.xdb     # ip2region 离线 IP 库
```

测试位于 `src/test/java/.../utils/AreaUtilsTest.java` 与 `IPUtilsTest.java`。

## 3. 核心类与职责

| 类/接口 | 所在包 | 职责 |
|---|---|---|
| `Area` | `...framework.ip.core` | 区域节点模型：`id`、`name`、`type`、`parent`、`children`；常量 `ID_GLOBAL=0`、`ID_CHINA=1` |
| `AreaTypeEnum` | `...framework.ip.core.enums` | 区域类型：国家(1)、省份(2)、城市(3)、地区(4)，实现 `IntArrayValuable` |
| `AreaUtils` | `...framework.ip.core.utils` | 静态加载 `area.csv` 构建区域树；提供查询、路径解析、格式化、按类型筛选、向上查找父级等能力 |
| `IPUtils` | `...framework.ip.core.utils` | 以 `Searcher.newWithBuffer(...)` 将 `ip2region.xdb` 全量载入内存，提供 IP → 地区编号/地区对象 |

## 4. 自动配置与生效方式

- **没有自动配置**：模块中不存在 `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，也没有 `@AutoConfiguration` / `@ConfigurationProperties`。
- `AreaUtils` 与 `IPUtils` 都通过“私有构造 + 静态 `INSTANCE`”在类加载时完成初始化：
  - `AreaUtils` 使用 Hutool `CsvUtil` 读取 `classpath:area.csv`，先创建全部节点，再二次遍历建立父子关系，并记录启动耗时。
  - `IPUtils` 使用 Hutool `ResourceUtil.readBytes("ip2region.xdb")` 读取文件，再调用 `Searcher.newWithBuffer(bytes)`。
- 因此无需任何配置即可使用；数据文件随 jar 一起打包。

## 5. 关键配置项

本模块**没有定义任何配置项**（无 `@ConfigurationProperties`、无自动配置条件属性）。数据源固定为 classpath 下的：

| 资源 | 说明 |
|---|---|
| `area.csv` | 行政区划数据，列顺序为：编号、名称、类型、父编号；首行为表头 |
| `ip2region.xdb` | ip2region 离线库，全量加载到内存，查询不依赖外部服务 |

## 6. 使用方式 / 典型用法

### 6.1 IP 查询归属地

```java
Integer areaId = IPUtils.getAreaId("120.202.4.50"); // IP → 区域编号
Area area = IPUtils.getArea("120.202.4.50");        // IP → Area 对象

// 也支持 ip2region Searcher#checkIP 返回的时间戳形式
Area area2 = IPUtils.getArea(ipLong);
```

对应管理端接口（`AreaController`）：

```java
@GetMapping("/system/area/get-by-ip")
public CommonResult<String> getAreaByIp(@RequestParam("ip") String ip) {
    Area area = IPUtils.getArea(ip);
    return success(area == null ? "未知" : AreaUtils.format(area.getId()));
}
```

### 6.2 地区树与格式化

```java
Area china = AreaUtils.getArea(Area.ID_CHINA);       // 获取中国节点
List<Area> provinces = china.getChildren();           // 子节点

Area area = AreaUtils.getArea(110105);
String text = AreaUtils.format(area.getId());         // "北京 北京市 朝阳区"
String custom = AreaUtils.format(area.getId(), "/");  // 自定义分隔符

Area parsed = AreaUtils.parseArea("河南省/郑州市/金水区");
List<String> paths = AreaUtils.getAreaNodePathList(List.of(china));
List<Area> cities = AreaUtils.getByType(AreaTypeEnum.CITY, a -> a);
Integer provinceId = AreaUtils.getParentIdByType(110105, AreaTypeEnum.PROVINCE);
```

## 7. 依赖关系

模块自身依赖：

- `yshop-common`（`IntArrayValuable`、集合与对象工具）
- `org.lionsoul:ip2region`（版本由 `yshop-dependencies` 统一管理，当前为 `2.7.0`）
- Lombok
- `slf4j-api`（`provided`，仅工具类日志使用）
- `yshop-spring-boot-starter-test`（test scope）

上游使用者：

- `yshop-module-system-biz`：地区管理接口、操作日志地区解析；
- `yshop-module-member-biz`：用户地址地区树；
- `yshop-spring-boot-starter-excel`：`AreaConvert`；
- `yshop-server` 通过上述业务模块传递获得。

## 8. 注意事项

1. **首次使用时加载**：静态初始化发生在类第一次被访问时，而不是应用启动时；首次调用会有一次文件读取与建树耗时。
2. `IPUtils.SEARCHER` 在加载失败时只记录错误日志，字段可能为 `null`；随后调用 `getAreaId(...)` 可能触发空指针。生产环境应确认 `ip2region.xdb` 已正确打包。
3. `ip2region.xdb` 采用 **全量内存缓存**（`newWithBuffer`），可避免磁盘 IO，但会占用额外堆内存。
4. `AreaUtils.format(...)` 在中国区域下会跳过 `ID_GLOBAL` 与 `ID_CHINA` 父节点，因此输出不包含“中国”；其它国家则正常显示。
5. `AreaUtils.parseArea(...)` 第一级名称会在**全量区域集合**中查找，存在同名地区时可能命中非预期节点；导入数据时建议按完整路径校验。
6. `AreaUtils.getByType(...)` 遍历的是 `areas.values()`，其中包含 `id=0` 的“全球”虚拟根节点（其 `type=0` 不等于任何 `AreaTypeEnum`），不会污染结果。
7. `area.csv` 必须以 UTF-8 读取（代码使用 `ResourceUtil.getUtf8Reader`），修改数据文件时不要改变列顺序与表头。
8. 本模块无自动配置，因此无法通过配置关闭；若某部署不需要 IP 能力，只能不引入依赖或避免调用。
