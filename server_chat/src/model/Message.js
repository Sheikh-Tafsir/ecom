const { IS_REQUIRED} = require('../utils/Messages');
const { CONTENT_TYPE } = require('../utils/Enum');
const { generateUuidV7 } = require('../utils/UuidUtils');

module.exports = (sequelize, DataTypes) => {
    const Message = sequelize.define('Message',
        {
            id: {
                type: DataTypes.UUID,
                primaryKey: true,
                defaultValue: () => generateUuidV7(),
            },

            chatId: {
                type: DataTypes.UUID,
                allowNull: false,
                field: 'chat_id',
                validate: {
                    notNull: { msg: IS_REQUIRED },
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
                type: DataTypes.UUID,
                allowNull: false,
                field: 'sender_id',
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

            version: {
                type: DataTypes.INTEGER,
                allowNull: false,
                defaultValue: 0,
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
