# MeterSphere IntelliJ IDEA Plugin

## MeterSphere API Debugger 是一款类似于 Postman 的 IntelliJ IDEA 插件，旨在帮助开发者高效调试 API。此外，该插件还支持一键将 API 同步到 [MeterSphere](https://github.com/metersphere/metersphere)。

---

## 特性

### API 同步功能

- 基于 Javadoc 的分析，无需额外的注解
- 支持列表、集合和其他数据结构，包括嵌套泛型解析
- 自动识别类并生成接口定义到 MeterSphere
- [详情查看 MeterSphere Plugin 的公共库，专注于提供通用的功能和工具](/README_LIB_zh)

### API 调试支持

- 提供完整的 API 调试功能，支持发送请求并进行实时调试
- 自动构建 API 请求 URL 和参数，简化接口调用流程
- 自动捕获并生成域名，支持在不同环境之间的动态切换，提升测试灵活性
- 允许用户自定义解析 API 请求参数，以适应不同的业务需求和数据格式
- 构建并展示 API 导航树，直观地展示接口层级结构，帮助用户快速定位和浏览接口
- 集成 SearchEverywhere 搜索功能，支持高效检索 API 接口，提升开发和测试效率
- 支持在发送请求时修改自动生成的参数，方便调试和验证接口的不同场景
- 根据接口规范动态生成模拟数据，快速模拟真实环境中的接口响应，支持高效调试
- 提供 API 管理功能，包括搜索、过滤和修改接口名称，以便于组织和维护 API 文档
- 支持从调试历史记录中一键定位到相应 API，并进行后续的二次调试，确保问题的有效解决
- 根据接口定义的规范自动生成请求数据，确保生成的数据与接口要求相符，支持多种数据格式和验证规则
- [详情查看 API 调试和测试相关的功能](/README_DEBUGGER_zh)

---


## 兼容的 IDE 版本：2022.2.5+

---

## 问题反馈

如果您在使用过程中遇到任何问题或有进一步的需求，请在 [MeterSphere 项目的主仓库](https://github.com/metersphere/metersphere/issues) 提交 GitHub Issue。

---

## License & Copyright

Copyright (c) 2014-2024 飞致云 FIT2CLOUD, All rights reserved.

Licensed under The GNU General Public License version 3 (GPLv3)  (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

https://www.gnu.org/licenses/gpl-3.0.html

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
