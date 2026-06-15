const { IS_REQUIRED, ALREADY_EXISTS} = require('../utils/Messages');

module.exports = (sequelize, DataTypes) => {
    const MessageReceipt = sequelize.define('MessageReceipt',
        {
            messageId: {
                type: DataTypes.INTEGER,
                allowNull: false,
                field: 'message_id',
                validate: {
                    notNull: { msg: IS_REQUIRED },
                    isInt: true,
                },
            },

            userId: {
                type: DataTypes.INTEGER,
                allowNull: false,
                field: 'viewer_id',
                validate: {
                    notNull: { msg: IS_REQUIRED },
                    isInt: true,
                },
            },

            deliveredAt: {
                type: DataTypes.DATE,
                allowNull: true,
                field: 'delivered_at'
            },

            readAt: {
                type: DataTypes.DATE,
                allowNull: true,
                field: 'read_at'
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
                defaultValue: 1,
            }
        },
        {
            tableName: 'message_receipts',
            timestamps: true,
            createdAt: 'created_at',
            updatedAt: 'updated_at',
            indexes: [
                {
                    unique: true,
                    fields: ['message_id', 'user_id'],
                    msg: ALREADY_EXISTS
                }
            ]
        }
    );

    MessageReceipt.associate = models => {
        MessageReceipt.belongsTo(models.Message, { foreignKey: 'messageId', as: 'Message' });
        // MessageReceipt.belongsTo(models.User, { foreignKey: 'viewerId', as: 'Viewer' });
    };

    return MessageReceipt;
};
