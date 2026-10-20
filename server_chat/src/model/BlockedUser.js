const { IS_REQUIRED, ALREADY_EXISTS } = require('../utils/Messages');
const { generateUuidV7 } = require('../utils/UuidUtils');

module.exports = (sequelize, DataTypes) => {
    const BlockedUser = sequelize.define('BlockedUser',
        {
            id: {
                type: DataTypes.UUID,
                primaryKey: true,
                defaultValue: () => generateUuidV7(),
            },
            userId: {
                type: DataTypes.UUID,
                allowNull: false,
                field: 'user_id',
                validate: {
                    notNull: { msg: IS_REQUIRED },
                },
            },
            blockedUserId: {
                type: DataTypes.UUID,
                allowNull: false,
                field: 'blocked_user_id',
                validate: {
                    notNull: { msg: IS_REQUIRED },
                },
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
        },
        {
            tableName: 'blocked_users',
            timestamps: true,
            createdAt: 'created_at',
            updatedAt: 'updated_at',
            indexes: [
                {
                    unique: true,
                    fields: ['user_id', 'blocked_user_id'],
                    msg: ALREADY_EXISTS
                }
            ]
        }
    );

    return BlockedUser;
};
