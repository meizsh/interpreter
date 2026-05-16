import React, { useState, useEffect, useRef } from 'react';
import { uploadAudioFile, getSessionMetadata, getSessionStatus, uploadStudentAudio, getDiagnosisReport } from './services/apiService';

const WS_URL = import.meta.env.VITE_WS_URL ?? 'ws://localhost:8080/ws/interpreter';

export default function App() {
  const [hints, setHints] = useState([]);
  const [revealedHints, setRevealedHints] = useState([]);
  const [hintPool, setHintPool] = useState([]);
  const [audioFileUrl, setAudioFileUrl] = useState(null);
  const [fileName, setFileName] = useState('');
  const [isPlaying, setIsPlaying] = useState(false);
  const [sessionId, setSessionId] = useState(null);
  const [sessionStatus, setSessionStatus] = useState('idle');
  const [serverTip, setServerTip] = useState('请先上传一段口译音频，AI 将自动分析。');
  const [errorMessage, setErrorMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [studentAudioUrl, setStudentAudioUrl] = useState(null);
  const [studentFileName, setStudentFileName] = useState('');
  const [diagnosisReport, setDiagnosisReport] = useState(null);
  const [gradingStatus, setGradingStatus] = useState('idle');
  const [isRecording, setIsRecording] = useState(false);
  const [isRecordingPaused, setIsRecordingPaused] = useState(false);
  const [recordingSeconds, setRecordingSeconds] = useState(0);

  const audioRef = useRef(null);
  const hintTimerRef = useRef(null);
  const pollTimerRef = useRef(null);
  const diagnosisPollRef = useRef(null);
  const wsRef = useRef(null);
  const mediaStreamRef = useRef(null);
  const audioContextRef = useRef(null);
  const sourceRef = useRef(null);
  const processorRef = useRef(null);
  const recordedBuffersRef = useRef([]);
  const recordingTimerRef = useRef(null);
  const sampleRateRef = useRef(16000);
  const isPausedRef = useRef(false);

  // 注入一段 CSS 动画
  useEffect(() => {
    const style = document.createElement('style');
    style.innerHTML = `
      @keyframes pulse {
        0% { box-shadow: 0 0 0 0 rgba(79, 70, 229, 0.4); }
        70% { box-shadow: 0 0 0 10px rgba(79, 70, 229, 0); }
        100% { box-shadow: 0 0 0 0 rgba(79, 70, 229, 0); }
      }
      .hover-card:hover { transform: translateY(-2px); box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1), 0 4px 6px -2px rgba(0,0,0,0.05); }
      .custom-scrollbar::-webkit-scrollbar { width: 6px; }
      .custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
      .custom-scrollbar::-webkit-scrollbar-thumb { background-color: #cbd5e1; border-radius: 20px; }
    `;
    document.head.appendChild(style);
    return () => document.head.removeChild(style);
  }, []);

  useEffect(() => {
    if (!sessionId) return;

    const pollStatus = async () => {
      try {
        const statusResponse = await getSessionStatus(sessionId);
        setSessionStatus(statusResponse.status);

        if (statusResponse.status === 'completed') {
          const metadata = await getSessionMetadata(sessionId);
          const terms = Array.isArray(metadata.term_hints) ? metadata.term_hints : [];
          setHintPool(terms.map((item, index) => ({ id: `${item.term || item.source}-${index}`, ...item })));
          setServerTip('AI 已经在后台成功听写并预翻译，绝对标尺已锁定！');
          setLoading(false);
          clearInterval(pollTimerRef.current);
        }

        if (statusResponse.status === 'graded') {
          setGradingStatus('completed');
          setServerTip('学生口译评估完成，可以查看诊断报告。');
        }

        if (statusResponse.status === 'error') {
          setServerTip('音频处理失败，请检查后端日志。');
          setErrorMessage(statusResponse.error_message || '处理错误');
          setLoading(false);
          clearInterval(pollTimerRef.current);
        }
      } catch (error) {
        console.error(error);
      }
    };

    pollStatus();
    pollTimerRef.current = window.setInterval(pollStatus, 2200);
    return () => window.clearInterval(pollTimerRef.current);
  }, [sessionId]);

  useEffect(() => {
    return () => {
      if (audioRef.current) {
        audioRef.current.pause();
      }
      window.clearInterval(hintTimerRef.current);
      window.clearInterval(pollTimerRef.current);
      window.clearInterval(diagnosisPollRef.current);
      window.clearInterval(recordingTimerRef.current);
      if (mediaStreamRef.current) {
        mediaStreamRef.current.getTracks().forEach((track) => track.stop());
      }
      if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
        wsRef.current.close();
      }
    };
  }, []);

  useEffect(() => {
    const ws = new WebSocket(WS_URL);
    ws.onopen = () => console.log('✅ WebSocket 已连接');
    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        if (data.hints && Array.isArray(data.hints) && data.hints.length > 0) {
          setHints((prev) => [...data.hints, ...prev]);
        }
      } catch (error) {
        console.warn('WebSocket 数据解析失败', error);
      }
    };
    ws.onerror = (err) => console.warn('WebSocket 错误', err);
    wsRef.current = ws;
    return () => {
      if (ws.readyState === WebSocket.OPEN) ws.close();
    };
  }, []);

  const handleFileUpload = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    setFileName(file.name);
    setAudioFileUrl(URL.createObjectURL(file));
    setHints([]);
    setRevealedHints([]);
    setHintPool([]);
    setStudentAudioUrl(null);
    setStudentFileName('');
    setDiagnosisReport(null);
    setErrorMessage('');
    setServerTip('文件已选中，正在上传到 AI 后端...');
    setLoading(true);
    setSessionStatus('uploading');

    try {
      const response = await uploadAudioFile(file);
      setSessionId(response.session_id);
      setSessionStatus(response.status || 'processing');
      setServerTip('AI 正在后台分析音频，请稍候...');
    } catch (error) {
      setErrorMessage(error.message);
      setServerTip('上传失败，请重试。');
      setLoading(false);
      setSessionStatus('error');
    }
  };

  const onPlay = () => {
    setIsPlaying(true);
    if (!audioRef.current) return;

    window.clearInterval(hintTimerRef.current);
    hintTimerRef.current = window.setInterval(() => {
      const currentTime = audioRef.current.currentTime;
      revealHints(currentTime);
    }, 300);
  };

  const onPause = () => {
    setIsPlaying(false);
    window.clearInterval(hintTimerRef.current);
  };

  const startRecording = async () => {
    if (isRecording) return;
    setErrorMessage('');
    setServerTip('正在请求麦克风权限...');

    try {
      if (audioRef.current && !audioRef.current.paused) {
        audioRef.current.pause();
      }
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const AudioContext = window.AudioContext || window.webkitAudioContext;
      const audioContext = new AudioContext();
      const source = audioContext.createMediaStreamSource(stream);
      const processor = audioContext.createScriptProcessor(4096, 1, 1);

      recordedBuffersRef.current = [];
      mediaStreamRef.current = stream;
      audioContextRef.current = audioContext;
      sourceRef.current = source;
      processorRef.current = processor;
      sampleRateRef.current = audioContext.sampleRate;

      processor.onaudioprocess = (event) => {
        if (!isPausedRef.current) {
          const channelData = event.inputBuffer.getChannelData(0);
          recordedBuffersRef.current.push(new Float32Array(channelData));
        }
      };

      source.connect(processor);
      processor.connect(audioContext.destination);
      setIsRecording(true);
      setIsRecordingPaused(false);
      setRecordingSeconds(0);
      setServerTip('录音已开始，点击暂停可中断，停止后自动上传。');
      recordingTimerRef.current = window.setInterval(() => {
        setRecordingSeconds((prev) => prev + 1);
      }, 1000);
    } catch (error) {
      console.error(error);
      setErrorMessage('麦克风权限拒绝或设备不可用。');
      setServerTip('录音失败，请检查麦克风设置。');
    }
  };

  const pauseRecording = () => {
    if (!isRecording || isRecordingPaused) return;
    isPausedRef.current = true;
    window.clearInterval(recordingTimerRef.current);
    setIsRecordingPaused(true);
    setServerTip('录音已暂停，可继续录音或停止上传当前已录内容。');
  };

  const resumeRecording = () => {
    if (!isRecording || !isRecordingPaused) return;
    isPausedRef.current = false;
    setIsRecordingPaused(false);
    setServerTip('录音已继续，可继续补录。');
    recordingTimerRef.current = window.setInterval(() => {
      setRecordingSeconds((prev) => prev + 1);
    }, 1000);
  };

  const stopRecording = async () => {
    if (!isRecording) return;
    setIsRecording(false);
    setIsRecordingPaused(false);
    window.clearInterval(recordingTimerRef.current);

    if (processorRef.current) {
      processorRef.current.disconnect();
      processorRef.current.onaudioprocess = null;
    }
    if (sourceRef.current) {
      sourceRef.current.disconnect();
    }
    if (audioContextRef.current) {
      await audioContextRef.current.close();
    }
    if (mediaStreamRef.current) {
      mediaStreamRef.current.getTracks().forEach((track) => track.stop());
    }

    const wavBlob = createWavBlob(recordedBuffersRef.current, sampleRateRef.current);
    const recordedFile = new File([wavBlob], `recording_${Date.now()}.wav`, { type: 'audio/wav' });
    handleRecordedAudioUpload(recordedFile, wavBlob);
  };

  const handleRecordedAudioUpload = async (recordedFile, blob) => {
    setFileName(recordedFile.name);
    const blobUrl = URL.createObjectURL(blob);
    setAudioFileUrl(blobUrl);
    setHints([]);
    setRevealedHints([]);
    setHintPool([]);
    setStudentAudioUrl(null);
    setStudentFileName('');
    setDiagnosisReport(null);
    setErrorMessage('');
    setServerTip('录音已生成，正在上传到 AI 后端。请手动点击播放器回放。');
    setLoading(true);
    setSessionStatus('uploading');

    try {
      const response = await uploadAudioFile(recordedFile);
      setSessionId(response.session_id);
      setSessionStatus(response.status || 'processing');
      setServerTip('AI 正在后台分析录音，请稍候...');
    } catch (error) {
      setErrorMessage(error.message);
      setServerTip('录音上传失败，请重试。');
      setLoading(false);
      setSessionStatus('error');
    }
  };

  const handleStudentAudioUpload = async (event) => {
    const file = event.target.files[0];
    if (!file || !sessionId) return;

    setStudentFileName(file.name);
    setStudentAudioUrl(URL.createObjectURL(file));
    setErrorMessage('');
    setServerTip('正在上传学生口译录音，AI 将进行诊断...');
    setGradingStatus('uploading');

    try {
      await uploadStudentAudio(sessionId, file);
      setServerTip('学生口译上传成功，AI 正在评分...');
      setGradingStatus('processing');
      setDiagnosisReport(null);

      if (diagnosisPollRef.current) {
        window.clearInterval(diagnosisPollRef.current);
      }

      const pollDiagnosis = async () => {
        try {
          const statusResponse = await getSessionStatus(sessionId);
          if (statusResponse.status === 'graded') {
            const report = await getDiagnosisReport(sessionId);
            setDiagnosisReport(report.diagnosis_report || report);
            setServerTip('诊断报告已生成。');
            setGradingStatus('completed');
            window.clearInterval(diagnosisPollRef.current);
          } else if (statusResponse.status === 'error') {
            setErrorMessage(statusResponse.error_message || '诊断失败');
            setServerTip('学生口译诊断失败，请重试。');
            setGradingStatus('error');
            window.clearInterval(diagnosisPollRef.current);
          }
        } catch (pollError) {
          console.error(pollError);
        }
      };

      await pollDiagnosis();
      diagnosisPollRef.current = window.setInterval(pollDiagnosis, 2200);
    } catch (error) {
      setErrorMessage(error.message);
      setServerTip('学生口译上传或评估失败。');
      setGradingStatus('error');
    }
  };

  const createWavBlob = (buffers, sampleRate) => {
    const mergedBuffer = flattenAudioBuffers(buffers);
    const wavBuffer = encodeWav(mergedBuffer, sampleRate);
    return new Blob([wavBuffer], { type: 'audio/wav' });
  };

  const flattenAudioBuffers = (buffers) => {
    const length = buffers.reduce((sum, buffer) => sum + buffer.length, 0);
    const result = new Float32Array(length);
    let offset = 0;
    buffers.forEach((buffer) => {
      result.set(buffer, offset);
      offset += buffer.length;
    });
    return result;
  };

  const encodeWav = (samples, sampleRate) => {
    const buffer = new ArrayBuffer(44 + samples.length * 2);
    const view = new DataView(buffer);

    writeString(view, 0, 'RIFF');
    view.setUint32(4, 36 + samples.length * 2, true);
    writeString(view, 8, 'WAVE');
    writeString(view, 12, 'fmt ');
    view.setUint32(16, 16, true);
    view.setUint16(20, 1, true);
    view.setUint16(22, 1, true);
    view.setUint32(24, sampleRate, true);
    view.setUint32(28, sampleRate * 2, true);
    view.setUint16(32, 2, true);
    view.setUint16(34, 16, true);
    writeString(view, 36, 'data');
    view.setUint32(40, samples.length * 2, true);

    floatTo16BitPCM(view, 44, samples);
    return view;
  };

  const floatTo16BitPCM = (output, offset, input) => {
    for (let i = 0; i < input.length; i += 1) {
      const s = Math.max(-1, Math.min(1, input[i]));
      output.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7fff, true);
      offset += 2;
    }
  };

  const writeString = (view, offset, string) => {
    for (let i = 0; i < string.length; i += 1) {
      view.setUint8(offset + i, string.charCodeAt(i));
    }
  };

  const revealHints = (currentTime) => {
    const newHints = hintPool.filter((hint) => {
      return !revealedHints.some((item) => item.id === hint.id) && (hint.timestamp || 0) <= currentTime + 0.5;
    });

    if (newHints.length > 0) {
      setRevealedHints((prev) => [...prev, ...newHints].sort((a, b) => (a.timestamp || 0) - (b.timestamp || 0)));
      setServerTip('术语已锁定，继续盲听并注意表达。');
    }
  };

  const downloadAudio = () => {
    if (!audioFileUrl || !fileName) return;

    const link = document.createElement('a');
    link.href = audioFileUrl;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const activeHints = revealedHints.length > 0 ? revealedHints : hints;
  const hintTitle = sessionStatus === 'completed' ? '术语卡片已锁定' : '等待音频播放产生数据...';

  return (
    <div style={{ display: 'flex', minHeight: '100vh', fontFamily: 'Inter, Segoe UI, sans-serif', backgroundColor: '#f8fafc', color: '#334155' }}>
      <div style={{ flex: 7, padding: '40px 50px', display: 'flex', flexDirection: 'column' }}>
        <header style={{ marginBottom: '40px' }}>
          <h1 style={{ fontSize: '28px', fontWeight: 800, color: '#0f172a', margin: '0 0 8px 0', display: 'flex', alignItems: 'center', gap: '12px' }}>
            <span style={{ fontSize: '32px' }}>🎙️</span> AIAI 人机协同口译工作台
          </h1>
          <p style={{ color: '#64748b', fontSize: '15px', margin: 0 }}>基于 AI 大模型的同声传译与听辨辅助训练系统</p>
        </header>

        <div style={{ backgroundColor: '#ffffff', borderRadius: '24px', padding: '40px', boxShadow: '0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.03)', border: '1px solid #f1f5f9' }}>
          <div style={{ position: 'relative', border: '2px dashed #cbd5e1', borderRadius: '16px', padding: '40px 20px', textAlign: 'center', textTransform: 'none', backgroundColor: '#f8fafc' }}>
            <input type="file" accept="audio/*" onChange={handleFileUpload} id="audio-upload" style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', opacity: 0, cursor: 'pointer' }} />
            <div style={{ fontSize: '40px', marginBottom: '12px' }}>{fileName ? '🎵' : '📁'}</div>
            <h3 style={{ margin: '0 0 8px 0', color: '#334155', fontSize: '18px' }}>{fileName ? fileName : '点击或拖拽上传音频文件'}</h3>
            <p style={{ margin: 0, color: '#94a3b8', fontSize: '14px' }}>支持 MP3、WAV、M4A 等音频格式。</p>
          </div>

          <div style={{ marginTop: '20px', display: 'flex', alignItems: 'center', gap: '14px' }}>
            {!isRecording ? (
              <button
                type="button"
                onClick={startRecording}
                style={{
                  border: 'none',
                  borderRadius: '14px',
                  padding: '14px 22px',
                  backgroundColor: '#2563eb',
                  color: '#ffffff',
                  fontWeight: 700,
                  cursor: 'pointer',
                  boxShadow: '0 10px 25px rgba(37, 99, 235, 0.2)',
                }}
              >
                🎧 开始录音
              </button>
            ) : (
              <>
                <button
                  type="button"
                  onClick={isRecordingPaused ? resumeRecording : pauseRecording}
                  style={{
                    border: 'none',
                    borderRadius: '14px',
                    padding: '14px 22px',
                    backgroundColor: isRecordingPaused ? '#10b981' : '#f59e0b',
                    color: '#ffffff',
                    fontWeight: 700,
                    cursor: 'pointer',
                    boxShadow: '0 10px 25px rgba(245, 158, 11, 0.2)',
                  }}
                >
                  {isRecordingPaused ? `▶️ 继续录音 (${recordingSeconds}s)` : `⏸️ 暂停录音 (${recordingSeconds}s)`}
                </button>
                <button
                  type="button"
                  onClick={stopRecording}
                  style={{
                    border: 'none',
                    borderRadius: '14px',
                    padding: '14px 22px',
                    backgroundColor: '#ef4444',
                    color: '#ffffff',
                    fontWeight: 700,
                    cursor: 'pointer',
                    boxShadow: '0 10px 25px rgba(239, 68, 68, 0.2)',
                  }}
                >
                  ⏹️ 停止并上传
                </button>
              </>
            )}
            <span style={{ color: '#475569', fontSize: '14px' }}>
              {isRecording ? (isRecordingPaused ? '录音已暂停，可继续录制当前片段。' : '录音中，可随时暂停并继续。') : '先听一段，再录一段，支持中断继续。'}
            </span>
          </div>

          <div style={{ marginTop: '30px', padding: '24px', backgroundColor: '#eff6ff', borderRadius: '16px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontWeight: 600, color: '#1e40af', display: 'flex', alignItems: 'center', gap: '8px' }}>
                {loading ? (
                  <><div style={{ width: '10px', height: '10px', backgroundColor: '#4f46e5', borderRadius: '50%', animation: 'pulse 1.5s infinite' }}></div> AI 正在后台进行静默分析</>
                ) : sessionStatus === 'completed' ? (
                  '🎯 AI 已锁定底牌，可进入实战状态'
                ) : (
                  '▶️ 准备就绪，点击播放开始分析'
                )}
              </span>
              <span style={{ fontSize: '12px', background: '#dbeafe', color: '#1e40af', padding: '4px 10px', borderRadius: '20px', fontWeight: 600 }}>V2 引擎</span>
            </div>
            {audioFileUrl && (
              <div>
                <audio ref={audioRef} controls src={audioFileUrl} onPlay={onPlay} onPause={onPause} onEnded={onPause} style={{ width: '100%', height: '40px', outline: 'none' }} />
                <button
                  onClick={downloadAudio}
                  style={{
                    marginTop: '12px',
                    padding: '8px 14px',
                    backgroundColor: '#10b981',
                    color: '#ffffff',
                    border: 'none',
                    borderRadius: '8px',
                    fontWeight: 600,
                    cursor: 'pointer',
                    fontSize: '13px',
                  }}
                >
                  ⬇️ 下载口译音频
                </button>
              </div>
            )}
            {sessionId && (
              <div style={{ marginTop: '18px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
                <label style={{ display: 'block', fontWeight: 600, color: '#0f172a' }}>上传学生口译录音</label>
                <input type="file" accept="audio/*" onChange={handleStudentAudioUpload} style={{ width: '100%' }} />
                {studentAudioUrl && <div style={{ fontSize: '13px', color: '#64748b' }}>已选择：{studentFileName}</div>}
                {gradingStatus === 'processing' && <div style={{ fontSize: '13px', color: '#0f172a' }}>AI 评分进行中...</div>}
                {diagnosisReport && (
                  <div style={{ marginTop: '12px', padding: '16px', background: '#f8fafc', borderRadius: '14px', border: '1px solid #e2e8f0' }}>
                    <div style={{ fontWeight: 700, marginBottom: '10px' }}>诊断报告</div>
                    <div style={{ fontSize: '14px', color: '#334155', marginBottom: '8px' }}>综合评分：{diagnosisReport.score ?? 'N/A'}</div>
                    <div style={{ fontSize: '14px', color: '#334155', marginBottom: '8px' }}><strong>准确度：</strong>{diagnosisReport.accuracy}</div>
                    <div style={{ fontSize: '14px', color: '#334155', marginBottom: '8px' }}><strong>完整性：</strong>{diagnosisReport.completeness}</div>
                    <div style={{ fontSize: '14px', color: '#334155', marginBottom: '8px' }}><strong>流畅性：</strong>{diagnosisReport.fluency}</div>
                    <div style={{ fontSize: '14px', color: '#334155', marginBottom: '8px' }}><strong>术语：</strong>{diagnosisReport.terminology}</div>
                    {Array.isArray(diagnosisReport.main_issues) && diagnosisReport.main_issues.length > 0 && (
                      <div style={{ fontSize: '14px', color: '#334155' }}><strong>主要问题：</strong> {diagnosisReport.main_issues.join('； ')}</div>
                    )}
                  </div>
                )}
              </div>
            )}
            <div style={{ fontSize: '14px', color: errorMessage ? '#b91c1c' : '#475569' }}>{errorMessage || serverTip}</div>
          </div>
        </div>
      </div>

      <div className="custom-scrollbar" style={{ flex: 4, backgroundColor: '#ffffff', borderLeft: '1px solid #e2e8f0', padding: '40px 30px', overflowY: 'auto', display: 'flex', flexDirection: 'column' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', paddingBottom: '16px', borderBottom: '2px solid #f1f5f9' }}>
          <h2 style={{ fontSize: '18px', fontWeight: 700, color: '#0f172a', margin: 0, display: 'flex', alignItems: 'center', gap: '8px' }}>
            ⚡ 增强外脑 (Hints)
          </h2>
          <span style={{ fontSize: '12px', color: '#64748b', background: '#f1f5f9', padding: '4px 8px', borderRadius: '6px' }}>实时提示</span>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {activeHints.length === 0 ? (
            <div style={{ textAlign: 'center', color: '#94a3b8', marginTop: '60px', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px' }}>
              <span style={{ fontSize: '40px', opacity: 0.5 }}>🎧</span>
              <p>{hintTitle}</p>
            </div>
          ) : (
            activeHints.map((hint) => (
              <div key={hint.id} className="hover-card" style={{ padding: '20px', borderRadius: '16px', backgroundColor: hint.type === 'number' ? '#f0fdf4' : '#f8fafc', border: `1px solid ${hint.type === 'number' ? '#bbf7d0' : '#e2e8f0'}`, borderLeft: `4px solid ${hint.type === 'number' ? '#22c55e' : '#6366f1'}`, transition: 'all 0.3s ease' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
                  <span style={{ fontSize: '18px', fontWeight: 700, color: '#0f172a' }}>{hint.source || hint.term || 'Unknown'}</span>
                  <span style={{ fontSize: '12px', fontWeight: 600, color: hint.type === 'number' ? '#166534' : '#4f46e5', backgroundColor: hint.type === 'number' ? '#dcfce7' : '#e0e7ff', padding: '2px 8px', borderRadius: '12px' }}>
                    {hint.type === 'number' ? '数字校验' : '术语提示'}
                  </span>
                </div>
                <div style={{ fontSize: '15px', color: '#475569', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span style={{ color: '#94a3b8' }}>➔</span> {hint.translation || '暂无翻译'}
                </div>
                {(hint.timestamp || 0) > 0 && (
                  <div style={{ marginTop: '10px', fontSize: '12px', color: '#64748b' }}>时间: {Math.round((hint.timestamp || 0) * 10) / 10}s</div>
                )}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}