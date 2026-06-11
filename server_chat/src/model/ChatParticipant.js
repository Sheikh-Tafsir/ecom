const { IS_REQUIRED, ALREADY_EXISTS } = require('../utils/Messages');
const { CHAT_MEMBER_TYPE } = require('../utils/Enum');

module.exports = (sequelize, DataTypes) => {
    const ChatParticipant = sequelize.define('ChatParticipant',
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

            userId: {
                type: DataTypes.INTEGER,
                allowNull: false,
                field: 'user_id',
                validate: {
                    notNull: { msg: IS_REQUIRED },
                    isInt: true,
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
            tableName: 'chat_participants',
            timestamps: true,
            createdAt: 'created_at',
            updatedAt: 'updated_at',
            indexes: [
                {
                    unique: true,
                    fields: ['chat_id', 'user_id'],
                    msg: ALREADY_EXISTS
                }
            ]
        }
    );

    // Define associations
    ChatParticipant.associate = models => {
        // ChatParticipant.belongsTo(models.Chat, { foreignKey: 'chatId', as: 'Chat' });
    };

    return ChatParticipant;
};
