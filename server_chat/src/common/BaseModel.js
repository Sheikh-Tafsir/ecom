const { generateUuidV7 } = require('../utils/UuidUtils');

/**
 * Shared base fields for all Sequelize models — mirrors Spring Boot's BaseEntity.
 * Provides: id (UUID v7), createdAt, updatedAt, version (optimistic locking).
 */
const baseFields = (DataTypes) => ({
    id: {
        type: DataTypes.UUID,
        primaryKey: true,
        defaultValue: () => generateUuidV7(),
    },
    createdAt: {
        type: DataTypes.DATE,
        allowNull: false,
        defaultValue: DataTypes.NOW,
        field: 'created_at'
    },
    updatedAt: {
        type: DataTypes.DATE,
        allowNull: false,
        defaultValue: DataTypes.NOW,
        onUpdate: DataTypes.NOW,
        field: 'updated_at'
    },
    version: {
        type: DataTypes.INTEGER,
        allowNull: false,
        defaultValue: 0,
    }
});

/**
 * Shared model options — provides timestamp mapping and merges any extras (indexes, etc.).
 */
const baseOptions = (tableName, extra = {}) => ({
    tableName,
    timestamps: true,
    createdAt: 'created_at',
    updatedAt: 'updated_at',
    ...extra,
});

module.exports = { baseFields, baseOptions };
