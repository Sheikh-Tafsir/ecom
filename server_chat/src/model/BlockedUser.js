const { IS_REQUIRED, ALREADY_EXISTS } = require('../utils/Messages');
const { baseFields, baseOptions } = require('../common/BaseModel');

module.exports = (sequelize, DataTypes) => {
    const BlockedUser = sequelize.define('BlockedUser',
        {
            ...baseFields(DataTypes),

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
        },
        baseOptions('blocked_users', {
            indexes: [
                {
                    unique: true,
                    fields: ['user_id', 'blocked_user_id'],
                    msg: ALREADY_EXISTS
                }
            ]
        })
    );

    return BlockedUser;
};
