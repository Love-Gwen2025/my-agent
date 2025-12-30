/**
 * 模型设置页面组件
 *
 * 独立页面，用于管理用户自定义 AI 模型配置
 */
import { useState, useEffect } from 'react';
import { ArrowLeft, Plus, Trash2, Star, StarOff, Edit2, Loader2, CheckCircle, XCircle, TestTube } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import clsx from 'clsx';
import { useAppStore } from '../../store';
import {
    getUserModels,
    createUserModel,
    updateUserModel,
    deleteUserModel,
    setDefaultUserModel,
    testUserModel,
} from '../../api';
import type { UserModel, UserModelPayload, UserModelTestPayload } from '../../types';

/** 提供商选项 */
const PROVIDERS = [
    { value: 'deepseek', label: 'DeepSeek', defaultBaseUrl: 'https://api.deepseek.com' },
    { value: 'openai', label: 'OpenAI', defaultBaseUrl: 'https://api.openai.com/v1' },
    { value: 'gemini', label: 'Google Gemini', defaultBaseUrl: '' },
    { value: 'custom', label: '自定义中转站', defaultBaseUrl: '' },
];

/** 常用模型预设 */
const MODEL_PRESETS: Record<string, string[]> = {
    deepseek: ['deepseek-chat', 'deepseek-reasoner'],
    openai: ['gpt-4', 'gpt-4-turbo', 'gpt-4o', 'gpt-3.5-turbo'],
    gemini: ['gemini-2.0-flash', 'gemini-1.5-pro', 'gemini-1.5-flash'],
    custom: [],
};

export function ModelSettingsPage() {
    const { setCurrentPage } = useAppStore();
    const [models, setModels] = useState<UserModel[]>([]);
    const [loading, setLoading] = useState(true);
    const [showAddModal, setShowAddModal] = useState(false);
    const [editingModel, setEditingModel] = useState<UserModel | null>(null);

    // 加载模型列表
    useEffect(() => {
        loadModels();
    }, []);

    async function loadModels() {
        setLoading(true);
        const data = await getUserModels();
        setModels(data);
        setLoading(false);
    }

    // 删除模型
    async function handleDelete(id: string) {
        if (!confirm('确定要删除此模型配置吗？')) return;
        try {
            await deleteUserModel(id);
            setModels(models.filter((m) => m.id !== id));
        } catch (e) {
            alert('删除失败: ' + (e as Error).message);
        }
    }

    // 设为默认
    async function handleSetDefault(id: string) {
        try {
            await setDefaultUserModel(id);
            setModels(models.map((m) => ({ ...m, isDefault: m.id === id })));
        } catch (e) {
            alert('设置失败: ' + (e as Error).message);
        }
    }

    return (
        <div className="h-screen flex flex-col bg-background text-foreground">
            {/* 顶部导航栏 */}
            <header className="flex items-center gap-4 px-6 py-4 border-b border-border/30 bg-surface-container">
                <button
                    onClick={() => setCurrentPage('chat')}
                    className="p-2 rounded-lg hover:bg-surface-container-high transition-colors"
                >
                    <ArrowLeft className="w-5 h-5" />
                </button>
                <h1 className="text-xl font-semibold">模型设置</h1>
                <div className="flex-1" />
                <button
                    onClick={() => setShowAddModal(true)}
                    className="flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:bg-primary/90 transition-colors"
                >
                    <Plus className="w-4 h-4" />
                    添加模型
                </button>
            </header>

            {/* 内容区域 */}
            <main className="flex-1 overflow-auto p-6">
                {/* 系统默认模型提示 */}
                <div className="mb-6 p-4 bg-surface-container rounded-xl border border-border/30">
                    <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center">
                            <Star className="w-5 h-5 text-primary" />
                        </div>
                        <div>
                            <h3 className="font-medium">DeepSeek R1 (系统默认)</h3>
                            <p className="text-sm text-muted">deepseek-reasoner · 系统提供的默认模型</p>
                        </div>
                    </div>
                </div>

                {/* 用户模型列表 */}
                {loading ? (
                    <div className="flex items-center justify-center py-20">
                        <Loader2 className="w-8 h-8 animate-spin text-primary" />
                    </div>
                ) : models.length === 0 ? (
                    <div className="text-center py-20 text-muted">
                        <p className="mb-4">还没有添加自定义模型</p>
                        <button
                            onClick={() => setShowAddModal(true)}
                            className="text-primary hover:underline"
                        >
                            添加第一个模型
                        </button>
                    </div>
                ) : (
                    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                        {models.map((model) => (
                            <ModelCard
                                key={model.id}
                                model={model}
                                onEdit={() => setEditingModel(model)}
                                onDelete={() => handleDelete(model.id)}
                                onSetDefault={() => handleSetDefault(model.id)}
                            />
                        ))}
                    </div>
                )}
            </main>

            {/* 添加/编辑模态框 */}
            <AnimatePresence>
                {(showAddModal || editingModel) && (
                    <ModelFormModal
                        model={editingModel}
                        onClose={() => {
                            setShowAddModal(false);
                            setEditingModel(null);
                        }}
                        onSave={async (data) => {
                            if (editingModel) {
                                await updateUserModel(editingModel.id, data);
                            } else {
                                await createUserModel(data as UserModelPayload);
                            }
                            loadModels();
                            setShowAddModal(false);
                            setEditingModel(null);
                        }}
                    />
                )}
            </AnimatePresence>
        </div>
    );
}

/** 模型卡片组件 */
function ModelCard({
    model,
    onEdit,
    onDelete,
    onSetDefault,
}: {
    model: UserModel;
    onEdit: () => void;
    onDelete: () => void;
    onSetDefault: () => void;
}) {
    const providerLabel = PROVIDERS.find((p) => p.value === model.provider)?.label || model.provider;

    return (
        <div className="p-4 bg-surface-container rounded-xl border border-border/30 hover:border-primary/30 transition-colors">
            <div className="flex items-start justify-between mb-3">
                <div>
                    <h3 className="font-medium">{model.modelName}</h3>
                    <p className="text-sm text-muted">{providerLabel}</p>
                </div>
                {model.isDefault && (
                    <span className="px-2 py-0.5 text-xs bg-primary/10 text-primary rounded-full">
                        默认
                    </span>
                )}
            </div>

            <div className="text-sm text-muted mb-4">
                <p>模型: {model.modelCode}</p>
                <p>温度: {model.temperature} · 超时: {model.timeout}s</p>
            </div>

            <div className="flex items-center gap-2">
                <button
                    onClick={onEdit}
                    className="p-2 rounded-lg hover:bg-surface-container-high transition-colors text-muted hover:text-foreground"
                    title="编辑"
                >
                    <Edit2 className="w-4 h-4" />
                </button>
                <button
                    onClick={onSetDefault}
                    disabled={model.isDefault}
                    className={clsx(
                        'p-2 rounded-lg transition-colors',
                        model.isDefault
                            ? 'text-primary cursor-not-allowed'
                            : 'hover:bg-surface-container-high text-muted hover:text-foreground'
                    )}
                    title={model.isDefault ? '当前默认' : '设为默认'}
                >
                    {model.isDefault ? <Star className="w-4 h-4 fill-current" /> : <StarOff className="w-4 h-4" />}
                </button>
                <button
                    onClick={onDelete}
                    className="p-2 rounded-lg hover:bg-red-500/10 text-muted hover:text-red-500 transition-colors"
                    title="删除"
                >
                    <Trash2 className="w-4 h-4" />
                </button>
            </div>
        </div>
    );
}

/** 模型表单模态框 */
function ModelFormModal({
    model,
    onClose,
    onSave,
}: {
    model: UserModel | null;
    onClose: () => void;
    onSave: (data: UserModelPayload) => Promise<void>;
}) {
    const isEdit = !!model;
    const [loading, setLoading] = useState(false);
    const [testing, setTesting] = useState(false);
    const [testResult, setTestResult] = useState<{ success: boolean; message: string } | null>(null);

    // 表单状态
    const [provider, setProvider] = useState(model?.provider || 'deepseek');
    const [modelName, setModelName] = useState(model?.modelName || '');
    const [modelCode, setModelCode] = useState(model?.modelCode || '');
    const [apiKey, setApiKey] = useState('');
    const [baseUrl, setBaseUrl] = useState(model?.baseUrl || '');
    const [temperature, setTemperature] = useState(model?.temperature || 0.7);
    const [timeout, setTimeout] = useState(model?.timeout || 30);

    // 提供商改变时更新默认 baseUrl
    useEffect(() => {
        if (!isEdit) {
            const providerInfo = PROVIDERS.find((p) => p.value === provider);
            if (providerInfo?.defaultBaseUrl) {
                setBaseUrl(providerInfo.defaultBaseUrl);
            }
        }
    }, [provider, isEdit]);

    // 测试连接
    async function handleTest() {
        if (!apiKey) {
            alert('请先填写 API Key');
            return;
        }
        setTesting(true);
        setTestResult(null);
        try {
            const payload: UserModelTestPayload = {
                provider,
                modelCode: modelCode || MODEL_PRESETS[provider]?.[0] || 'gpt-4',
                apiKey,
                baseUrl: baseUrl || undefined,
            };
            const result = await testUserModel(payload);
            setTestResult(result);
        } catch (e) {
            setTestResult({ success: false, message: (e as Error).message });
        } finally {
            setTesting(false);
        }
    }

    // 保存
    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        if (!modelName || !modelCode || (!isEdit && !apiKey)) {
            alert('请填写必填项');
            return;
        }
        if (provider === 'custom' && !baseUrl) {
            alert('自定义提供商必须填写 Base URL');
            return;
        }

        setLoading(true);
        try {
            await onSave({
                modelName,
                provider,
                modelCode,
                apiKey: apiKey || undefined as unknown as string,
                baseUrl: baseUrl || undefined,
                temperature,
                timeout,
            });
        } catch (e) {
            alert('保存失败: ' + (e as Error).message);
        } finally {
            setLoading(false);
        }
    }

    return (
        <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
            onClick={onClose}
        >
            <motion.div
                initial={{ scale: 0.95, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                exit={{ scale: 0.95, opacity: 0 }}
                className="w-full max-w-lg bg-surface-container rounded-2xl shadow-2xl overflow-hidden"
                onClick={(e) => e.stopPropagation()}
            >
                {/* 标题 */}
                <div className="px-6 py-4 border-b border-border/30">
                    <h2 className="text-lg font-semibold">{isEdit ? '编辑模型' : '添加模型'}</h2>
                </div>

                {/* 表单 */}
                <form onSubmit={handleSubmit} className="p-6 space-y-4 max-h-[70vh] overflow-auto">
                    {/* 提供商 */}
                    <div>
                        <label className="block text-sm font-medium mb-2">提供商</label>
                        <select
                            value={provider}
                            onChange={(e) => setProvider(e.target.value)}
                            className="w-full px-3 py-2 bg-surface-container-high rounded-lg border border-border/50 focus:border-primary focus:outline-none"
                        >
                            {PROVIDERS.map((p) => (
                                <option key={p.value} value={p.value}>
                                    {p.label}
                                </option>
                            ))}
                        </select>
                    </div>

                    {/* 模型名称 */}
                    <div>
                        <label className="block text-sm font-medium mb-2">显示名称 *</label>
                        <input
                            type="text"
                            value={modelName}
                            onChange={(e) => setModelName(e.target.value)}
                            placeholder="例如: 我的 GPT-4"
                            className="w-full px-3 py-2 bg-surface-container-high rounded-lg border border-border/50 focus:border-primary focus:outline-none"
                            required
                        />
                    </div>

                    {/* 模型编码 */}
                    <div>
                        <label className="block text-sm font-medium mb-2">模型编码 *</label>
                        <div className="flex gap-2">
                            <input
                                type="text"
                                value={modelCode}
                                onChange={(e) => setModelCode(e.target.value)}
                                placeholder="例如: gpt-4"
                                className="flex-1 px-3 py-2 bg-surface-container-high rounded-lg border border-border/50 focus:border-primary focus:outline-none"
                                required
                            />
                            {MODEL_PRESETS[provider]?.length > 0 && (
                                <select
                                    value=""
                                    onChange={(e) => e.target.value && setModelCode(e.target.value)}
                                    className="px-3 py-2 bg-surface-container-high rounded-lg border border-border/50"
                                >
                                    <option value="">快速选择</option>
                                    {MODEL_PRESETS[provider].map((m) => (
                                        <option key={m} value={m}>
                                            {m}
                                        </option>
                                    ))}
                                </select>
                            )}
                        </div>
                    </div>

                    {/* API Key */}
                    <div>
                        <label className="block text-sm font-medium mb-2">
                            API Key {isEdit ? '(留空保持不变)' : '*'}
                        </label>
                        <input
                            type="password"
                            value={apiKey}
                            onChange={(e) => setApiKey(e.target.value)}
                            placeholder={isEdit ? '••••••••' : '输入 API Key'}
                            className="w-full px-3 py-2 bg-surface-container-high rounded-lg border border-border/50 focus:border-primary focus:outline-none"
                            required={!isEdit}
                        />
                    </div>

                    {/* Base URL */}
                    <div>
                        <label className="block text-sm font-medium mb-2">
                            Base URL {provider === 'custom' && '*'}
                        </label>
                        <input
                            type="url"
                            value={baseUrl}
                            onChange={(e) => setBaseUrl(e.target.value)}
                            placeholder="https://api.example.com/v1"
                            className="w-full px-3 py-2 bg-surface-container-high rounded-lg border border-border/50 focus:border-primary focus:outline-none"
                            required={provider === 'custom'}
                        />
                    </div>

                    {/* 温度和超时 */}
                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium mb-2">温度 ({temperature})</label>
                            <input
                                type="range"
                                min="0"
                                max="2"
                                step="0.1"
                                value={temperature}
                                onChange={(e) => setTemperature(parseFloat(e.target.value))}
                                className="w-full"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium mb-2">超时 (秒)</label>
                            <input
                                type="number"
                                min="1"
                                max="300"
                                value={timeout}
                                onChange={(e) => setTimeout(parseInt(e.target.value) || 30)}
                                className="w-full px-3 py-2 bg-surface-container-high rounded-lg border border-border/50 focus:border-primary focus:outline-none"
                            />
                        </div>
                    </div>

                    {/* 测试连接 */}
                    <div className="pt-2">
                        <button
                            type="button"
                            onClick={handleTest}
                            disabled={testing || !apiKey}
                            className="flex items-center gap-2 px-4 py-2 border border-border/50 rounded-lg hover:bg-surface-container-high transition-colors disabled:opacity-50"
                        >
                            {testing ? (
                                <Loader2 className="w-4 h-4 animate-spin" />
                            ) : (
                                <TestTube className="w-4 h-4" />
                            )}
                            测试连接
                        </button>
                        {testResult && (
                            <div
                                className={clsx(
                                    'mt-2 p-3 rounded-lg text-sm flex items-start gap-2',
                                    testResult.success ? 'bg-green-500/10 text-green-600' : 'bg-red-500/10 text-red-500'
                                )}
                            >
                                {testResult.success ? (
                                    <CheckCircle className="w-4 h-4 shrink-0 mt-0.5" />
                                ) : (
                                    <XCircle className="w-4 h-4 shrink-0 mt-0.5" />
                                )}
                                <span>{testResult.message}</span>
                            </div>
                        )}
                    </div>
                </form>

                {/* 底部操作 */}
                <div className="px-6 py-4 border-t border-border/30 flex justify-end gap-3">
                    <button
                        type="button"
                        onClick={onClose}
                        className="px-4 py-2 rounded-lg hover:bg-surface-container-high transition-colors"
                    >
                        取消
                    </button>
                    <button
                        type="submit"
                        onClick={handleSubmit}
                        disabled={loading}
                        className="flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:bg-primary/90 transition-colors disabled:opacity-50"
                    >
                        {loading && <Loader2 className="w-4 h-4 animate-spin" />}
                        {isEdit ? '保存' : '添加'}
                    </button>
                </div>
            </motion.div>
        </motion.div>
    );
}
