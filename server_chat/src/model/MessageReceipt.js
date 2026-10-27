const { IS_REQUIRED, ALREADY_EXISTS } = require('../utils/Messages');
const { baseFields, baseOptions } = require('./BaseModel');

module.exports = (sequelize, DataTypes) => {
    const MessageReceipt = sequelize.define('MessageReceipt',
        {
            ...baseFields(DataTypes),

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
        },
        baseOptions('message_receipts', {
            indexes: [
                {
                    unique: true,
                    fields: ['message_id', 'user_id'],
                    msg: ALREADY_EXISTS
                }
            ]
        })
    );

    MessageReceipt.associate = models => {
        MessageReceipt.belongsTo(models.Message, { foreignKey: 'messageId', as: 'Message' });
    };

    return MessageReceipt;
};
