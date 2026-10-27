const { describe, it, before, after } = require('node:test');
const assert = require('node:assert');
const axios = require('axios');
const RedisConfig = require('../../src/config/RedisConfig');
const UserService = require('../../src/service/UserService');

describe('UserService Unit Tests', () => {
    let originalRedisGet;
    let originalRedisSet;
    let originalAxiosPost;
    let originalInternalKey;

    before(() => {
        originalRedisGet = RedisConfig.get;
        originalRedisSet = RedisConfig.set;
        originalAxiosPost = axios.post;
        originalInternalKey = process.env.INTERNAL_SERVICE_KEY;
        process.env.INTERNAL_SERVICE_KEY = 'test-internal-service-key-123';
    });

    after(() => {
        RedisConfig.get = originalRedisGet;
        RedisConfig.set = originalRedisSet;
        axios.post = originalAxiosPost;
        process.env.INTERNAL_SERVICE_KEY = originalInternalKey;
        try {
            RedisConfig.disconnect();
        } catch (e) {}
    });

    it('should return empty object when userIds is empty or invalid', async () => {
        const result1 = await UserService.getUsersBatch([]);
        const result2 = await UserService.getUsersBatch(null);
        assert.deepStrictEqual(result1, {});
        assert.deepStrictEqual(result2, {});
    });

    it('should return cached users from Redis without calling HTTP', async () => {
        const cachedUser = {
            id: 'user-1',
            name: 'Alice',
            image: 'https://example.com/alice.jpg',
            email: 'alice@example.com'
        };

        RedisConfig.get = async (key) => {
            if (key === 'cache:user:user-1') {
                return JSON.stringify(cachedUser);
            }
            return null;
        };

        let httpCalled = false;
        axios.post = async () => {
            httpCalled = true;
            return { data: { data: [] } };
        };

        const result = await UserService.getUsersBatch(['user-1']);
        assert.strictEqual(httpCalled, false);
        assert.deepStrictEqual(result['user-1'], cachedUser);
    });

    it('should fetch missing users via HTTP POST and cache them in Redis', async () => {
        const cachedUser = { id: 'user-1', name: 'Alice', image: null, email: null };
        const fetchedUser = { id: 'user-2', name: 'Bob', image: 'https://example.com/bob.jpg', email: 'bob@example.com' };

        RedisConfig.get = async (key) => {
            if (key === 'cache:user:user-1') return JSON.stringify(cachedUser);
            return null;
        };

        let setCalledWith = null;
        RedisConfig.set = async (key, val, mode, ttl) => {
            setCalledWith = { key, val: JSON.parse(val), mode, ttl };
        };

        let requestUrl = null;
        let requestPayload = null;
        let requestHeaders = null;
        axios.post = async (url, data, config) => {
            requestUrl = url;
            requestPayload = data;
            requestHeaders = config.headers;
            return {
                data: {
                    statusCode: 200,
                    data: [fetchedUser]
                }
            };
        };

        const result = await UserService.getUsersBatch(['user-1', 'user-2'], 'Bearer test-token-123');

        assert.ok(requestUrl.endsWith('/internal/users/batch'));
        assert.deepStrictEqual(requestPayload, ['user-2']);
        assert.strictEqual(requestHeaders.Authorization, 'Bearer test-token-123');
        assert.strictEqual(requestHeaders['X-Internal-Service-Key'], 'test-internal-service-key-123');
        assert.strictEqual(setCalledWith.key, 'cache:user:user-2');
        assert.strictEqual(setCalledWith.ttl, 600);
        assert.deepStrictEqual(result['user-1'], cachedUser);
        assert.deepStrictEqual(result['user-2'], fetchedUser);
    });

    it('should send X-Internal-Service-Key header for machine-to-machine requests', async () => {
        RedisConfig.get = async () => null;
        RedisConfig.set = async () => {};

        let usedHeaders = null;
        axios.post = async (url, data, config) => {
            usedHeaders = config.headers;
            return { data: { data: [{ id: 'user-3', name: 'Charlie' }] } };
        };

        await UserService.getUsersBatch(['user-3']);
        assert.ok(usedHeaders);
        assert.strictEqual(usedHeaders['X-Internal-Service-Key'], 'test-internal-service-key-123');
    });

    it('should fallback to safe default when HTTP request fails', async () => {
        RedisConfig.get = async () => null;
        RedisConfig.set = async () => {};
        axios.post = async () => {
            throw new Error('Network error');
        };

        const result = await UserService.getUsersBatch(['user-4']);
        assert.deepStrictEqual(result['user-4'], {
            id: 'user-4',
            name: 'User',
            image: null,
            email: null
        });
    });

    it('should fetch single user via getUserById', async () => {
        RedisConfig.get = async (key) => {
            if (key === 'cache:user:user-5') {
                return JSON.stringify({ id: 'user-5', name: 'Dave', image: null, email: null });
            }
            return null;
        };

        const user = await UserService.getUserById('user-5');
        assert.strictEqual(user.id, 'user-5');
        assert.strictEqual(user.name, 'Dave');
    });
});
