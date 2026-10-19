# yshop-spring-boot-starter-excel

> 所属：yshop-framework；文档依据本仓库源码整理。

## 1. 模块定位

`yshop-spring-boot-starter-excel` 是 Excel 导入导出与数据字典格式化 Starter，Maven 坐标 `co.yixiang.boot:yshop-spring-boot-starter-excel`。`pom.xml` 描述为“Excel 拓展”。

模块基于 Alibaba EasyExcel，提供：

- 统一的 `ExcelUtils.write(...)` / `ExcelUtils.read(...)`。
- 字典值 ↔ 字典标签转换（`DictConvert` + `@DictFormat`）。
- 金额（分）转元显示（`MoneyConvert`）。
- JSON 字段导出（`JsonConvert`）。
- 地区名称 ↔ 地区编号转换（`AreaConvert`，依赖可选模块 `biz-ip`）。
- Excel 列下拉选择（`@ExcelColumnSelect` + `SelectSheetWriteHandler`）。
- 字典查询工具 `DictFrameworkUtils`（带 1 分钟 Guava 本地缓存）。

注意：本模块的自动配置只有 `YshopDictAutoConfiguration`；各类 Converter **不会**被自动注册，需要在 `@ExcelProperty(converter = ...)` 中按字段显式指定。

## 2. 目录与包结构

根包：`co.yixiang.yshop.framework`

```text
co.yixiang.yshop.framework
├── dict
│   ├── config/YshopDictAutoConfiguration
│   └── core/DictFrameworkUtils
└── excel
    └── core
        ├── annotations/{DictFormat, ExcelColumnSelect}
        ├── convert/{AreaConvert, DictConvert, JsonConvert, MoneyConvert}
        ├── function/ExcelColumnSelectFunction
        ├── handler/SelectSheetWriteHandler
        └── util/ExcelUtils
```

自动配置注册文件：`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。

## 3. 核心类与职责

| 类/接口 | 所在包 | 职责 |
|---|---|---|
| `YshopDictAutoConfiguration` | `framework.dict.config` | 把容器中的 `DictDataApi` 注入 `DictFrameworkUtils`，并注册一个 `DictFrameworkUtils` Bean。 |
| `DictFrameworkUtils` | `framework.dict.core` | 字典查询工具：`getDictDataLabel(dictType, value)`、`getDictDataLabelList(dictType)`、`parseDictDataValue(dictType, label)`；三个 Guava `LoadingCache` 均 1 分钟过期、异步刷新；空结果用 `DICT_DATA_NULL` 占位。 |
| `DictFormat` | `framework.excel.core.annotations` | 字段级注解，`value()` 指定字典类型；与 `DictConvert` 配合使用。 |
| `DictConvert` | `framework.excel.core.convert` | 导出时把字典 value 转成 label；导入时把 label 解析回 value，并按字段 Java 类型转换；解析失败返回 `null` 并记录错误日志。 |
| `MoneyConvert` | `framework.excel.core.convert` | 把以“分”为单位的 `Integer` 金额除以 100，按 `HALF_UP` 保留 2 位，输出字符串。 |
| `JsonConvert` | `framework.excel.core.convert` | 导出时把对象序列化为 JSON 字符串。 |
| `AreaConvert` | `framework.excel.core.convert` | 导入时用 `AreaUtils.parseArea(label)` 解析地区名称并转成地区编号；依赖 `yshop-spring-boot-starter-biz-ip`。 |
| `ExcelColumnSelect` | `framework.excel.core.annotations` | 字段级注解，声明 Excel 列下拉数据源：`dictType` 与 `functionName` 二选一。 |
| `ExcelColumnSelectFunction` | `framework.excel.core.function` | 下拉数据源扩展接口：`getName()`、`getOptions()`；供非字典来源的下拉数据实现。 |
| `SelectSheetWriteHandler` | `framework.excel.core.handler` | EasyExcel `SheetWriteHandler`：扫描表头字段的 `@ExcelColumnSelect`，创建“字典sheet”，并通过名称/公式为第 1~2000 行设置下拉约束，阻止输入不在下拉项中的值。 |
| `ExcelUtils` | `framework.excel.core.util` | `write(response, filename, sheetName, head, data)`：输出 Excel，自动列宽、注册下拉处理器、用 `LongStringConverter` 防止 Long 精度丢失；`read(file, head)`：读取全部 sheet 数据。 |

## 4. 自动配置与生效方式

`AutoConfiguration.imports` 只注册了一个类：

```text
co.yixiang.yshop.framework.dict.config.YshopDictAutoConfiguration
```

生效要点：

- `YshopDictAutoConfiguration` 依赖 `co.yixiang.yshop.module.system.api.dict.DictDataApi` Bean；如果裁剪 system 模块，需要提供替代实现或排除该自动配置。
- `DictFrameworkUtils.init(DictDataApi)` 为静态字段赋值，之后 `DictConvert` 与 `SelectSheetWriteHandler` 才能查询字典。
- Converter 本身不是 Spring Bean，也没有全局注册；由 EasyExcel 在读取 `@ExcelProperty(converter = ...)` 时实例化。
- `SelectSheetWriteHandler` 在 `ExcelUtils.write` 中被显式注册，不需要额外配置。
- `AreaConvert` 依赖可选的 `biz-ip` 模块；只有类路径存在 `co.yixiang.yshop.framework.ip.*` 时才能使用。

## 5. 关键配置项

本模块**没有自己的 `@ConfigurationProperties` 配置项**。

| 配置项 | 默认值 | 说明 |
|---|---|---|
| 无 | — | 模块不读取任何专属配置项；字典类型通过 `@DictFormat("...")` 在字段上指定 |

运行期依赖的外部配置：

| 依赖项 | 说明 |
|---|---|
| `DictDataApi` Bean | 由 system 模块提供，`DictFrameworkUtils` 通过它查询字典 |
| `yshop-spring-boot-starter-biz-ip`（可选） | `AreaConvert` 使用 `AreaUtils`，未引入时不要使用该 Converter |

## 6. 使用方式 / 典型用法

导出 Excel：

```java
@GetMapping("/export")
@ApiAccessLog(operateType = OperateTypeEnum.EXPORT)
public void export(HttpServletResponse response) throws IOException {
    List<UserRespVO> list = userService.getUserList(reqVO);
    ExcelUtils.write(response, "用户数据.xls", "数据", UserRespVO.class, list);
}
```

导入 Excel：

```java
@PostMapping("/import")
public CommonResult<Boolean> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
    List<UserImportExcelVO> list = ExcelUtils.read(file, UserImportExcelVO.class);
    userService.importUsers(list);
    return CommonResult.success(true);
}
```

字典字段导入导出：

```java
public class UserImportExcelVO {
    @ExcelProperty(value = "用户性别", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.USER_SEX)
    private Integer sex;

    @ExcelProperty(value = "账号状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.COMMON_STATUS)
    private Integer status;
}
```

金额字段导出（金额单位为分）：

```java
@ExcelProperty(value = "订单金额", converter = MoneyConvert.class)
private Integer payPrice;
```

JSON 字段导出：

```java
@ExcelProperty(value = "短信参数", converter = JsonConvert.class)
private String templateContent;
```

下拉列（注解能力已提供，但当前仓库业务代码未使用）：

```java
public class UserImportExcelVO {
    @ExcelProperty(value = "用户性别")
    @ExcelColumnSelect(dictType = DictTypeConstants.USER_SEX)
    private Integer sex;
}
```

或使用自定义数据源：

```java
@Component
public class CitySelectFunction implements ExcelColumnSelectFunction {
    @Override
    public String getName() {
        return "cityOptions";
    }

    @Override
    public List<String> getOptions() {
        return cityService.listNames();
    }
}
```

## 7. 依赖关系

直接依赖：

- `co.yixiang.boot:yshop-common`。
- `org.springframework.boot:spring-boot-starter`。
- `co.yixiang.boot:yshop-module-system-api`（`DictDataApi`、字典 DTO）。
- `org.springframework:spring-web`（`provided`，`ExcelUtils` 使用 `MultipartFile`）。
- `jakarta.servlet-api`（`provided`，`ExcelUtils` 使用 `HttpServletResponse`）。
- `com.alibaba:easyexcel`（BOM 版本 3.3.3）。
- `com.google.guava:guava`（`DictFrameworkUtils` 缓存）。
- `co.yixiang.boot:yshop-spring-boot-starter-biz-ip`（`optional`，仅 `AreaConvert` 使用）。
- 测试：`yshop-spring-boot-starter-test`（`test`）。

## 8. 注意事项

1. Converter 不会自动注册，必须在字段的 `@ExcelProperty` 上显式指定 `converter`。
2. `DictConvert` 依赖 `DictFrameworkUtils` 已完成初始化，而后者依赖 system 模块的 `DictDataApi`；启动时缺少该 Bean 会导致自动配置失败。
3. 字典缓存有效期仅 1 分钟，字典数据修改后最多约 1 分钟生效；对实时性要求高的场景需注意。
4. `@ExcelColumnSelect` 与 `ExcelColumnSelectFunction` 在当前仓库业务代码中未发现使用，`ExcelColumnSelectFunction` 也没有实现类；该能力属于“已提供但未启用”。
5. `AreaConvert` 在当前仓库业务代码中未发现使用，且依赖可选的 `biz-ip`；未引入 `biz-ip` 时使用会触发类加载/找不到类错误。
6. `SelectSheetWriteHandler` 固定下拉范围为第 1~2000 行（`FIRST_ROW=1`、`LAST_ROW=2000`），超出范围不会自动扩展。
7. `ExcelUtils.write` 使用 `LongStringConverter`，将 Long 输出为字符串以避免前端/Excel 精度丢失。
8. `ExcelUtils.write` 先写内容、后设置响应头，目的是避免写入失败时响应头已被修改。
9. `DictConvert.convertToJavaData` 解析失败时返回 `null` 并打印错误日志，不会抛出异常；导入校验需在业务层补充非空检查。
