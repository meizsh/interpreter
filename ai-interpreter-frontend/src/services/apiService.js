const BASE_URL = "http://localhost:8080/api";

// 1. 上传演讲原音音频
export const uploadAudioFile = async (file) => {
  const formData = new FormData();
  formData.append("file", file);
  const response = await fetch(`${BASE_URL}/audio/upload`, { method: "POST", body: formData });
  return await response.json();
};

// 2. 获取 Session 状态
export const getSessionStatus = async (sessionId) => {
  const response = await fetch(`${BASE_URL}/session/${sessionId}`);
  return await response.json();
};

// 3. 获取会话元数据（术语卡片、原文、标准答案）
export const getSessionMetadata = async (sessionId) => {
  const response = await fetch(`${BASE_URL}/session/${sessionId}/metadata`);
  return await response.json();
};

// 4. 上传学生口译录音音频
export const uploadStudentAudio = async (sessionId, audioBlob) => {
  const formData = new FormData();
  // 必须和后端 Controller 要求的入参 @RequestParam("file") 一致
  formData.append("file", audioBlob, "student_expression.wav"); 
  const response = await fetch(`${BASE_URL}/audio/${sessionId}/student-audio`, { method: "POST", body: formData });
  return await response.json();
};

// 5. 获取诊断报告
export const getDiagnosisReport = async (sessionId) => {
  const response = await fetch(`${BASE_URL}/session/${sessionId}/report`);
  return await response.json();
};

// 向后兼容的对象导出
export const apiService = {
  uploadAudio: uploadAudioFile,
  getSession: getSessionStatus,
  uploadStudentAudio: uploadStudentAudio,
  getSessionMetadata: getSessionMetadata,
  getDiagnosisReport: getDiagnosisReport
};