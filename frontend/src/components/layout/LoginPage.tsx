/**
 * 登录页面组件 - Refined Modern Edition
 */
import { useState } from 'react';
import { Loader2, Sparkles, ArrowRight } from 'lucide-react';
import { useAppStore } from '../../store';
import { login, register } from '../../api';
import clsx from 'clsx';

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
      setError('Username and password are required');
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
        setError('Login failed. Please check your credentials.');
      }
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4 relative overflow-hidden bg-background">
      {/* Subtle Background Elements */}
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top_right,_var(--tw-gradient-stops))] from-primary/20 via-background to-background opacity-50" />
      <div className="absolute bottom-0 left-0 right-0 h-[50vh] bg-[radial-gradient(ellipse_at_bottom_left,_var(--tw-gradient-stops))] from-secondary/10 via-background to-background opacity-50" />
      
      {/* Grid Pattern */}
      <div className="absolute inset-0 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] opacity-20" />

      <div className="w-full max-w-[400px] relative z-10 animate-fade-in-up">
        {/* Logo */}
        <div className="text-center mb-10">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-primary/10 text-primary mb-6 shadow-glow-sm">
            <Sparkles className="w-8 h-8" />
          </div>
          <h1 className="text-3xl font-semibold tracking-tight text-foreground mb-2">
            Welcome back
          </h1>
          <p className="text-muted text-sm">
            Enter your details to access your workspace
          </p>
        </div>

        {/* Login form */}
        <form
          onSubmit={handleLogin}
          className="glass-card rounded-3xl p-8 shadow-2xl backdrop-blur-2xl"
        >
          {error && (
            <div className="mb-6 p-3 bg-red-500/10 border border-red-500/20 rounded-xl text-red-600 dark:text-red-400 text-xs font-medium flex items-center gap-2">
              <div className="w-1.5 h-1.5 bg-red-500 rounded-full" />
              {error}
            </div>
          )}

          <div className="space-y-5">
            <div>
              <label className="block text-xs font-medium text-muted-foreground uppercase tracking-wider mb-2 ml-1">
                Username
              </label>
              <input
                type="text"
                className="w-full px-4 py-3 bg-surface-highlight/50 border border-border/50 rounded-xl text-foreground outline-none focus:border-primary/50 focus:ring-4 focus:ring-primary/10 transition-all duration-300 placeholder:text-muted/40"
                placeholder="Enter username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                disabled={isLoading}
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-muted-foreground uppercase tracking-wider mb-2 ml-1">
                Password
              </label>
              <input
                type="password"
                className="w-full px-4 py-3 bg-surface-highlight/50 border border-border/50 rounded-xl text-foreground outline-none focus:border-primary/50 focus:ring-4 focus:ring-primary/10 transition-all duration-300 placeholder:text-muted/40"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={isLoading}
              />
            </div>

            <button
              type="submit"
              className={clsx(
                "w-full py-3.5 mt-2 rounded-xl font-medium text-sm flex items-center justify-center gap-2 transition-all duration-300",
                "bg-primary text-primary-foreground hover:bg-primary/90 hover:shadow-lg hover:-translate-y-0.5",
                isLoading && "opacity-70 cursor-not-allowed"
              )}
              disabled={isLoading}
            >
              {isLoading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Signing in...</span>
                </>
              ) : (
                <>
                  <span>Sign In</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </div>

          <p className="text-center text-muted/60 text-xs mt-8">
            New here? Account will be created automatically.
          </p>
        </form>
      </div>
    </div>
  );
}
