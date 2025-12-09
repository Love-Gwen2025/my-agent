/**
 * 登录页面组件
 */
import { useState } from 'react';
import { MessageSquare, Loader2 } from 'lucide-react';
import { useAppStore } from '../../store';
import { login } from '../../api';

/**
 * 登录页面
 */
export function LoginPage() {
  const { setUser, setToken } = useAppStore();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  /** 处理登录 */
  async function handleLogin(e: React.FormEvent) {
    e.preventDefault();
    if (!username.trim() || !password.trim()) {
      setError('请输入用户名和密码');
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      const response = await login({ username, password });
      setToken(response.token);
      setUser(response.user);
    } catch (err) {
      setError('登录失败，请检查用户名和密码');
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-[var(--bg-primary)] flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="w-16 h-16 bg-[var(--accent-primary)] rounded-2xl flex items-center justify-center mx-auto mb-4">
            <MessageSquare className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-[var(--text-primary)]">
            AI Chat
          </h1>
          <p className="text-[var(--text-secondary)] mt-2">
            智能对话助手
          </p>
        </div>

        {/* 登录表单 */}
        <form
          onSubmit={handleLogin}
          className="bg-[var(--bg-secondary)] rounded-2xl p-6 border border-[var(--border-color)]"
        >
          {error && (
            <div className="mb-4 p-3 bg-red-500/10 border border-red-500/30 rounded-lg text-red-400 text-sm">
              {error}
            </div>
          )}

          <div className="mb-4">
            <label className="block text-sm text-[var(--text-secondary)] mb-2">
              用户名
            </label>
            <input
              type="text"
              className="w-full px-4 py-2 bg-[var(--bg-primary)] border border-[var(--border-color)] rounded-lg text-[var(--text-primary)] outline-none focus:border-[var(--accent-primary)] transition-colors"
              placeholder="请输入用户名"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              disabled={isLoading}
            />
          </div>

          <div className="mb-6">
            <label className="block text-sm text-[var(--text-secondary)] mb-2">
              密码
            </label>
            <input
              type="password"
              className="w-full px-4 py-2 bg-[var(--bg-primary)] border border-[var(--border-color)] rounded-lg text-[var(--text-primary)] outline-none focus:border-[var(--accent-primary)] transition-colors"
              placeholder="请输入密码"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={isLoading}
            />
          </div>

          <button
            type="submit"
            className="w-full py-2 bg-[var(--accent-primary)] text-white rounded-lg hover:opacity-90 transition-opacity flex items-center justify-center gap-2 disabled:opacity-50"
            disabled={isLoading}
          >
            {isLoading ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                登录中...
              </>
            ) : (
              '登录'
            )}
          </button>
        </form>
      </div>
    </div>
  );
}
