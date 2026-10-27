const { IS_REQUIRED, ALREADY_EXISTS } = require('../utils/Messages');
const { CHAT_MEMBER_TYPE } = require('../utils/Enum');
const { baseFields, baseOptions } = require('./BaseModel');

module.exports = (sequelize, DataTypes) => {
    const ChatParticipant = sequelize.define('ChatParticipant',
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

            userId: {
                type: DataTypes.UUID,
                allowNull: false,
                field: 'user_id',
                validate: {
                    notNull: { msg: IS_REQUIRED },
                },
            },

            role: {
                type: DataTypes.STRING,
                allowNull: false,
                defaultValue: CHAT_MEMBER_TYPE.MEMBER,
                validate: {
                    notNull: { msg: IS_REQUIRED },
                }
            },

            unreadMessage: {
                type: DataTypes.INTEGER,
                allowNull: false,
                field: 'unread_message',
                defaultValue: 0,
                validate: {
                    notNull: { msg: IS_REQUIRED },
                    isInt: true,
                },
            },

            lastSeen: {
                type: DataTypes.DATE,
                defaultValue: DataTypes.NOW,
                allowNull: true,
                field: 'last_seen'
            },
        },
        baseOptions('chat_participants', {
            indexes: [
                {
                    unique: true,
                    fields: ['chat_id', 'user_id'],
                    msg: ALREADY_EXISTS
                }
            ]
        })
    );

    // Define associations
    ChatParticipant.associate = models => {
        ChatParticipant.belongsTo(models.Chat, { foreignKey: 'chatId', as: 'Chat' });
    };

    return ChatParticipant;
};
