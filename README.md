# metersphere-idea-plugin

## 支持导出 HTTP 接口到 MeterSphere.

### 特性：

- 基于 Javadoc 的分析，无需额外的注解。
- 自动识别类，生成API文档到 MeterSphere。
- 支持列表、集合、和其他数据结构，并支持嵌套泛型解析。
- 支持解析常见的注解如 @ResponseBody、@RequestMapping 等。

### 使用方法：

- 将光标定位到包/项目文件夹或类上，右键点击“Sync to MeterSphere”。
- 支持注解：
   <p>
   org.springframework.web.bind.annotation.Controller 
  <p>
   org.springframework.web.bind.annotation.RestController 
  <p>
   org.springframework.web.bind.annotation.RequestMapping 
  <p>
   org.springframework.web.bind.annotation.GetMapping 
  <p>
   org.springframework.web.bind.annotation.PutMapping 
  <p>
   org.springframework.web.bind.annotation.DeleteMapping 
  <p>
   org.springframework.web.bind.annotation.PatchMapping 
  <p>
   org.springframework.web.bind.annotation.PathVariable 
  <p>
   org.springframework.web.bind.annotation.RequestBody 
  <p>
   org.springframework.web.bind.annotation.RequestParam 
  <p>
   org.springframework.web.bind.annotation.ResponseBody
   </p>


### 安装方式：
- 直接在 idea -> Settings -> plugins -> Marketplace 搜索 MeterSphere 在线安装
- [下载离线包](https://plugins.jetbrains.com/plugin/18097-metersphere/versions)  
  idea -> Settings -> plugins -> 选择下载的插件 -> Install Plugin from Disk
### 用法：

- 将光标定位到项目/包级目录或者打开类，鼠标右键单击"Export MeterSphere".
- 自定义配置项： Preferences —> Other Settings —> MeterSphere

<table tr=1>
<thead>
<td>
配置项
</td>
<td>
含义
</td>
</thead>
<tr>
<td>
Server
</td>
<td>
MeterSphere Server 地址
</td>
</tr>
<tr>
<td>
AccessKey
</td>
<td>
AccessKey
</td>
</tr>
<tr>
<td>
SecretKey
</td>
<td>
SecretKey
</td>
</tr>
<tr>
<td>
Protocol
</td>
<td>
协议
</td>
</tr>
<tr>
<td>
Organization
</td>
<td>
组织
</td>
</tr>
<tr>
<td>
Project
</td>
<td>
项目
</td>
</tr>
<tr>
<td>
Module
</td>
<td>
接口将要导入的项目对应的模块
</td>
</tr>
<tr>
<td>
Mode
</td>
<td>
覆盖：
1.系统已存在的同一接口（请求类型+路径一致），请求参数内容不一致则覆盖系统原接口
2.系统已存在的同一接口（请求类型+路径一致），请求参数内容一致则不做变更
3.系统不存在的接口，则新增<br>
不覆盖：
1.系统已存在的同一接口（请求类型+路径一致），则不做变更
2.系统不存在的接口，则新增
</td>
</tr>
</table>

## Compatible IDE versions: 2020.3+

## 问题反馈

如果您在使用过程中遇到什么问题，或有进一步的需求需要反馈，请提交 GitHub Issue 到 [MeterSphere 项目的主仓库](https://github.com/metersphere/metersphere/issues)


## License & Copyright

Copyright (c) 2014-2024 飞致云 FIT2CLOUD, All rights reserved.

Licensed under The GNU General Public License version 3 (GPLv3)  (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

https://www.gnu.org/licenses/gpl-3.0.html

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
