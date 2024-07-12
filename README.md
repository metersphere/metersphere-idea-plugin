# MeterSphere Idea Plugin

## 支持导出 HTTP 接口到 MeterSphere.

### 特性：

- 基于 Javadoc 的分析，无需额外的注解
- 自动识别类，生成接口定义到 MeterSphere
- 支持列表、集合和其他数据结构，包括嵌套泛型解析

### Spring Framework 注解：

- `@Controller`：标记类为控制器，处理 HTTP 请求。
- `@RestController`：结合 `@Controller` 和 `@ResponseBody`，用于 RESTful 服务，返回 JSON 或 XML 数据。
- `@RequestMapping`：将 HTTP 请求映射到 MVC 和 REST 控制器的处理方法，可设置 URL、HTTP 方法等属性。

#### HTTP 方法映射注解：
- `@GetMapping`：将 HTTP GET 请求映射到特定处理方法。
- `@PutMapping`：将 HTTP PUT 请求映射到特定处理方法。
- `@DeleteMapping`：将 HTTP DELETE 请求映射到特定处理方法。
- `@PatchMapping`：将 HTTP PATCH 请求映射到特定处理方法。
- `@PathVariable`：用于从 URL 中获取参数值，映射到方法的参数上。
- `@RequestBody`：将 HTTP 请求体的内容绑定到方法的参数上，适用于 POST 请求等。
- `@RequestParam`：用于从请求中获取参数值，映射到方法的参数上。
- `@ResponseBody`：将方法的返回值直接作为 HTTP 响应体返回给客户端。

### Swagger 2.0 相关注解：

- `@Api`：修饰整个类，描述类的作用，可设置 `tags`、`value`、`description` 等属性
- `@ApiOperation`：修饰方法，描述操作或 HTTP 方法，可设置 `value`、`notes`、`response` 等属性
- `@ApiParam`：修饰方法参数，描述参数，可设置 `name`、`value`、`required`、`defaultValue` 等属性
- `@ApiModel`：修饰 POJO 类，描述类的作用，可设置 `description` 等属性
- `@ApiModelProperty`：修饰类字段，描述字段的作用，可设置 `value`、`notes`、`example` 等属性

### OpenAPI 3.0 相关注解：

- `@io.swagger.v3.oas.annotations.Operation`：修饰方法，描述操作或 HTTP 方法，可设置 `summary`、`description`、`responses` 等属性
- `@io.swagger.v3.oas.annotations.Parameter`：修饰方法参数，描述参数，可设置 `name`、`description`、`required`、`example` 等属性
- `@io.swagger.v3.oas.annotations.media.Schema`：修饰 POJO 类，描述类的作用

### 标准验证注解：

- `javax.validation.constraints.NotNull`
- `javax.validation.constraints.NotEmpty`
- `javax.validation.constraints.NotBlank`
- `javax.validation.constraints.Size`
- `javax.validation.constraints.Min`
- `javax.validation.constraints.Max`
- `javax.validation.constraints.DecimalMin`
- `javax.validation.constraints.DecimalMax`
- `javax.validation.constraints.Positive`
- `javax.validation.constraints.PositiveOrZero`
- `javax.validation.constraints.Negative`
- `javax.validation.constraints.NegativeOrZero`
- `jakarta.validation.constraints.NotNull`
- `jakarta.validation.constraints.NotEmpty`
- `jakarta.validation.constraints.NotBlank`
- `jakarta.validation.constraints.Size`
- `jakarta.validation.constraints.Min`
- `jakarta.validation.constraints.Max`
- `jakarta.validation.constraints.DecimalMin`
- `jakarta.validation.constraints.DecimalMax`
- `jakarta.validation.constraints.Positive`
- `jakarta.validation.constraints.PositiveOrZero`
- `jakarta.validation.constraints.Negative`
- `jakarta.validation.constraints.NegativeOrZero`

### 解析优先级说明：

- API 相关：
  - 文档注释标记 `@module` > `@menu` > `@Api` > Doc 注释第一行
  - API Summary Swagger 2 `@ApiOperation` > OpenAPI 3 `@Operation` > 文档注释标记 `@description` > 文档注释第一行

- 参数相关逻辑同 API 相关优先级一致。

### 使用方法：

- 将光标定位到包/项目文件夹或类上，右键点击“Upload to MeterSphere”。

### 安装方式：

- 直接在 IntelliJ IDEA 的插件市场搜索 MeterSphere 进行在线安装。
- [下载离线包](https://plugins.jetbrains.com/plugin/18097-metersphere/versions)，在 IntelliJ IDEA 中选择离线安装：
  `Settings -> Plugins -> Install Plugin from Disk`

### 用法：

- 将光标定位到项目/包级目录或打开的类，鼠标右键单击“Export MeterSphere”。
- 自定义配置项：Preferences -> Other Settings -> MeterSphere

<table>
<thead>
<tr>
<td>配置项</td>
<td>含义</td>
</tr>
</thead>
<tr>
<td>Server</td>
<td>MeterSphere 服务器地址</td>
</tr>
<tr>
<td>AccessKey</td>
<td>AccessKey</td>
</tr>
<tr>
<td>SecretKey</td>
<td>SecretKey</td>
</tr>
<tr>
<td>Protocol</td>
<td>协议</td>
</tr>
<tr>
<td>Organization</td>
<td>组织</td>
</tr>
<tr>
<td>Project</td>
<td>项目</td>
</tr>
<tr>
<td>Module</td>
<td>接口将要导入的项目对应的模块</td>
</tr>
<tr>
<td>Mode</td>
<td>覆盖：
   1. 系统已存在相同接口（请求类型+路径一致），请求参数内容不一致则覆盖系统原接口
   2. 系统已存在相同接口（请求类型+路径一致），请求参数内容一致则不做变更
   3. 系统不存在该接口，则新增<br>
   不覆盖：
   1. 系统已存在相同接口（请求类型+路径一致），则不做变更
   2. 系统不存在该接口，则新增
</td>
</tr>
</table>

## 兼容的 IDE 版本：2022.1+

## 问题反馈

如果您在使用过程中遇到任何问题或有进一步的需求，请在 [MeterSphere 项目的主仓库](https://github.com/metersphere/metersphere/issues) 提交 GitHub Issue。

## License & Copyright

Copyright (c) 2014-2024 飞致云 FIT2CLOUD, All rights reserved.

Licensed under The GNU General Public License version 3 (GPLv3)  (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

https://www.gnu.org/licenses/gpl-3.0.html

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
