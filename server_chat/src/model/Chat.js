const { IS_REQUIRED, lengthValidationMessage } = require('../utils/Messages');
const { CHAT_TYPE } = require('../utils/Enum');

module.exports = (sequelize, DataTypes) => {
    const Chat = sequelize.define('Chat',
        {
            type: {
                type: DataTypes.ENUM(...Object.values(CHAT_TYPE)),
                allowNull: false,
                validate: {
                    notNull: { msg: IS_REQUIRED },
                }
            },

            name: {
                type: DataTypes.STRING,
                allowNull: true,
                validate: {
                    len: {
                        args: [1, 63],
                        msg: lengthValidationMessage(1, 63)
                    },
                }
            },

            image: {
                type: DataTypes.STRING,
                allowNull: true,
                validate: {
                    len: {
                        args: [1, 511],
                        msg: lengthValidationMessage(1, 511)
                    },
                }
            },

            lastMessage: {
                type: DataTypes.TEXT,
                allowNull: true,
                field: 'last_message'
            },

            lastSent: {
                type: DataTypes.DATE,
                defaultValue: DataTypes.NOW,
                allowNull: true,
                field: 'last_sent'
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
            tableName: 'chats',
            timestamps: true,
            createdAt: 'created_at',
            updatedAt: 'updated_at',
        }
    );

    Chat.associate = models => {
        Chat.hasMany(models.ChatParticipant, { foreignKey: 'chatId', as: 'Participants' });
        Chat.hasMany(models.Message, { foreignKey: 'chatId', as: 'Messages' });
    };

    return Chat;
};
