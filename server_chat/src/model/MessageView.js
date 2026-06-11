const { IS_REQUIRED } = require('../utils/Messages');

module.exports = (sequelize, DataTypes) => {
    const MessageView = sequelize.define('MessageView',
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

            viewerId: {
                type: DataTypes.INTEGER,
                allowNull: false,
                field: 'viewer_id',
                validate: {
                    notNull: { msg: IS_REQUIRED },
                    isInt: true,
                },
            },

            seenTime: {
                type: DataTypes.DATE,
                allowNull: false,
                field: 'seen_time'
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
            tableName: 'message_views',
            timestamps: true,
            createdAt: 'created_at',
            updatedAt: 'updated_at',
        }
    );

    MessageView.associate = models => {
        MessageView.belongsTo(models.Message, { foreignKey: 'messageId', as: 'Message' });
        MessageView.belongsTo(models.User, { foreignKey: 'viewerId', as: 'Viewer' });
    };

    return MessageView;
};
