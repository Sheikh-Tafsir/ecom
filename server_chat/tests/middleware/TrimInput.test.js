const { describe, it } = require('node:test');
const assert = require('node:assert');
const TrimInput = require('../../src/middleware/TrimInput');

describe('TrimInput Middleware', () => {
    it('trims leading/trailing whitespace and converts empty/whitespace-only strings to null', () => {
        const req = {
            body: {
                message: '   Hello world!   ',
                empty: '',
                spacesOnly: '     ',
                number: 123,
                boolean: true,
                nullVal: null
            }
        };

        TrimInput(req, {}, () => {});

        assert.strictEqual(req.body.message, 'Hello world!');
        assert.strictEqual(req.body.empty, null);
        assert.strictEqual(req.body.spacesOnly, null);
        assert.strictEqual(req.body.number, 123);
        assert.strictEqual(req.body.boolean, true);
        assert.strictEqual(req.body.nullVal, null);
    });

    it('sanitizes nested objects and arrays recursively', () => {
        const req = {
            body: {
                user: {
                    name: '  Alice  ',
                    bio: '   '
                },
                tags: ['  tag1  ', '   ', 'tag2', ['  nested  ', '']]
            }
        };

        TrimInput(req, {}, () => {});

        assert.strictEqual(req.body.user.name, 'Alice');
        assert.strictEqual(req.body.user.bio, null);
        assert.strictEqual(req.body.tags[0], 'tag1');
        assert.strictEqual(req.body.tags[1], null);
        assert.strictEqual(req.body.tags[2], 'tag2');
        assert.strictEqual(req.body.tags[3][0], 'nested');
        assert.strictEqual(req.body.tags[3][1], null);
    });

    it('sanitizes query parameters and params if present', () => {
        const req = {
            query: {
                search: '  electronics  ',
                filter: '   '
            },
            params: {
                id: '   123   '
            }
        };

        TrimInput(req, {}, () => {});

        assert.strictEqual(req.query.search, 'electronics');
        assert.strictEqual(req.query.filter, null);
        assert.strictEqual(req.params.id, '123');
    });

    it('exposes sanitize function directly for non-HTTP payloads', () => {
        const socketPayload = {
            content: '   hello   ',
            emptyContent: '   '
        };

        TrimInput.sanitize(socketPayload);

        assert.strictEqual(socketPayload.content, 'hello');
        assert.strictEqual(socketPayload.emptyContent, null);
    });
});
