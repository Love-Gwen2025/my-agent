/**
 * 登录页面组件
 * 
 * Modern glassmorphism design with gradient background
 */
import { useState } from 'react';
import { Loader2, Sparkles } from 'lucide-react';
import { useAppStore } from '../../store';
import { login, register } from '../../api';

/**
 * 登录页面
 */
export function LoginPage() {
  const { setUser, setToken } = useAppStore();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  /** 处理登录（登录即注册） */
  async function handleLogin(e: React.FormEvent) {
    e.preventDefault();
    if (!username.trim() || !password.trim()) {
      setError('请输入用户名和密码');
      return;
    }

    setIsLoading(true);
    setError('');

    const params = { userCode: username, userPassword: password };

    try {
      // 1. 先尝试登录
      const token = await login(params);
      setToken(token);
      setUser({ id: 0, userCode: username, userName: username });
    } catch (loginErr) {
      try {
        // 2. 登录失败，尝试自动注册
        await register(params);
        // 3. 注册成功后重新登录
        const token = await login(params);
        setToken(token);
        setUser({ id: 0, userCode: username, userName: username });
      } catch (registerErr) {
        // 4. 注册也失败，说明是密码错误
        setError('登录失败，请检查用户名和密码');
      }
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4 relative overflow-hidden">
      {/* Animated gradient background */}
      <div
        className="absolute inset-0 opacity-30"
        style={{
          background: 'linear-gradient(-45deg, #667eea, #764ba2, #00d4ff, #a855f7)',
          backgroundSize: '400% 400%',
          animation: 'gradient-shift 15s ease infinite',
        }}
      />

      {/* Floating orbs */}
      <div
        className="absolute w-72 h-72 rounded-full opacity-20 blur-3xl"
        style={{
          background: 'var(--accent-gradient)',
          top: '10%',
          left: '10%',
          animation: 'float 6s ease-in-out infinite',
        }}
      />
      <div
        className="absolute w-96 h-96 rounded-full opacity-15 blur-3xl"
        style={{
          background: 'linear-gradient(135deg, #00d4ff 0%, #a855f7 100%)',
          bottom: '10%',
          right: '10%',
          animation: 'float 8s ease-in-out infinite reverse',
        }}
      />

      <div className="w-full max-w-md relative z-10 animate-fade-in-up">
        {/* Logo */}
        <div className="text-center mb-8">
          <div
            className="w-20 h-20 rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-lg animate-float"
            style={{ background: 'var(--accent-gradient)' }}
          >
            <Sparkles className="w-10 h-10 text-white" />
          </div>
          <h1 className="text-3xl font-bold gradient-text">
            AI Chat
          </h1>
          <p className="text-[var(--text-secondary)] mt-2 text-lg">
            智能对话助手
          </p>
        </div>

        {/* Login form - Glass effect */}
        <form
          onSubmit={handleLogin}
          className="glass-effect rounded-3xl p-8 shadow-lg"
        >
          {error && (
            <div className="mb-6 p-4 bg-red-500/10 border border-red-500/30 rounded-xl text-red-400 text-sm flex items-center gap-2">
              <span className="w-2 h-2 bg-red-400 rounded-full animate-pulse" />
              {error}
            </div>
          )}

          <div className="mb-5">
            <label className="block text-sm font-medium text-[var(--text-secondary)] mb-2">
              用户名
            </label>
            <input
              type="text"
              className="w-full px-4 py-3 bg-[var(--bg-primary)] border-2 border-[var(--border-color)] rounded-xl text-[var(--text-primary)] outline-none focus:border-[var(--accent-primary)] focus:shadow-lg transition-all duration-300 placeholder:text-[var(--text-secondary)]"
              placeholder="请输入用户名"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              disabled={isLoading}
            />
          </div>

          <div className="mb-8">
            <label className="block text-sm font-medium text-[var(--text-secondary)] mb-2">
              密码
            </label>
            <input
              type="password"
              className="w-full px-4 py-3 bg-[var(--bg-primary)] border-2 border-[var(--border-color)] rounded-xl text-[var(--text-primary)] outline-none focus:border-[var(--accent-primary)] focus:shadow-lg transition-all duration-300 placeholder:text-[var(--text-secondary)]"
              placeholder="请输入密码"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={isLoading}
            />
          </div>

          <button
            type="submit"
            className="w-full py-3 text-white rounded-xl font-medium text-lg btn-gradient flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
            disabled={isLoading}
          >
            {isLoading ? (
              <>
                <Loader2 className="w-5 h-5 animate-spin" />
                登录中...
              </>
            ) : (
              <>
                <Sparkles className="w-5 h-5" />
                开始体验
              </>
            )}
          </button>

          <p className="text-center text-[var(--text-secondary)] text-sm mt-6">
            首次使用？系统将自动为您创建账号
          </p>
        </form>
      </div>
    </div>
  );
}
