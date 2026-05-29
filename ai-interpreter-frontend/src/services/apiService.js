const BASE_URL = "http://localhost:8080/api";

const requestJson = async (url, options) => {
  let response;
  try {
    response = await fetch(url, options);
  } catch (error) {
    throw new Error("无法连接后端服务，请确认后端已启动。");
  }

  const data = await response.json().catch(() => ({}));
  if (!response.ok || data.code === -1) {
    throw new Error(data.message || `请求失败：${response.status}`);
  }
  return data;
};

export const uploadAudioFile = async (file, direction = "en-zh") => {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("direction", direction);
  return requestJson(`${BASE_URL}/audio/upload`, { method: "POST", body: formData });
};

export const getSessionStatus = async (sessionId) => {
  return requestJson(`${BASE_URL}/session/${sessionId}/status`);
};

export const getSessionMetadata = async (sessionId) => {
  return requestJson(`${BASE_URL}/session/${sessionId}/metadata`);
};

export const uploadStudentAudio = async (sessionId, audioBlob) => {
  const formData = new FormData();
  formData.append("studentAudio", audioBlob, audioBlob.name || "student_expression.wav");
  return requestJson(`${BASE_URL}/audio/${sessionId}/student-audio`, { method: "POST", body: formData });
};

export const getDiagnosisReport = async (sessionId) => {
  return requestJson(`${BASE_URL}/session/${sessionId}/report`);
};

export const apiService = {
  uploadAudio: uploadAudioFile,
  getSession: getSessionStatus,
  uploadStudentAudio,
  getSessionMetadata,
  getDiagnosisReport
};
