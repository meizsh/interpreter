const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

export async function uploadAudioFile(file) {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch(`${API_URL}/api/audio/upload`, {
    method: 'POST',
    body: formData,
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: '上传失败' }));
    throw new Error(error.message || '上传失败');
  }

  return response.json();
}

export async function getSessionMetadata(sessionId) {
  const response = await fetch(`${API_URL}/api/session/${sessionId}/metadata`);
  if (!response.ok) {
    throw new Error('获取元数据失败');
  }
  return response.json();
}

export async function getSessionStatus(sessionId) {
  const response = await fetch(`${API_URL}/api/session/${sessionId}/status`);
  if (!response.ok) {
    throw new Error('获取状态失败');
  }
  return response.json();
}

export async function uploadStudentAudio(sessionId, file) {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch(`${API_URL}/api/audio/${sessionId}/student-audio`, {
    method: 'POST',
    body: formData,
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: '上传学生录音失败' }));
    throw new Error(error.message || '上传学生录音失败');
  }

  return response.json();
}

export async function getDiagnosisReport(sessionId) {
  const response = await fetch(`${API_URL}/api/session/${sessionId}/report`);
  if (!response.ok) {
    const errorBody = await response.json().catch(() => ({}));
    throw new Error(errorBody.message || '获取诊断报告失败');
  }
  return response.json();
}
