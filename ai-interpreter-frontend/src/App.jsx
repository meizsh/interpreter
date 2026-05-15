import React, { useState, useEffect, useRef } from 'react';

export default function App() {
  const [hints, setHints] = useState([]);
  const [audioFileUrl, setAudioFileUrl] = useState(null);
  const [fileName, setFileName] = useState("");
  const [isPlaying, setIsPlaying] = useState(false);
  const wsRef = useRef(null);
  const captureIntervalRef = useRef(null);

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
    const ws = new WebSocket('ws://localhost:8080/ws/interpreter');
    ws.onopen = () => console.log('✅ 成功连接后端');
    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      if (data.hints && data.hints.length > 0) {
        setHints(prev => [...data.hints, ...prev]);
      }
    };
    wsRef.current = ws;
    return () => {
      if (ws.readyState === 1) ws.close();
      clearInterval(captureIntervalRef.current);
    };
  }, []);

  const handleFileUpload = (event) => {
    const file = event.target.files[0];
    if (file) {
      setFileName(file.name);
      setAudioFileUrl(URL.createObjectURL(file));
      setHints([]);
    }
  };

  const onPlay = () => {
    setIsPlaying(true);
    const sendMockData = () => {
      if (wsRef.current?.readyState === WebSocket.OPEN) {
        wsRef.current.send(JSON.stringify({ timestamp: Date.now(), audioData: "base64" }));
      }
    };
    sendMockData(); // 立刻发一次
    captureIntervalRef.current = setInterval(sendMockData, 8000);
  };

  const onPause = () => {
    setIsPlaying(false);
    clearInterval(captureIntervalRef.current);
  };

  return (
    <div style={{ display: 'flex', height: '100vh', fontFamily: '"Inter", "Segoe UI", sans-serif', backgroundColor: '#f8fafc', color: '#334155' }}>
      
      {/* 左侧工作区 */}
      <div style={{ flex: 7, padding: '40px 50px', display: 'flex', flexDirection: 'column' }}>
        <header style={{ marginBottom: '40px' }}>
          <h1 style={{ fontSize: '28px', fontWeight: '800', color: '#0f172a', margin: '0 0 8px 0', display: 'flex', alignItems: 'center', gap: '12px' }}>
            <span style={{ fontSize: '32px' }}>🎙️</span> AIAI 人机协同口译工作台
          </h1>
          <p style={{ color: '#64748b', fontSize: '15px', margin: 0 }}>基于 AI大模型 的同声传译与听辨辅助训练系统</p>
        </header>

        {/* 核心操作卡片 */}
        <div style={{ backgroundColor: '#ffffff', borderRadius: '24px', padding: '40px', boxShadow: '0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.03)', border: '1px solid #f1f5f9' }}>
          
          {/* 上传区域 */}
          <div style={{ position: 'relative', border: '2px dashed #cbd5e1', borderRadius: '16px', padding: '40px 20px', textAlign: 'center', transition: 'all 0.2s', backgroundColor: '#f8fafc' }}>
            <input type="file" accept="audio/*" onChange={handleFileUpload} id="audio-upload" style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', opacity: 0, cursor: 'pointer' }} />
            <div style={{ fontSize: '40px', marginBottom: '12px' }}>{fileName ? '🎵' : '📁'}</div>
            <h3 style={{ margin: '0 0 8px 0', color: '#334155', fontSize: '18px' }}>
              {fileName ? fileName : "点击或拖拽上传音频文件"}
            </h3>
            <p style={{ margin: 0, color: '#94a3b8', fontSize: '14px' }}>支持 MP3, WAV 格式</p>
          </div>

          {/* 播放器与状态指示 */}
          {audioFileUrl && (
            <div style={{ marginTop: '30px', padding: '24px', backgroundColor: '#eff6ff', borderRadius: '16px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontWeight: '600', color: '#1e40af', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  {isPlaying ? (
                    <><div style={{ width: '10px', height: '10px', backgroundColor: '#4f46e5', borderRadius: '50%', animation: 'pulse 1.5s infinite' }}></div> AI 实时监听分析中</>
                  ) : "▶️ 准备就绪，点击播放开始分析"}
                </span>
                <span style={{ fontSize: '12px', background: '#dbeafe', color: '#1e40af', padding: '4px 10px', borderRadius: '20px', fontWeight: '600' }}>V2 引擎</span>
              </div>
              <audio controls src={audioFileUrl} onPlay={onPlay} onPause={onPause} onEnded={onPause} style={{ width: '100%', height: '40px', outline: 'none' }} />
            </div>
          )}
        </div>
      </div>

      {/* 右侧提示区 (侧边栏) */}
      <div className="custom-scrollbar" style={{ flex: 4, backgroundColor: '#ffffff', borderLeft: '1px solid #e2e8f0', padding: '40px 30px', overflowY: 'auto', display: 'flex', flexDirection: 'column' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', paddingBottom: '16px', borderBottom: '2px solid #f1f5f9' }}>
          <h2 style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a', margin: 0, display: 'flex', alignItems: 'center', gap: '8px' }}>
            ⚡ 增强外脑 (Hints)
          </h2>
          <span style={{ fontSize: '12px', color: '#64748b', background: '#f1f5f9', padding: '4px 8px', borderRadius: '6px' }}>实时提取</span>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {hints.length === 0 && (
            <div style={{ textAlign: 'center', color: '#94a3b8', marginTop: '60px', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px' }}>
              <span style={{ fontSize: '40px', opacity: 0.5 }}>🎧</span>
              <p>等待音频播放产生数据...</p>
            </div>
          )}
          
          {hints.map((hint, index) => (
            <div key={index} className="hover-card" style={{ padding: '20px', borderRadius: '16px', backgroundColor: hint.type === 'number' ? '#f0fdf4' : '#f8fafc', border: `1px solid ${hint.type === 'number' ? '#bbf7d0' : '#e2e8f0'}`, borderLeft: `4px solid ${hint.type === 'number' ? '#22c55e' : '#6366f1'}`, transition: 'all 0.3s ease' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
                <span style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a' }}>{hint.source}</span>
                <span style={{ fontSize: '12px', fontWeight: '600', color: hint.type === 'number' ? '#166534' : '#4f46e5', backgroundColor: hint.type === 'number' ? '#dcfce7' : '#e0e7ff', padding: '2px 8px', borderRadius: '12px' }}>
                  {hint.type === 'number' ? '数字校验' : '术语提示'}
                </span>
              </div>
              <div style={{ fontSize: '15px', color: '#475569', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ color: '#94a3b8' }}>➔</span> {hint.translation}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}