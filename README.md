# metersphere-idea-plugin

## 支持导出 HTTP 接口到 MeterSphere.

### Features：

- Javadoc-based analysis,no additional annotations.
- Automatically identify classes, generate api documents to MeterSphere.
- Support list, set, collection and other data structures, and support nested generic parsing.
- Support the parsing of common annotations such as @ResponseBody, @RequestMapping, and generate api documents based on
  json5.

### Usage：

- Locate the cursor to the package/project folder or class, right-click Export MeterSphere.
- Support annotations：
   <p>
   org.springframework.web.bind.annotation.Controller 
   org.springframework.web.bind.annotation.RestController 
   org.springframework.web.bind.annotation.RequestMapping 
   org.springframework.web.bind.annotation.GetMapping 
   org.springframework.web.bind.annotation.PutMapping 
   org.springframework.web.bind.annotation.DeleteMapping 
   org.springframework.web.bind.annotation.PatchMapping 
   org.springframework.web.bind.annotation.PathVariable 
   org.springframework.web.bind.annotation.RequestBody 
   org.springframework.web.bind.annotation.RequestParam 
   org.springframework.web.bind.annotation.ResponseBody
   </p>

### 特性：

- 基于javadoc解析，无代码入侵
- 自动识别类，生成接口定义到 MeterSphere
- 支持List、Set、Collection等数据结构，支持嵌套泛型解析
- 支持@ResponseBody等常用注解的解析

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
apiServer
</td>
<td>
MeterSphere API 服务器地址
</td>
</tr>
<tr>
<td>
accesskey
</td>
<td>
accesskey
</td>
</tr>
<tr>
<td>
secretkey
</td>
<td>
secretkey
</td>
</tr>
<tr>
<td>
protocol
</td>
<td>
协议
</td>
</tr>
<tr>
<td>
workspace
</td>
<td>
工作空间
</td>
</tr>
<tr>
<td>
project
</td>
<td>
项目
</td>
</tr>
<tr>
<td>
module
</td>
<td>
接口将要导入的项目对应的模块
</td>
</tr>
<tr>
<td>
mode
</td>
<td>
覆盖/不覆盖已导入同名接口
</td>
</tr>
<tr>
<td>
object-deepth
</td>
<td>
针对接口中含有复杂嵌套对象的解析深度
</td>
</tr>
<tr>
<td>
version
</td>
<td>
新增接口建立的版本号
</td>
</tr>
<tr>
<td>
update-version
</td>
<td>
覆盖接口所生成的版本号
</td>
</tr>
<tr>
<td>
context-path
</td>
<td>
接口路径前缀
</td>
</tr>
<tr>
<td>
export-name
</td>
<td>
导出的模块名称
</td>
</tr>
<tr>
<td>
javadoc
</td>
<td>
支持读取接口/类名前注释的 javadoc 作为接口名称
</td>
</tr>
<tr>
<td>
coverModule
</td>
<td>
选择覆盖的同时是否一并更新旧接口的模块
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
