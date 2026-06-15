const { IS_REQUIRED} = require('../utils/Messages');
const { CONTENT_TYPE } = require('../utils/Enum');

module.exports = (sequelize, DataTypes) => {
    const Message = sequelize.define('Message',
        {
            chatId: {
                type: DataTypes.INTEGER,
                allowNull: false,
                field: 'chat_id',
                validate: {
                    notNull: { msg: IS_REQUIRED },
                    isInt: true,
                },
            },

            content: {
                type: DataTypes.TEXT,
                allowNull: false,
                validate: {
                    notNull: { msg: IS_REQUIRED },
                }
            },

            contentType: {
                type: DataTypes.ENUM(...Object.values(CONTENT_TYPE)),
                allowNull: false,
                defaultValue: CONTENT_TYPE.TEXT,
                field: 'content_type',
                validate: {
                    notNull: { msg: IS_REQUIRED },
                }
            },

            senderId: {
                type: DataTypes.INTEGER,
                allowNull: false,
                field: 'sender_id',
                validate: {
                    notNull: { msg: IS_REQUIRED },
                    isInt: true,
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

            version: {
                type: DataTypes.INTEGER,
                allowNull: false,
                defaultValue: 1,
            }
        },
        {
            tableName: 'messages',
            timestamps: true,
            createdAt: 'created_at',
            updatedAt: 'updated_at',
        }
    );

    // Define associations
    Message.associate = models => {
        Message.belongsTo(models.Chat, { foreignKey: 'chatId', as: 'Chat' });
        Message.belongsTo(models.User, { foreignKey: 'senderId', as: 'Sender' });
        Message.hasMany(models.MessageReceipt, { foreignKey: 'messageId', as: 'MessageReceipts' });
    };

    return Message;
};
