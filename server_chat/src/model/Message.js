const { IS_REQUIRED } = require('../utils/Messages');
const { CONTENT_TYPE } = require('../utils/Enum');
const { baseFields, baseOptions } = require('./BaseModel');

module.exports = (sequelize, DataTypes) => {
    const Message = sequelize.define('Message',
        {
            ...baseFields(DataTypes),

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
        },
        baseOptions('messages')
    );

    // Define associations
    Message.associate = models => {
        Message.belongsTo(models.Chat, { foreignKey: 'chatId', as: 'Chat' });
        Message.hasMany(models.MessageReceipt, { foreignKey: 'messageId', as: 'MessageReceipts' });
    };

    return Message;
};
