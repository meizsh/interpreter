# AI-Assisted Interpreter

一个面向口译训练的本地 Web 应用。后端使用 Java Spring Boot，前端使用 React + Vite，集成百度语音识别 ASR 和百度千帆大模型，用于完成素材听写、参考译文生成、术语提示、学生口译录音诊断等流程。

## 功能概览

- 支持上传口译素材音频
- 支持网页录音或上传学生口译录音
- 支持英译中 / 中译英方向切换
- 支持 MP3、M4A、AAC 自动转 WAV
- 支持较长音频自动分段 ASR
- 支持学生录音回放和下载
- 生成术语提示和口译诊断报告

## 项目结构

```text
AI-Assisted-Interpreter/
├── ai-interpreter-backend/     # Spring Boot 后端
├── ai-interpreter-frontend/    # React + Vite 前端
├── API_INTEGRATION_GUIDE.md
├── DEVELOPMENT_CHECKLIST.md
└── PROJECT_ROADMAP.md
```

## 环境要求

请先安装：

- JDK 17 或更高版本
- Maven
- Node.js 18 或更高版本
- npm
- FFmpeg

检查命令：

```bash
java -version
mvn -version
node -v
npm -v
ffmpeg -version
```

## 第一步：配置百度 API

本项目需要两组百度服务：

1. 百度语音识别 ASR：用于识别素材音频和学生口译录音。
2. 百度千帆 LLM：用于生成参考译文、术语卡片和诊断报告。

请在本机环境变量中配置：

```bash
BAIDU_ASR_API_KEY=你的百度ASR API Key
BAIDU_ASR_SECRET_KEY=你的百度ASR Secret Key
BAIDU_ASR_APP_ID=你的百度ASR App ID

BAIDU_LLM_API_KEY=你的百度千帆v2 API Key
BAIDU_LLM_MODEL=ernie-x1-turbo-32k

FFMPEG_PATH=ffmpeg
```

说明：

- 百度 ASR 仍需要 `API Key + Secret Key`。
- 百度千帆新版 v2 使用 `BAIDU_LLM_API_KEY` 作为 Bearer Key，不需要 Secret Key。
- 如果系统 PATH 中已经能直接运行 `ffmpeg`，`FFMPEG_PATH=ffmpeg` 即可。
- Windows 上也可以把 `FFMPEG_PATH` 配成完整路径，例如 `C:\Users\you\AppData\Local\Microsoft\WinGet\Links\ffmpeg.exe`。

### Windows PowerShell 配置示例

```powershell
[Environment]::SetEnvironmentVariable("BAIDU_ASR_API_KEY", "your-asr-api-key", "User")
[Environment]::SetEnvironmentVariable("BAIDU_ASR_SECRET_KEY", "your-asr-secret-key", "User")
[Environment]::SetEnvironmentVariable("BAIDU_ASR_APP_ID", "your-asr-app-id", "User")

[Environment]::SetEnvironmentVariable("BAIDU_LLM_API_KEY", "your-qianfan-v2-api-key", "User")
[Environment]::SetEnvironmentVariable("BAIDU_LLM_MODEL", "ernie-x1-turbo-32k", "User")
[Environment]::SetEnvironmentVariable("FFMPEG_PATH", "ffmpeg", "User")
```

配置完成后，建议重新打开终端。

## 第二步：启动后端

进入后端目录：

```bash
cd ai-interpreter-backend
```

启动 Spring Boot：

```bash
mvn spring-boot:run
```

后端默认运行在：

```text
http://localhost:8080
```

健康检查：

```text
http://localhost:8080/api/session/health
```

正常返回示例：

```json
{
  "code": 0,
  "message": "OK",
  "timestamp": 1780000000000
}
```

## 第三步：启动前端

另开一个终端，进入前端目录：

```bash
cd ai-interpreter-frontend
```

安装依赖：

```bash
npm install
```

启动开发服务器：

```bash
npm run dev
```

前端默认运行在：

```text
http://127.0.0.1:5173/
```

## 使用流程

1. 打开前端页面。
2. 选择口译方向：`英译中` 或 `中译英`。
3. 上传口译素材音频。
4. 等待 AI 完成素材听写、参考译文和术语提示生成。
5. 播放素材音频，按需暂停。
6. 使用网页录音，或上传学生口译录音。
7. 等待系统生成诊断报告。
8. 在页面查看评分、准确度、完整性、流畅性、术语问题和改进建议。

## 支持的音频格式

素材音频和学生口译录音都支持：

- WAV
- PCM
- MP3
- M4A
- AAC

非 WAV 音频会通过 FFmpeg 自动转换为适合百度 ASR 的 16k 单声道 WAV。

## 常见问题

### 1. 页面显示“无法连接后端服务”

请确认后端已经启动，并访问：

```text
http://localhost:8080/api/session/health
```

如果访问失败，说明后端没有正常运行。

### 2. ASR 返回参数错误或格式错误

常见原因：

- 没有安装 FFmpeg
- `FFMPEG_PATH` 配置错误
- 上传音频损坏
- 百度 ASR Key 或 Secret 配置错误

可以先运行：

```bash
ffmpeg -version
```

确认 FFmpeg 可用。

### 3. 60 秒左右录音处理很慢

这是正常现象。系统会先转码，再按百度 ASR 限制分段识别，最后调用大模型生成诊断报告。音频越长，等待时间越久。

### 4. 中译英识别不准

请确认页面口译方向选择为 `中译英`。方向会影响：

- 素材音频使用中文 ASR
- 学生口译录音使用英文 ASR
- 参考译文生成方向
- 术语卡片方向
- 诊断报告提示词

### 5. 不要提交真实 API Key

请不要把真实 API Key 写入代码或提交到 GitHub。推荐使用环境变量配置。

## 构建检查

后端检查：

```bash
cd ai-interpreter-backend
mvn test
```

前端检查：

```bash
cd ai-interpreter-frontend
npm run build
```

## 生产部署提示

当前项目更适合作为本地原型或课程演示版本。如果要部署给多人使用，建议补充：

- 数据库存储 Session，而不是只用内存 Map
- 用户登录和权限控制
- 上传文件定期清理任务
- API Key 服务端安全管理
- 长音频改用百度长语音识别接口
- HTTPS 和正式域名 CORS 配置

