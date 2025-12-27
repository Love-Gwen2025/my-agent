/**
 * 用户资料编辑弹窗
 *
 * 包含两个面板：
 * - 基础信息：头像、用户名、手机号、地址
 * - 隐私设置：密码修改
 */
import { useState, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X, User, Lock, Camera, Check, Loader2 } from 'lucide-react';
import clsx from 'clsx';
import { useAppStore } from '../../store';
import { uploadAvatar, updateProfile, changePassword } from '../../api/auth';

interface UserProfileModalProps {
    isOpen: boolean;
    onClose: () => void;
}

type TabType = 'basic' | 'privacy';

export function UserProfileModal({ isOpen, onClose }: UserProfileModalProps) {
    const { user, setUser } = useAppStore();
    const [activeTab, setActiveTab] = useState<TabType>('basic');
    const [isEditing, setIsEditing] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);

    // 基础信息编辑表单
    const [editForm, setEditForm] = useState({
        userName: user?.userName || '',
        userPhone: '',
        address: '',
    });

    // 密码修改表单
    const [passwordForm, setPasswordForm] = useState({
        oldPassword: '',
        newPassword: '',
        confirmPassword: '',
    });
    const [isChangingPassword, setIsChangingPassword] = useState(false);

    const fileInputRef = useRef<HTMLInputElement>(null);

    // 处理头像上传
    const handleAvatarClick = () => {
        fileInputRef.current?.click();
    };

    const handleAvatarChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;

        setIsLoading(true);
        setError(null);
        try {
            const avatarUrl = await uploadAvatar(file);
            if (user) {
                setUser({ ...user, avatar: avatarUrl });
            }
            setSuccess('头像上传成功');
            setTimeout(() => setSuccess(null), 2000);
        } catch {
            setError('头像上传失败');
        } finally {
            setIsLoading(false);
        }
    };

    // 保存基础信息
    const handleSaveProfile = async () => {
        setIsLoading(true);
        setError(null);
        try {
            const updatedUser = await updateProfile({
                userName: editForm.userName || undefined,
                userPhone: editForm.userPhone || undefined,
                address: editForm.address || undefined,
            });
            setUser({ ...user, ...updatedUser });
            setIsEditing(false);
            setSuccess('保存成功');
            setTimeout(() => setSuccess(null), 2000);
        } catch {
            setError('保存失败');
        } finally {
            setIsLoading(false);
        }
    };

    // 修改密码
    const handleChangePassword = async () => {
        if (passwordForm.newPassword !== passwordForm.confirmPassword) {
            setError('两次输入的新密码不一致');
            return;
        }
        if (passwordForm.newPassword.length < 6) {
            setError('新密码至少需要6位');
            return;
        }

        setIsLoading(true);
        setError(null);
        try {
            await changePassword(passwordForm.oldPassword, passwordForm.newPassword);
            setPasswordForm({ oldPassword: '', newPassword: '', confirmPassword: '' });
            setIsChangingPassword(false);
            setSuccess('密码修改成功');
            setTimeout(() => setSuccess(null), 2000);
        } catch {
            setError('旧密码不正确');
        } finally {
            setIsLoading(false);
        }
    };

    const tabs = [
        { id: 'basic' as const, label: '基础信息', icon: User },
        { id: 'privacy' as const, label: '隐私设置', icon: Lock },
    ];

    return (
        <AnimatePresence>
            {isOpen && (
                <>
                    {/* 遮罩层 */}
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50"
                        onClick={onClose}
                    />

                    {/* 弹窗主体 */}
                    <motion.div
                        initial={{ opacity: 0, scale: 0.95, y: 20 }}
                        animate={{ opacity: 1, scale: 1, y: 0 }}
                        exit={{ opacity: 0, scale: 0.95, y: 20 }}
                        transition={{ duration: 0.2 }}
                        className="fixed inset-0 flex items-center justify-center z-50 p-4"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <div className="w-full max-w-2xl bg-white dark:bg-gray-900 rounded-2xl shadow-2xl overflow-hidden border border-gray-200 dark:border-gray-700">
                            {/* 标题栏 */}
                            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800">
                                <h2 className="text-lg font-semibold text-foreground">用户设置</h2>
                                <button
                                    onClick={onClose}
                                    className="p-2 rounded-lg hover:bg-surface-container-high transition-colors"
                                >
                                    <X className="w-5 h-5 text-muted" />
                                </button>
                            </div>

                            <div className="flex min-h-[400px]">
                                {/* 左侧导航 */}
                                <div className="w-48 border-r border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 p-4">
                                    {tabs.map((tab) => (
                                        <button
                                            key={tab.id}
                                            onClick={() => {
                                                setActiveTab(tab.id);
                                                setError(null);
                                                setIsEditing(false);
                                                setIsChangingPassword(false);
                                            }}
                                            className={clsx(
                                                'w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all mb-2',
                                                activeTab === tab.id
                                                    ? 'bg-blue-100 dark:bg-blue-900/50 text-blue-600 dark:text-blue-400'
                                                    : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700'
                                            )}
                                        >
                                            <tab.icon className="w-4 h-4" />
                                            {tab.label}
                                        </button>
                                    ))}
                                </div>

                                {/* 右侧内容 */}
                                <div className="flex-1 p-6 bg-white dark:bg-gray-900">
                                    {/* 消息提示 */}
                                    {(error || success) && (
                                        <motion.div
                                            initial={{ opacity: 0, y: -10 }}
                                            animate={{ opacity: 1, y: 0 }}
                                            className={clsx(
                                                'mb-4 px-4 py-2 rounded-lg text-sm',
                                                error ? 'bg-red-500/10 text-red-500' : 'bg-green-500/10 text-green-500'
                                            )}
                                        >
                                            {error || success}
                                        </motion.div>
                                    )}

                                    {/* 基础信息面板 */}
                                    {activeTab === 'basic' && (
                                        <div className="space-y-6">
                                            {/* 头像 */}
                                            <div className="flex items-center gap-6">
                                                <div className="relative group">
                                                    <div
                                                        onClick={handleAvatarClick}
                                                        className="relative w-20 h-20 rounded-full bg-gradient-to-br from-indigo-500 via-purple-500 to-pink-500 flex items-center justify-center text-white text-2xl font-bold cursor-pointer overflow-hidden"
                                                    >
                                                        {user?.avatar ? (
                                                            <img src={user.avatar} alt="avatar" className="w-full h-full object-cover" />
                                                        ) : (
                                                            user?.userName?.charAt(0).toUpperCase() || 'U'
                                                        )}
                                                        <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity">
                                                            <Camera className="w-6 h-6 text-white" />
                                                        </div>
                                                    </div>
                                                    <input
                                                        ref={fileInputRef}
                                                        type="file"
                                                        accept="image/jpeg,image/png,image/gif,image/webp"
                                                        onChange={handleAvatarChange}
                                                        className="hidden"
                                                    />
                                                </div>
                                                <div>
                                                    <div className="text-sm text-muted">点击头像更换</div>
                                                    <div className="text-xs text-muted/60 mt-1">支持 JPG、PNG、GIF、WebP，最大 5MB</div>
                                                </div>
                                            </div>

                                            {/* 用户信息 */}
                                            {!isEditing ? (
                                                <div className="space-y-4">
                                                    <div className="flex justify-between items-center py-3 border-b border-gray-200 dark:border-gray-700">
                                                        <span className="text-gray-500 dark:text-gray-400">用户名</span>
                                                        <span className="text-gray-900 dark:text-gray-100">{user?.userName || '-'}</span>
                                                    </div>
                                                    <div className="flex justify-between items-center py-3 border-b border-gray-200 dark:border-gray-700">
                                                        <span className="text-gray-500 dark:text-gray-400">账号</span>
                                                        <span className="text-gray-900 dark:text-gray-100">{user?.userCode || '-'}</span>
                                                    </div>
                                                    <button
                                                        onClick={() => {
                                                            setIsEditing(true);
                                                            setEditForm({
                                                                userName: user?.userName || '',
                                                                userPhone: '',
                                                                address: '',
                                                            });
                                                        }}
                                                        className="mt-4 px-6 py-2.5 rounded-xl text-sm font-medium bg-blue-100 dark:bg-blue-900 text-blue-600 dark:text-blue-400 hover:bg-blue-200 dark:hover:bg-blue-800 transition-colors"
                                                    >
                                                        编辑资料
                                                    </button>
                                                </div>
                                            ) : (
                                                <div className="space-y-4">
                                                    <div>
                                                        <label className="block text-sm text-gray-500 dark:text-gray-400 mb-2">用户名</label>
                                                        <input
                                                            type="text"
                                                            value={editForm.userName}
                                                            onChange={(e) => setEditForm({ ...editForm, userName: e.target.value })}
                                                            className="w-full px-4 py-2.5 rounded-xl bg-gray-100 dark:bg-gray-800 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-gray-100 focus:outline-none focus:border-blue-500"
                                                        />
                                                    </div>
                                                    <div>
                                                        <label className="block text-sm text-gray-500 dark:text-gray-400 mb-2">手机号</label>
                                                        <input
                                                            type="text"
                                                            value={editForm.userPhone}
                                                            onChange={(e) => setEditForm({ ...editForm, userPhone: e.target.value })}
                                                            placeholder="选填"
                                                            className="w-full px-4 py-2.5 rounded-xl bg-gray-100 dark:bg-gray-800 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-gray-100 placeholder-gray-400 focus:outline-none focus:border-blue-500"
                                                        />
                                                    </div>
                                                    <div>
                                                        <label className="block text-sm text-gray-500 dark:text-gray-400 mb-2">地址</label>
                                                        <input
                                                            type="text"
                                                            value={editForm.address}
                                                            onChange={(e) => setEditForm({ ...editForm, address: e.target.value })}
                                                            placeholder="选填"
                                                            className="w-full px-4 py-2.5 rounded-xl bg-gray-100 dark:bg-gray-800 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-gray-100 placeholder-gray-400 focus:outline-none focus:border-blue-500"
                                                        />
                                                    </div>
                                                    <div className="flex gap-3 mt-6">
                                                        <button
                                                            onClick={() => setIsEditing(false)}
                                                            className="px-6 py-2.5 rounded-xl text-sm font-medium text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
                                                        >
                                                            取消
                                                        </button>
                                                        <button
                                                            onClick={handleSaveProfile}
                                                            disabled={isLoading}
                                                            className="px-6 py-2.5 rounded-xl text-sm font-medium bg-blue-600 hover:bg-blue-700 text-white transition-all disabled:opacity-50 flex items-center gap-2"
                                                        >
                                                            {isLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Check className="w-4 h-4" />}
                                                            保存
                                                        </button>
                                                    </div>
                                                </div>
                                            )}
                                        </div>
                                    )}

                                    {/* 隐私设置面板 */}
                                    {activeTab === 'privacy' && (
                                        <div className="space-y-6">
                                            <div className="flex justify-between items-center py-3 border-b border-gray-200 dark:border-gray-700">
                                                <div>
                                                    <div className="text-gray-900 dark:text-gray-100 font-medium">登录密码</div>
                                                    <div className="text-sm text-gray-500 dark:text-gray-400 mt-1">••••••••</div>
                                                </div>
                                                {!isChangingPassword && (
                                                    <button
                                                        onClick={() => setIsChangingPassword(true)}
                                                        className="px-4 py-2 rounded-xl text-sm font-medium bg-blue-100 dark:bg-blue-900 text-blue-600 dark:text-blue-400 hover:bg-blue-200 dark:hover:bg-blue-800 transition-colors"
                                                    >
                                                        修改密码
                                                    </button>
                                                )}
                                            </div>

                                            {isChangingPassword && (
                                                <motion.div
                                                    initial={{ opacity: 0, height: 0 }}
                                                    animate={{ opacity: 1, height: 'auto' }}
                                                    className="space-y-4 pt-4"
                                                >
                                                    <div>
                                                        <label className="block text-sm text-gray-500 dark:text-gray-400 mb-2">旧密码</label>
                                                        <input
                                                            type="password"
                                                            value={passwordForm.oldPassword}
                                                            onChange={(e) => setPasswordForm({ ...passwordForm, oldPassword: e.target.value })}
                                                            className="w-full px-4 py-2.5 rounded-xl bg-gray-100 dark:bg-gray-800 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-gray-100 focus:outline-none focus:border-blue-500"
                                                        />
                                                    </div>
                                                    <div>
                                                        <label className="block text-sm text-gray-500 dark:text-gray-400 mb-2">新密码</label>
                                                        <input
                                                            type="password"
                                                            value={passwordForm.newPassword}
                                                            onChange={(e) => setPasswordForm({ ...passwordForm, newPassword: e.target.value })}
                                                            placeholder="至少6位"
                                                            className="w-full px-4 py-2.5 rounded-xl bg-gray-100 dark:bg-gray-800 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-gray-100 placeholder-gray-400 focus:outline-none focus:border-blue-500"
                                                        />
                                                    </div>
                                                    <div>
                                                        <label className="block text-sm text-gray-500 dark:text-gray-400 mb-2">确认新密码</label>
                                                        <input
                                                            type="password"
                                                            value={passwordForm.confirmPassword}
                                                            onChange={(e) => setPasswordForm({ ...passwordForm, confirmPassword: e.target.value })}
                                                            className="w-full px-4 py-2.5 rounded-xl bg-gray-100 dark:bg-gray-800 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-gray-100 focus:outline-none focus:border-blue-500"
                                                        />
                                                    </div>
                                                    <div className="flex gap-3 mt-6">
                                                        <button
                                                            onClick={() => {
                                                                setIsChangingPassword(false);
                                                                setPasswordForm({ oldPassword: '', newPassword: '', confirmPassword: '' });
                                                                setError(null);
                                                            }}
                                                            className="px-6 py-2.5 rounded-xl text-sm font-medium text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
                                                        >
                                                            取消
                                                        </button>
                                                        <button
                                                            onClick={handleChangePassword}
                                                            disabled={isLoading}
                                                            className="px-6 py-2.5 rounded-xl text-sm font-medium bg-blue-600 hover:bg-blue-700 text-white transition-all disabled:opacity-50 flex items-center gap-2"
                                                        >
                                                            {isLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Check className="w-4 h-4" />}
                                                            确认修改
                                                        </button>
                                                    </div>
                                                </motion.div>
                                            )}
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>
                    </motion.div>
                </>
            )}
        </AnimatePresence>
    );
}
