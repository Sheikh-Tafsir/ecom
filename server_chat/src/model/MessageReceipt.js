const { IS_REQUIRED, ALREADY_EXISTS} = require('../utils/Messages');
const { generateUuidV7 } = require('../utils/UuidUtils');

module.exports = (sequelize, DataTypes) => {
    const MessageReceipt = sequelize.define('MessageReceipt',
        {
            id: {
                type: DataTypes.UUID,
                primaryKey: true,
                defaultValue: () => generateUuidV7(),
            },

            messageId: {
                type: DataTypes.UUID,
                allowNull: false,
                field: 'message_id',
                validate: {
                    notNull: { msg: IS_REQUIRED },
                },
            },

            userId: {
                type: DataTypes.UUID,
                allowNull: false,
                field: 'user_id',
                validate: {
                    notNull: { msg: IS_REQUIRED },
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
                defaultValue: 0,
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
