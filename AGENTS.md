<!-- OPENSPEC:START -->
# OpenSpec Instructions

These instructions are for AI assistants working in this project.

Always open `@/openspec/AGENTS.md` when the request:
- Mentions planning or proposals (words like proposal, spec, change, plan)
- Introduces new capabilities, breaking changes, architecture shifts, or big performance/security work
- Sounds ambiguous and you need the authoritative spec before coding

Use `@/openspec/AGENTS.md` to learn:
- How to create and apply change proposals
- Spec format and conventions
- Project structure and guidelines

Keep this managed block so 'openspec update' can refresh the instructions.

<!-- OPENSPEC:END -->
# Other
- 代码需要加上详尽清晰详细易懂的中文注释，方法内部也要有注释，不能使用行尾注释
- 避免"=="号的使用，应使用工具类
- 避免使用全类名，应该导包
- 回答问题用中文
- 分步注释用“1.”的形式，如果只有一步，就不用加序号
- 需要引入或调用第三方库时，必须通过MCP的context7工具先查阅官方文档：1. 使用`resolve-library-id`定位库及版本；2. 使用`get-library-docs`按主题获取API说明（必要时翻页）；3. 将检索到的关键信息引用到代码实现或说明中，如文档缺失需显式告知
# java开发规范
- 业务层依赖注入必须注入到Base类中集中管理
- 涉及数据库交互的操作需要放到manager层中，在manager中创建方法，利用MPJ拼接sql后调用返回给service
- Bo是传输层实体，Vo是返回层实体，Vo只能在Controller层作为返回视图,实体间的转换用mapstruct，转换器应放置在converter包下以converter结尾
- 我更喜欢Objects.isNull这种断言式判断方法，不喜欢==
