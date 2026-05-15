# 🚀 AI 口译工作台 - 快速开发检验清单

## 核心功能实现状态追踪

### 【第一阶段】后端数据处理基础 ⏳ IN_PROGRESS

#### 1️⃣ 配置百度服务 API
- [ ] **百度 ASR 语音识别配置**
  - 获取 API Key & Secret Key
  - 在 `application.properties` 中配置
  - 编写 `BaiduAsrService.java` 集成类
  - 测试：上传 MP3 → 输出文本
  
- [ ] **百度千帆大模型配置** (ERNIE-X1-Turbo-32K)
  - 获取 API Key & Secret Key & Access Token
  - 在 `BaiduLLMService.java` 中实现翻译方法
  - 在 `BaiduLLMService.java` 中实现术语提取方法
  - 测试：输入原文 → 输出翻译 + 术语列表

#### 2️⃣ 后端接口开发
- [ ] **音频上传接口** 
  ```
  POST /api/audio/upload
  [完成度: ____%]
  ```
  - 接收音频文件 (multipart/form-data)
  - 保存到服务器目录
  - 返回 session_id
  
- [ ] **自动ASR转写**
  ```
  自动触发，无需前端调用
  [完成度: ____%]
  ```
  - 调用百度ASR获取原文
  - 存储到缓存/数据库
  
- [ ] **自动翻译&术语提取**
  ```
  自动触发，无需前端调用
  [完成度: ____%]
  ```
  - 调用百度LLM进行翻译
  - 提取术语和时间轴
  - 存储标准答案和术语列表
  
- [ ] **元数据查询接口**
  ```
  GET /api/session/{sessionId}/metadata
  [完成度: ____%]
  ```
  - 返回锁定的原文、答案、术语

#### 3️⃣ 核心功能检验
- [ ] 🧪 功能测试
  ```
  测试场景：上传"https://xxx/demo.mp3" (1 分钟英文音频)
  
  ✓ Step 1: 文件上传成功，获得 session_id = "sess_12345"
  ✓ Step 2: 等待 3 秒，ASR 完成 (原文正确识别)
  ✓ Step 3: LLM 翻译完成 (标准答案准确)
  ✓ Step 4: 术语提取 (5-8 个关键词，时间轴准确)
  ✓ Step 5: 查询 metadata，数据完整无误
  
  预期耗时: < 5 秒
  实际耗时: ___ 秒
  ```

---

### 【第二阶段】前端UI/交互实现 ⏳ NOT_STARTED

#### 4️⃣ 前端页面改进
- [ ] **优化上传卡片** 
  ```
  文件: ai-interpreter-frontend/src/App.jsx
  [完成度: ____%]
  ```
  - 美化文件上传按钮
  - 添加进度条动画
  - 显示"AI 已在后台成功听写并预翻译..."提示

- [ ] **构建双栏沙盒布局**
  ```
  文件: src/components/SandboxView.jsx
  [完成度: ____%]
  ```
  - 左栏：音频播放器 + 录音按钮（干净空白）
  - 右栏：术语提示窗口（初始空白，等待卡片）

#### 5️⃣ 音频播放和录音功能
- [ ] **Web Audio API 播放器**
  ```
  文件: src/components/AudioPlayer.jsx
  [完成度: ____%]
  ```
  - 加载音频文件
  - 播放/暂停/进度控制
  - 实时获取当前播放时间
  - 通过 WebSocket 推送时间信息

- [ ] **Web Audio API 录音器**
  ```
  文件: src/components/AudioRecorder.jsx
  [完成度: ____%]
  ```
  - 调用麦克风权限
  - 录制用户口译
  - 保存为 WAV/MP3
  - 上传到后端

#### 6️⃣ 动态术语卡片系统
- [ ] **术语卡片组件**
  ```
  文件: src/components/TermCard.jsx
  [完成度: ____%]
  ```
  - 接收术语数据（英文、中文、时间轴）
  - 黄色高亮样式
  - 动画弹出效果

- [ ] **术语卡片容器**
  ```
  文件: src/components/HintsWindow.jsx
  [完成度: ____%]
  ```
  - 监听音频播放时间
  - 根据时间轴动态推送卡片
  - 管理卡片生命周期（出现→滑出）
  - 通过 WebSocket 接收后端推送

#### 7️⃣ 核心功能检验
- [ ] 🧪 UI/交互测试
  ```
  测试场景：上传音频后进入沙盒
  
  ✓ 页面平滑切入双栏布局
  ✓ 左栏：只看到播放器和录音按钮，没有其他文字
  ✓ 右栏：初始空白
  ✓ 点击播放，音频开始播放
  ✓ 术语卡片随时间轴动态弹出
    - 00:02 → Digital Economy (数字经济)
    - 00:05 → Sustainable Development (可持续发展)
    - ...
  ✓ 用户可以同时：听音频 + 看卡片 + 对着麦克风讲话
  ✓ 点击"开始录音"，红色灯亮起
  ✓ 点击"结束录音"，灯灭灭，录音保存
  
  延迟要求: 卡片弹出延迟 < 100ms
  实际延迟: ___ ms
  ```

---

### 【第三阶段】LLM 智能诊断系统 ⏳ NOT_STARTED

#### 8️⃣ 学生口译评阅
- [ ] **学生ASR转写接口**
  ```
  POST /api/session/{sessionId}/student-asr
  [完成度: ____%]
  ```
  - 接收学生录音文件
  - 调用百度ASR转写
  - 返回学生的口译文本

- [ ] **三方文本对比逻辑**
  ```
  文件: ai-interpreter-backend/src/main/java/...GradingService.java
  [完成度: ____%]
  ```
  - 比对【源文】【标准答案】【学生翻译】
  - 计算相似度
  - 识别漏译、误译、不流畅部分
  - 标记错误位置和类型

- [ ] **LLM 诊断评分**
  ```
  文件: GradingService.java (调用 BaiduLLMService)
  [完成度: ____%]
  ```
  - 发送三方文本到大模型
  - 获取综合得分 (0-100)
  - 获取诊断分析和建议
  - 提取改进要点

#### 9️⃣ 诊断报告生成
- [ ] **报告模板**
  ```
  文件: src/templates/DiagnosisReport.html
  [完成度: ____%]
  ```
  - 综合得分展示
  - 三栏对照布局
  - 错误标注和高亮
  - 改进建议列表

#### 🔟 核心功能检验
- [ ] 🧪 诊断评阅测试
  ```
  测试场景：学生完成口译，提交评阅
  
  ✓ 学生点击"结束口译 提交 AI 阅卷"
  ✓ 页面加载中... (2-3 秒)
  ✓ 诊断报告展开显示
  ✓ 综合得分: 88/100
  ✓ 三栏对照清晰可见
  ✓ 错误部分清晰标注 (例如："漏译 sustainable")
  ✓ 改进建议具体有用
  
  报告生成耗时: < 5 秒
  实际耗时: ___ 秒
  ```

---

### 【第四阶段】前端诊断报告展示 ⏳ NOT_STARTED

#### 1️⃣1️⃣ 报告展示组件
- [ ] **诊断报告组件**
  ```
  文件: src/components/DiagnosisReport.jsx
  [完成度: ____%]
  ```
  - 接收报告数据
  - 下拉展开动画
  - 分页展示长内容

- [ ] **三栏对照布局**
  ```
  [完成度: ____%]
  ```
  - 源文本左栏
  - 标准答案中栏
  - 学生翻译右栏
  - 同步滚动对齐

- [ ] **错误标注系统**
  ```
  [完成度: ____%]
  ```
  - 高亮漏译部分 (红色)
  - 高亮误译部分 (橙色)
  - 高亮不流畅部分 (黄色)
  - 鼠标悬停显示详细说明

---

### 【第五阶段】WebSocket 实时通信 ⏳ NOT_STARTED

#### 1️⃣2️⃣ 实时同步
- [ ] **WebSocket 配置** (后端已有框架)
  ```
  文件: ai-interpreter-backend/.../WebSocketConfig.java
  [完成度: ____%]
  ```
  - 验证端点配置: /ws
  - 配置消息处理器

- [ ] **音频播放进度同步**
  ```
  [完成度: ____%]
  ```
  - 前端每 100ms 推送一次当前播放时间
  - 后端接收并准备术语推送

- [ ] **术语卡片推送**
  ```
  [完成度: ____%]
  ```
  - 后端根据播放时间推送术语
  - 前端接收并动态渲染卡片
  - 延迟控制 < 100ms

---

### 【第六阶段】完整端到端测试 ⏳ NOT_STARTED

#### 1️⃣3️⃣ 全流程测试
- [ ] **完整场景测试**
  ```
  🧪 测试场景：一名学生的完整口译训练流程
  
  1. 上传音频 (1 分钟英文演讲)
     ✓ 成功上传
     ✓ 文件保存
     ✓ 处理进度正常
  
  2. 进入沙盒
     ✓ 页面平滑切换
     ✓ 布局正确
     ✓ 无错误提示
  
  3. 播放音频 + 看术语 + 录音
     ✓ 播放流畅，无卡顿
     ✓ 术语卡片准时弹出
     ✓ 录音正常工作
  
  4. 提交评阅
     ✓ 点击提交无误
     ✓ 后台处理成功
     ✓ 报告生成
  
  5. 查看诊断报告
     ✓ 报告展开动画流畅
     ✓ 内容清晰准确
     ✓ 无排版错误
  
  总耗时: ___ 秒
  用户体验评分: ___/10
  ```

- [ ] **性能基准测试**
  ```
  📊 性能指标
  
  - 页面初始加载: ___ ms (目标: < 3s)
  - 音频上传: ___ ms (1分钟音频，目标: < 2s)
  - 后端处理: ___ ms (ASR+翻译+术语，目标: < 5s)
  - 术语卡片延迟: ___ ms (目标: < 100ms)
  - 诊断报告生成: ___ ms (目标: < 5s)
  - 页面切换动画: ___ ms (目标: < 1s)
  ```

- [ ] **浏览器兼容性**
  ```
  ✓ Chrome 120+
  ✓ Firefox 121+
  ✓ Safari 17+
  ✓ Edge 120+
  ```

---

## 📊 整体进度仪表板

```
【第一阶段】后端数据处理  ▓▓▓░░░░░░░ 30%
【第二阶段】前端UI/交互   ░░░░░░░░░░  0%
【第三阶段】LLM诊断系统   ░░░░░░░░░░  0%
【第四阶段】报告展示      ░░░░░░░░░░  0%
【第五阶段】WebSocket     ░░░░░░░░░░  0%
【第六阶段】端到端测试    ░░░░░░░░░░  0%

总体进度: ▓░░░░░░░░░  5%
```

---

## 🎯 下一周 (W1) 目标

### 优先级最高任务 (必须完成)
1. ✅ 配置百度 ASR API
2. ✅ 配置百度千帆 LLM API
3. ⏳ 开发音频上传接口
4. ⏳ 实现自动ASR转写
5. ⏳ 实现自动翻译和术语提取

### 预期产出
- [ ] `POST /api/audio/upload` 接口可用
- [ ] `GET /api/session/{id}/metadata` 接口可用
- [ ] 完整的后端数据处理流程（3-5 秒内）

### 完成标准
- 上传音频 → 3 秒内完成全部处理 ✓
- 查询 API 返回正确的原文、翻译、术语 ✓
- 无错误日志，正常完成 ✓

---

## 📞 故障排查速查表

| 问题 | 症状 | 解决方案 |
|------|------|--------|
| ASR 失败 | 音频无法转写 | 检查API Key、音频格式、网络 |
| LLM 超时 | 翻译 > 10s 未返回 | 检查模型配置、请求格式 |
| 卡片不显示 | 沙盒没有术语卡 | 检查WebSocket连接、时间轴标记 |
| 录音无声 | 用户点击录音但无音频 | 检查麦克风权限、浏览器配置 |
| 报告崩溃 | 诊断报告显示异常 | 检查数据格式、错误标注逻辑 |

---

## 💾 重要文件清单

### 后端源码
```
ai-interpreter-backend/src/main/java/com/interpreter/aibackend/
├── AiBackendApplication.java
├── AiInterpreterHandler.java
├── WebSocketConfig.java
├── service/
│   ├── BaiduAsrService.java          ⏳ TODO
│   ├── BaiduLLMService.java          ⏳ TODO
│   ├── GradingService.java           ⏳ TODO
│   └── AudioProcessingService.java   ⏳ TODO
├── controller/
│   ├── AudioController.java          ⏳ TODO
│   └── SessionController.java        ⏳ TODO
└── entity/
    ├── Session.java                 ⏳ TODO
    ├── TermHint.java                ⏳ TODO
    └── DiagnosisReport.java         ⏳ TODO
```

### 前端源码
```
ai-interpreter-frontend/src/
├── App.jsx
├── components/
│   ├── AudioPlayer.jsx              ⏳ TODO
│   ├── AudioRecorder.jsx            ⏳ TODO
│   ├── SandboxView.jsx              ⏳ TODO
│   ├── HintsWindow.jsx              ⏳ TODO
│   ├── TermCard.jsx                 ⏳ TODO
│   └── DiagnosisReport.jsx          ⏳ TODO
├── services/
│   ├── apiService.js                ⏳ TODO
│   ├── websocketService.js          ⏳ TODO
│   └── audioService.js              ⏳ TODO
└── styles/
    └── App.css                      ⏳ TODO
```

---

**上次更新**: 2026-05-15
**负责人**: [Your Name]
**状态**: 🟡 规划完成，即将启动第一阶段

