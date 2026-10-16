const fs = require('fs');
const path = require('path');
const sequelize = require('./SequelizeConfig');
const { logger } = require('./logger');

const ADVISORY_LOCK_ID = 742918374;

const initDb = async () => {
    let lockAcquired = false;
    try {
        logger.info('Acquiring migration advisory lock...');
        await sequelize.query(`SELECT pg_advisory_lock(${ADVISORY_LOCK_ID});`);
        lockAcquired = true;
        logger.info('Migration advisory lock acquired.');

        // 1. Ensure SequelizeMeta table exists
        await sequelize.query(`
            CREATE TABLE IF NOT EXISTS "SequelizeMeta" (
                "name" VARCHAR(255) NOT NULL PRIMARY KEY
            );
        `);

        // 2. Fetch applied migrations
        const [appliedRows] = await sequelize.query('SELECT name FROM "SequelizeMeta";');
        const appliedSet = new Set(appliedRows.map((r) => r.name));

        // 3. Scan migrations directory
        const migrationsDir = path.join(__dirname, '../../migrations');
        if (!fs.existsSync(migrationsDir)) {
            logger.info('No migrations directory found. Skipping schema initialization.');
            return;
        }

        const files = fs
            .readdirSync(migrationsDir)
            .filter((f) => f.endsWith('.sql'))
            .sort();

        for (const file of files) {
            if (appliedSet.has(file)) {
                logger.debug(`Migration ${file} already applied. Skipping.`);
                continue;
            }

            logger.info(`Applying migration: ${file}...`);
            const filePath = path.join(migrationsDir, file);
            const sql = fs.readFileSync(filePath, 'utf8');

            await sequelize.transaction(async (t) => {
                await sequelize.query(sql, { transaction: t });
                await sequelize.query(
                    'INSERT INTO "SequelizeMeta" ("name") VALUES (:name);',
                    {
                        replacements: { name: file },
                        transaction: t
                    }
                );
            });

            logger.info(`Migration ${file} applied successfully.`);
        }

        logger.info('Database schema migration complete.');
    } catch (error) {
        logger.error('Database migration failed:', error.message);
        throw error;
    } finally {
        if (lockAcquired) {
            try {
                await sequelize.query(`SELECT pg_advisory_unlock(${ADVISORY_LOCK_ID});`);
                logger.info('Migration advisory lock released.');
            } catch (unlockErr) {
                logger.error('Failed to release migration advisory lock:', unlockErr.message);
            }
        }
    }
};

module.exports = initDb;
