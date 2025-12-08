---
--- Created by 33435.
--- DateTime: 2025/10/29 11:56
--- 方案概要: 1.采用双结构存储用户登录信息，一个zset存储token ，然后根据token索引真实的session。
--- 2.zset以加入时间为score
-- 3.每次加入后，根据maxLoginNum，score删除zset对应元素与元素对应的session
-- KEYS[1] : 用户的indexKey，即userId为key，多个token的zset为值
-- KEYS[2] : 用户的sessionKey，即token为key，session为值
-- ARGV[1]: 用户当前所允许最大会话数
--  ARGV[2]: 当前会话的时间戳，作为排序分数
-- ARGV[3]:当前会话对象的json字符串
-- ARGV[4]: 过期时间戳
local indexKey       = KEYS[1];
local sessionKey     = KEYS[2];
local maxLoginNum    = tonumber(ARGV[1]);
local score          = tonumber(ARGV[2]);
local currentSession = ARGV[3];
local ttlSec         = tonumber(ARGV[4]);
-- 类似三元表达式
local delCmd         = ARGV[5] or "UNLINK"
-- 参数校验 lua中。 只有nil与false为false

if (not maxLoginNum or not score or not ttlSec) then
    return redis.error_reply("ARGV参数错误")
end

-- 获取全部数据 zrange 只返回元素值
local total = redis.call("ZRANGE", indexKey, "0", "-1")
local expiredMember = {}
local chunk = 1000
for i = 1, #total, 1 do
    local value = redis.call("GET", total[i]);
    -- 为空，加入过期集合中
    if (not value) then
        expiredMember[#expiredMember + 1] = total[i];
    end
end
-- 删除zset中过期的元素
for i = 1, #expiredMember, chunk do
    local last = math.min(#expiredMember, i + chunk - 1)
    redis.call("ZREM", indexKey, unpack(expiredMember, i, last))
end

-- 向有序集合indexKey中以score为得分存储sessionKey,
-- 应对“长期不登陆”用户：如果某个帐号很久都不再登录，脚本也就不会再触发。此时即使 ZSet 里已经被你清空，会话 key 也删掉了，indexKey 这个空键仍会一直存着。设置 EXPIRE 能保证一段时间后 Redis    自动清理，不留下空壳键。
-- 防御突发异常：比如 Lua 中途报错、Java 在脚本执行后抛出异常导致后续清理逻辑没跑完，索引就可能残留。TTL 相当于兜底，避免故障期间产生的垃圾永久滞留。
-- 便于统一回收策略：如果未来新增场景（例如被封禁用户、账号迁移），即便没走登录流程，也能靠时间回收这些键，Redis 内存不会越积越多。
redis.call("ZADD", indexKey, score, sessionKey);
redis.call("EXPIRE",indexKey,ttlSec);

-- 向redis中存sesionKey关联的会话对象
redis.call("SET", sessionKey, currentSession, "EX", ttlSec);
-- 获取当前zset集合中的token数
local currentLoginNum = redis.call("ZCARD", indexKey);
-- 如果大于，那么只保留分数最大的max个,弹出最小的c -m个
local member = {}
if currentLoginNum > maxLoginNum then
    local over = currentLoginNum - maxLoginNum;
    local poped = redis.call("ZPOPMIN", indexKey, over);
    if poped and #poped > 0 then
        for i = 1, #poped, 2 do
            member[#member + 1] = poped[i]
        end
        -- 分批释放参数并调用redis，裁剪上下界
        for i = 1, #member, chunk do
            local last = math.min(#member, i + chunk - 1)
            redis.call(delCmd, unpack(member, i, last))
        end
    end
end
return #member
